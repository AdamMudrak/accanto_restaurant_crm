package com.example.restaurant.service.payment;

import com.example.restaurant.dto.request.payment.PaymentRequest;
import com.example.restaurant.dto.response.list.payment.PaymentListResponse;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.notfound.GuestNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderItemNotFoundException;
import com.example.restaurant.exception.types.notfound.OrderNotFoundException;
import com.example.restaurant.exception.types.notfound.RestaurantNotFoundException;
import com.example.restaurant.mapper.payment.PaymentMapper;
import com.example.restaurant.mapper.payment.PaymentToMonobankMapper;
import com.example.restaurant.mapper.payment.PaymentToStripeMapper;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.order.GuestRepository;
import com.example.restaurant.repository.order.OrderItemRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.repository.payment.PaymentRepository;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankInvoiceRequest;
import com.example.restaurant.service.payment.monobank.dto.request.MonobankStatusRequest;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankInvoiceResponse;
import com.example.restaurant.service.payment.monobank.dto.response.MonobankStatusResponse;
import com.example.restaurant.service.payment.stripe.dto.request.StripeCheckoutSessionRequest;
import com.example.restaurant.service.payment.stripe.dto.request.StripeStatusRequest;
import com.example.restaurant.service.payment.stripe.dto.response.StripeCheckoutSessionResponse;
import com.example.restaurant.service.utils.CookieExtractor;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {
    private static final int MAX_SYNC_ERROR_LENGTH = 1024;
    private final OrderRepository orderRepository;
    private final GuestRepository guestRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;
    private final PaymentToMonobankMapper paymentToMonobankMapper;
    private final PaymentToStripeMapper paymentToStripeMapper;
    private final PaymentProviderStrategy paymentProviderStrategy;
    private final RestaurantRepository restaurantRepository;
    private final PaymentSettlementService settlementService;
    private final TransactionTemplate transactionTemplate;

    public PaymentResponse syncPayment(Long id) {
        Payment payment = settlementService.lockById(id);
        if (payment.getMethod() != Payment.PaymentMethod.ONLINE
                || payment.getProviderPaymentId() == null) {
            throw new ItemIllegalStateException("Provider invoice is unavailable.");
        }
        syncPendingPayment(payment);
        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentListResponse getPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);

        return toPaymentListResponse(payments, pageable);
    }

    @Transactional(readOnly = true)
    public PaymentListResponse getPaymentsNeedingReconciliation(Pageable pageable) {
        return toPaymentListResponse(
                paymentRepository.findNeedingReconciliation(pageable), pageable);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment with id " + paymentId + " not found"));

        return paymentMapper.toPaymentResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentListResponse getPaymentsByOrderId(Long orderId, Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAllByOrderId(orderId, pageable);

        return toPaymentListResponse(payments, pageable);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse pay(PaymentRequest paymentRequest, HttpServletRequest request) {
        PreparedPayment prepared = transactionTemplate.execute(ignored ->
                preparePayment(paymentRequest, request));
        if (prepared.existing() != null) {
            return prepared.existing();
        }
        try {
            return initializeProviderPayment(prepared);
        } catch (RuntimeException exception) {
            transactionTemplate.executeWithoutResult(ignored -> {
                Payment payment = settlementService.lockById(prepared.id());
                if (payment.getProviderPaymentId() == null) {
                    payment.setReconciliationIssue("Provider creation result is uncertain; "
                            + "verify provider reference " + payment.getId() + " before retrying.");
                }
            });
            throw exception;
        }
    }

    private PreparedPayment preparePayment(PaymentRequest paymentRequest,
                                            HttpServletRequest request) {
        Order order = findOrderByUuid(request);
        Guest guest = findGuest(request);
        if (!guest.getOrder().getId().equals(order.getId())) {
            throw new ItemIllegalStateException("Guest does not belong to this order.");
        }
        List<OrderItem> orderItems = findOrderItemsForPayment(
                paymentRequest.orderItemsIds(), order.getId());
        Optional<Payment> existingPayment = findExistingPayment(orderItems, paymentRequest);

        validateOrderItemsArePayable(orderItems);

        if (existingPayment.isPresent()) {
            return new PreparedPayment(existingPayment.get().getId(),
                    paymentMapper.toPaymentResponse(existingPayment.get()), null, null);
        }

        Payment payment = new Payment();
        BigDecimal amount = getTotalAmountForItems(orderItems);
        BigDecimal tipAmount = paymentRequest.tipAmount() != null
                ? paymentRequest.tipAmount()
                : BigDecimal.ZERO;
        validateTotalAmount(amount, tipAmount);
        Restaurant restaurant = findRestaurant();
        if (order.getCurrency() == null) {
            throw new ItemIllegalStateException(
                    "Order currency requires historical reconciliation.");
        }
        PaymentProviderType paymentProviderType = restaurant.getPaymentProvider();
        payment = paymentRepository.save(
                paymentMapper.toPayment(
                        payment, order, guest, amount, tipAmount, paymentProviderType));
        prepareOrderItemsForPayment(orderItems, payment);
        resetProviderState(payment);
        paymentRepository.flush();

        return new PreparedPayment(payment.getId(), null,
                paymentProviderType == PaymentProviderType.MONOBANK
                        ? paymentToMonobankMapper.toMonobankInvoiceRequest(payment, order) : null,
                paymentProviderType == PaymentProviderType.STRIPE
                        ? paymentToStripeMapper.toStripeCheckoutSessionRequest(
                                payment, order) : null);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncPendingPayments() {
        paymentRepository
                .findAllByStatusAndMethodAndProviderPaymentIdIsNotNull(
                        Payment.PaymentStatus.PENDING, Payment.PaymentMethod.ONLINE)
                .stream().map(Payment::getId).toList()
                .forEach(id -> transactionTemplate.executeWithoutResult(ignored ->
                        syncPendingPayment(settlementService.lockById(id))));
    }

    private Restaurant findRestaurant() {
        return restaurantRepository.findSingleton()
                .orElseThrow(() -> new RestaurantNotFoundException("Couldn't obtain restaurant"));
    }

    private PaymentListResponse toPaymentListResponse(Page<Payment> payments, Pageable pageable) {
        List<PaymentResponse> paymentResponses = payments.getContent().stream()
                .map(paymentMapper::toPaymentResponse)
                .toList();

        return new PaymentListResponse(
                paymentResponses,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                payments.getTotalElements());
    }

    private Order findOrderByUuid(HttpServletRequest request) {
        String orderUuid = CookieExtractor.extractCookieValueByName(request, "orderUuid");
        return orderRepository
                .lockByUuidAndStatus(
                        orderUuid, Order.OrderStatus.OPENED)
                .orElseThrow(() -> new OrderNotFoundException(
                        "No opened with uuid: " + orderUuid + " found."));
    }

    private Guest findGuest(HttpServletRequest request) {
        String guestUuid = CookieExtractor.extractCookieValueByName(request, "guestUuid");
        return guestRepository.findByUuid(guestUuid).orElseThrow(
                () -> new GuestNotFoundException("Guest with uuid: " + guestUuid + " not found."));
    }

    private List<OrderItem> findOrderItemsForPayment(Set<Long> itemsIds, Long orderId) {
        return itemsIds.stream().sorted()
                .map(itemId -> orderItemRepository.findByIdAndOrderId(itemId, orderId)
                        .orElseThrow(() -> new OrderItemNotFoundException(
                                "Can't find order item with id: " + itemId
                                        + " in order with id: " + orderId)))
                .toList();
    }

    private Optional<Payment> findExistingPayment(
            List<OrderItem> orderItems,
            PaymentRequest paymentRequest) {
        List<Payment> existingPayments = orderItems.stream()
                .map(OrderItem::getPayment)
                .filter(Objects::nonNull)
                .filter(payment -> payment.getStatus() == Payment.PaymentStatus.PENDING)
                .distinct()
                .toList();

        if (existingPayments.isEmpty()) {
            return Optional.empty();
        }

        if (existingPayments.size() > 1) {
            throw new ItemIllegalStateException(
                    "Selected order items belong to different payments.");
        }

        Payment payment = existingPayments.get(0);
        validateExistingPaymentMatchesRequest(payment, paymentRequest.orderItemsIds());

        return Optional.of(payment);
    }

    private void validateExistingPaymentMatchesRequest(
            Payment payment,
            Set<Long> requestedItemIds) {
        Set<Long> existingItemIds = payment.getOrderItems().stream()
                .map(OrderItem::getId)
                .collect(Collectors.toSet());

        if (!existingItemIds.equals(requestedItemIds)) {
            throw new ItemIllegalStateException(
                    "Selected order items do not match existing payment items.");
        }
    }

    private BigDecimal getTotalAmountForItems(List<OrderItem> orderItems) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem orderItem : orderItems) {
            validateOrderItemIsPayable(orderItem);
            totalAmount = totalAmount.add(orderItem.getItemPriceSnapshot()
                    .multiply(BigDecimal.valueOf(orderItem.getQuantity())));
        }

        return totalAmount;
    }

    private void validateOrderItemsArePayable(List<OrderItem> orderItems) {
        orderItems.forEach(this::validateOrderItemIsPayable);
    }

    private void validateOrderItemIsPayable(OrderItem orderItem) {
        if (orderItem.getStatus() != OrderItem.OrderItemStatus.CONFIRMED
                && orderItem.getStatus() != OrderItem.OrderItemStatus.SERVED) {
            throw new ItemIllegalStateException(
                    "Order item with id " + orderItem.getId()
                            + " must be CONFIRMED or SERVED before payment.");
        }
    }

    private void prepareOrderItemsForPayment(List<OrderItem> orderItems, Payment payment) {
        payment.getOrderItems().clear();
        for (OrderItem orderItem : orderItems) {
            payment.getOrderItems().add(orderItem);
            orderItem.setPayment(payment);
        }
    }

    private void resetProviderState(Payment payment) {
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setProviderPaymentId(null);
        payment.setPaymentPageUrl(null);
        payment.setCompletedAt(null);
        payment.setProviderModifiedAt(null);
        payment.setFailureReason(null);
        payment.setProviderErrorCode(null);
        payment.setProviderErrorText(null);
        payment.setSyncAttempts(0);
        payment.setLastSyncAttemptAt(null);
        payment.setLastSyncError(null);
    }

    private void validateTotalAmount(BigDecimal amount, BigDecimal tipAmount) {
        if (amount.add(tipAmount).compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemIllegalStateException("Payment amount must be greater than zero.");
        }
    }

    private PaymentResponse initializeProviderPayment(PreparedPayment prepared) {
        String providerId;
        String pageUrl;
        if (prepared.mono() != null) {
            PaymentProvider<MonobankInvoiceRequest, MonobankInvoiceResponse,
                    MonobankStatusRequest, MonobankStatusResponse> provider =
                    paymentProviderStrategy.getPaymentProvider(PaymentProviderType.MONOBANK);
            MonobankInvoiceResponse response = provider.pay(prepared.mono());
            providerId = response.invoiceId();
            pageUrl = response.pageUrl();
        } else {
            PaymentProvider<StripeCheckoutSessionRequest, StripeCheckoutSessionResponse,
                    StripeStatusRequest, StripeCheckoutSessionResponse> provider =
                    paymentProviderStrategy.getPaymentProvider(PaymentProviderType.STRIPE);
            StripeCheckoutSessionResponse response = provider.pay(prepared.stripe());
            providerId = response.id();
            pageUrl = response.url();
        }
        return transactionTemplate.execute(ignored -> {
            Payment payment = settlementService.lockById(prepared.id());
            payment.setProviderPaymentId(providerId);
            payment.setPaymentPageUrl(pageUrl);
            return paymentMapper.toPaymentResponse(payment);
        });
    }

    private record PreparedPayment(Long id, PaymentResponse existing,
                                   MonobankInvoiceRequest mono,
                                   StripeCheckoutSessionRequest stripe) {}

    private void refreshPaymentStatus(Payment payment) {
        switch (payment.getProviderType()) {
            case MONOBANK -> refreshMonobankStatus(payment);
            case STRIPE -> refreshStripeStatus(payment);
            default -> throw new IllegalStateException(
                    "Unknown payment provider: " + payment.getProviderType());
        }
    }

    private void syncPendingPayment(Payment payment) {
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            return;
        }

        try {
            payment.setLastSyncAttemptAt(LocalDateTime.now());
            refreshPaymentStatus(payment);
            payment.setSyncAttempts(0);
            payment.setLastSyncError(null);
        } catch (RuntimeException exception) {
            payment.setSyncAttempts(payment.getSyncAttempts() + 1);
            payment.setLastSyncError(toSyncError(exception));
        }
    }

    private String toSyncError(RuntimeException exception) {
        String error = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        if (error.length() > MAX_SYNC_ERROR_LENGTH) {
            return error.substring(0, MAX_SYNC_ERROR_LENGTH);
        }

        return error;
    }

    private void refreshMonobankStatus(Payment payment) {
        PaymentProvider<MonobankInvoiceRequest, MonobankInvoiceResponse,
                MonobankStatusRequest, MonobankStatusResponse> paymentProvider =
                paymentProviderStrategy.getPaymentProvider(PaymentProviderType.MONOBANK);
        MonobankStatusResponse response = paymentProvider.getStatus(
                new MonobankStatusRequest(payment.getProviderPaymentId()));

        settlementService.applyStatus(payment, toPaymentStatus(response.status()),
                response.modifiedDate());
        payment.setFailureReason(response.failureReason());
        payment.setProviderErrorCode(response.errCode());
        payment.setProviderErrorText(response.errText());

    }

    private void refreshStripeStatus(Payment payment) {
        PaymentProvider<StripeCheckoutSessionRequest, StripeCheckoutSessionResponse,
                StripeStatusRequest, StripeCheckoutSessionResponse> paymentProvider =
                paymentProviderStrategy.getPaymentProvider(PaymentProviderType.STRIPE);
        StripeCheckoutSessionResponse response =
                paymentProvider.getStatus(new StripeStatusRequest(payment.getProviderPaymentId()));

        settlementService.applyStatus(payment, toPaymentStatus(response), null);
    }

    private Payment.PaymentStatus toPaymentStatus(MonobankStatusResponse.Status status) {
        return switch (status) {
            case created, processing, hold -> Payment.PaymentStatus.PENDING;
            case success -> Payment.PaymentStatus.SUCCEEDED;
            case failure, reversed -> Payment.PaymentStatus.FAILED;
            case expired -> Payment.PaymentStatus.EXPIRED;
        };
    }

    private Payment.PaymentStatus toPaymentStatus(StripeCheckoutSessionResponse response) {
        if ("paid".equals(response.paymentStatus())) {
            return Payment.PaymentStatus.SUCCEEDED;
        }

        if ("expired".equals(response.status())) {
            return Payment.PaymentStatus.EXPIRED;
        }

        return Payment.PaymentStatus.PENDING;
    }
}
