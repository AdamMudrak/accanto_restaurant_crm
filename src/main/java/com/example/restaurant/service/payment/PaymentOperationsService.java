package com.example.restaurant.service.payment;

import com.example.restaurant.dto.request.payment.LinkInvoiceRequest;
import com.example.restaurant.dto.request.payment.RefundRequest;
import com.example.restaurant.dto.request.payment.ResolvePaymentRequest;
import com.example.restaurant.dto.response.payment.PaymentResponse;
import com.example.restaurant.dto.response.payment.RefundResponse;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.mapper.payment.PaymentMapper;
import com.example.restaurant.repository.payment.PaymentRefundRepository;
import com.example.restaurant.repository.payment.PaymentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PaymentOperationsService {
    private final PaymentSettlementService settlement;
    private final PaymentRepository payments;
    private final PaymentRefundRepository refunds;
    private final PaymentProviderStrategy providers;
    private final PaymentMapper mapper;
    private final TransactionTemplate transactions;

    @Transactional
    public PaymentResponse link(Long id, LinkInvoiceRequest request, Employee actor) {
        Payment payment = settlement.lockById(id);
        require(payment.getMethod() == Payment.PaymentMethod.ONLINE, "Online payment required.");
        require(payment.getProviderPaymentId() == null
                || payment.getProviderPaymentId().equals(request.invoiceId()),
                "Cannot replace a linked provider invoice.");
        ProviderInvoice invoice = providers.getPaymentProvider(payment.getProviderType())
                .inspect(request.invoiceId());
        require(invoice != null && request.invoiceId().equals(invoice.id())
                && String.valueOf(payment.getId()).equals(invoice.reference())
                && invoice.amount() == cents(payment)
                && payment.getCurrency() != null
                && payment.getCurrency().name().equalsIgnoreCase(invoice.currency())
                && invoice.status() != null, "Provider invoice does not match this payment.");
        require(payments.findByProviderPaymentId(invoice.id())
                .map(existing -> existing.getId().equals(id)).orElse(true),
                "Provider invoice is already linked to another payment.");
        payment.setProviderPaymentId(invoice.id());
        payment.setReconciliationIssue(null);
        settlement.applyStatus(payment, invoice.status(), invoice.modifiedAt());
        payment.setResolutionNote("Verified provider invoice " + invoice.id());
        payment.setResolvedAt(now());
        payment.setResolvedBy(actor);
        return mapper.toPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse resolve(Long id, ResolvePaymentRequest request, Employee actor) {
        Payment payment = settlement.lockById(id);
        require(payment.getReconciliationIssue() != null, "Payment has no unresolved issue.");
        if (request.action() == ResolvePaymentRequest.Action.CONFIRM_NOT_CHARGED) {
            require(payment.getStatus() == Payment.PaymentStatus.PENDING
                    && payment.getProviderPaymentId() == null,
                    "Only an unknown invoice may be resolved using provider dashboard evidence.");
            payment.setStatus(Payment.PaymentStatus.FAILED);
        } else {
            require(payment.getStatus() != Payment.PaymentStatus.PENDING,
                    "Confirm the final provider status first.");
            require(payment.getRefund() != null
                    && payment.getRefund().getStatus() == PaymentRefund.Status.SUCCEEDED,
                    "Financial conflicts require a confirmed refund before acknowledgement.");
        }
        payment.setResolutionNote(request.action() + ": " + request.evidence());
        payment.setResolvedAt(now());
        payment.setResolvedBy(actor);
        payment.setReconciliationIssue(null);
        return mapper.toPaymentResponse(payment);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefundResponse refund(Long id, RefundRequest request, Employee actor) {
        boolean created = Boolean.TRUE.equals(transactions.execute(ignored -> {
            Payment payment = settlement.lockById(id);
            if (refunds.findByPaymentId(id).isPresent()) {
                return false;
            }
            require(payment.getStatus() == Payment.PaymentStatus.SUCCEEDED
                    && !payment.isReconstructed() && payment.getCurrency() != null,
                    "A verified successful receipt with known currency is required.");
            require(!payment.isTipPaid(), "Reverse the tip payout before refunding this receipt.");
            require(payment.getMethod() == Payment.PaymentMethod.CASH
                    ? Boolean.TRUE.equals(request.confirmCashReturned())
                    : payment.getProviderPaymentId() != null,
                    "Confirm cash was returned, or link the online invoice first.");
            PaymentRefund refund = new PaymentRefund();
            refund.setPayment(payment);
            refund.setRequestedBy(actor);
            refund.setReason(request.reason());
            refund.setAmount(payment.getAmount().add(tips(payment)));
            refund.setCreatedAt(now());
            if (payment.getMethod() == Payment.PaymentMethod.CASH) {
                refund.setStatus(PaymentRefund.Status.SUCCEEDED);
                refund.setCompletedAt(now());
            }
            refunds.save(refund);
            return true;
        }));
        if (created) {
            execute(id, true, null);
        }
        return getRefund(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefundResponse retryRefund(Long id) {
        transactions.executeWithoutResult(ignored -> {
            settlement.lockById(id);
            PaymentRefund refund = refunds.findByPaymentId(id).orElseThrow();
            require(refund.getStatus() == PaymentRefund.Status.FAILED,
                    "Only a provider-confirmed failed refund can be retried.");
            refund.setAttempts(refund.getAttempts() + 1);
            refund.setProviderRefundId(null);
            refund.setError(null);
            refund.setStatus(PaymentRefund.Status.REQUESTED);
        });
        execute(id, true, null);
        return getRefund(id);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RefundResponse syncRefund(Long id, String providerRefundId) {
        execute(id, false, providerRefundId);
        return getRefund(id);
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(Long id) {
        return transactions.execute(ignored -> response(refunds.findByPaymentId(id)
                .orElseThrow(() -> new EntityNotFoundException("Refund not found."))));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncPendingRefunds() {
        var ids = transactions.execute(ignored -> refunds.findByStatusIn(List.of(
                PaymentRefund.Status.REQUESTED, PaymentRefund.Status.PENDING,
                PaymentRefund.Status.UNKNOWN)).stream().map(refund -> refund.getPayment().getId())
                .toList());
        for (Long id : ids) {
            execute(id, false, null);
        }
    }

    private void execute(Long id, boolean create, String suppliedId) {
        RefundWork work = transactions.execute(ignored -> {
            Payment payment = settlement.lockById(id);
            PaymentRefund refund = refunds.findByPaymentId(id)
                    .orElseThrow(() -> new EntityNotFoundException("Refund not found."));
            if (payment.getMethod() == Payment.PaymentMethod.CASH
                    || refund.getStatus() == PaymentRefund.Status.SUCCEEDED
                    || refund.getStatus() == PaymentRefund.Status.FAILED) {
                return null;
            }
            require(suppliedId == null || refund.getProviderRefundId() == null
                    || suppliedId.equals(refund.getProviderRefundId()), "Refund ID cannot change.");
            return new RefundWork(payment.getProviderType(), payment.getProviderPaymentId(),
                    "restaurant-refund-" + refund.getId() + "-" + refund.getAttempts(),
                    cents(payment),
                    payment.getCurrency().name(), suppliedId == null
                    ? refund.getProviderRefundId() : suppliedId, refund.getAttempts());
        });
        if (work == null) {
            return;
        }
        ProviderRefund result;
        try {
            var provider = providers.getPaymentProvider(work.provider());
            result = create ? provider.refund(work.invoice(), work.reference(),
                    work.amount(), work.currency()) : provider.inspectRefund(work.invoice(),
                    work.reference(), work.refundId(), work.amount(), work.currency());
            require(result != null && result.id() != null && result.status() != null
                    && (create || work.refundId() == null || work.refundId().equals(result.id()))
                    && result.amount() == work.amount()
                    && work.currency().equalsIgnoreCase(result.currency()),
                    "Refund result does not match the original receipt.");
        } catch (RuntimeException exception) {
            transactions.executeWithoutResult(ignored -> {
                settlement.lockById(id);
                PaymentRefund refund = refunds.findByPaymentId(id).orElseThrow();
                if (refund.getAttempts() == work.attempt()
                        && refund.getStatus() != PaymentRefund.Status.SUCCEEDED
                        && refund.getStatus() != PaymentRefund.Status.FAILED) {
                    refund.setStatus(PaymentRefund.Status.UNKNOWN);
                    String error = exception.getClass().getSimpleName() + ": "
                            + exception.getMessage();
                    refund.setError(error.substring(0, Math.min(error.length(), 1024)));
                }
            });
            return;
        }
        transactions.executeWithoutResult(ignored -> {
            settlement.lockById(id);
            PaymentRefund refund = refunds.findByPaymentId(id).orElseThrow();
            if (refund.getAttempts() != work.attempt()) {
                refund.getPayment().setReconciliationIssue(
                        "A previous refund attempt completed after a retry; "
                                + "verify provider history.");
                return;
            }
            if (refund.getStatus() != PaymentRefund.Status.SUCCEEDED
                    && refund.getStatus() != PaymentRefund.Status.FAILED) {
                refund.setProviderRefundId(result.id());
                refund.setStatus(result.status());
                refund.setError(null);
                if (result.status() == PaymentRefund.Status.SUCCEEDED) {
                    refund.setCompletedAt(now());
                }
            }
        });
    }

    private RefundResponse response(PaymentRefund refund) {
        return new RefundResponse(refund.getId(), refund.getPayment().getId(), refund.getStatus(),
                refund.getAmount(), refund.getPayment().getCurrency(), refund.getProviderRefundId(),
                refund.getError(), refund.getReason(), refund.getCreatedAt(),
                refund.getCompletedAt());
    }

    private long cents(Payment payment) {
        return payment.getAmount().add(tips(payment)).movePointRight(2).longValueExact();
    }

    private BigDecimal tips(Payment payment) {
        return Objects.requireNonNullElse(payment.getTipAmount(), BigDecimal.ZERO);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new ItemIllegalStateException(message);
        }
    }

    private record RefundWork(PaymentProviderType provider, String invoice, String reference,
                              long amount, String currency, String refundId, int attempt) {}
}
