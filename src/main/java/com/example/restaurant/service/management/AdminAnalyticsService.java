package com.example.restaurant.service.management;

import com.example.restaurant.dto.response.analytics.CashlessTipPaymentResponse;
import com.example.restaurant.dto.response.analytics.EmployeeTipsResponse;
import com.example.restaurant.dto.response.analytics.MonthlyAnalyticsResponse;
import com.example.restaurant.dto.response.analytics.RevenueSummaryResponse;
import com.example.restaurant.dto.response.list.analytics.CashlessTipPaymentListResponse;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.mapper.payment.CashlessTipPaymentMapper;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.payment.PaymentRefundRepository;
import com.example.restaurant.repository.payment.PaymentRepository;
import com.example.restaurant.service.payment.PaymentSettlementService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminAnalyticsService {
    private final PaymentRepository paymentRepository;
    private final CashlessTipPaymentMapper cashlessTipPaymentMapper;
    private final RestaurantRepository restaurantRepository;
    private final PaymentSettlementService settlement;
    private final PaymentRefundRepository refunds;

    @Transactional(readOnly = true)
    public RevenueSummaryResponse getRevenueSummary(int year, int month) {
        DateRange range = toDateRange(year, month);
        return new RevenueSummaryResponse(year, month,
                paymentRepository.summarizeBetween(range.from(), range.to()).stream()
                        .map(total -> new RevenueSummaryResponse.CurrencyRevenue(
                                total.getCurrency(), total.getCashRevenue(),
                                total.getOnlineRevenue(),
                                total.getCashRevenue().add(total.getOnlineRevenue()),
                                total.getOnlineTips(), total.getPaymentCount(),
                                total.getOrderCount()))
                        .toList(), paymentRepository.countUnresolvedHistory(),
                paymentRepository.countByReconciliationIssueIsNotNull(),
                restaurantRepository.findSingleton().orElseThrow().getTimeZone(),
                refundTotals(range));
    }

    @Transactional(readOnly = true)
    public MonthlyAnalyticsResponse getMonthlySummary(int year, int month,
                                                      Restaurant.Currency currency) {
        DateRange dateRange = toDateRange(year, month);
        Restaurant.Currency selected = currency == null
                ? restaurantRepository.findSingleton().orElseThrow().getCurrency() : currency;
        PaymentRepository.OnlineTotals totals = paymentRepository.summarizeOnlineBetween(
                selected, dateRange.from(), dateRange.to());
        return new MonthlyAnalyticsResponse(
                year,
                month,
                totals.getRevenue(), totals.getTips(), totals.getPaymentCount(), selected);
    }

    @Transactional(readOnly = true)
    public List<EmployeeTipsResponse> getEmployeeTips(int year, int month) {
        DateRange dateRange = toDateRange(year, month);
        List<Payment> payments = paymentRepository.findSucceededTipPaymentsBetween(
                Payment.PaymentStatus.SUCCEEDED, dateRange.from(), dateRange.to());
        Map<EmployeeCurrency, EmployeeTipAccumulator> tipsByEmployeeId = new HashMap<>();

        for (Payment payment : payments) {
            Employee employee = payment.getTipEmployee();
            if (employee == null) {
                continue;
            }
            EmployeeTipAccumulator accumulator = tipsByEmployeeId.computeIfAbsent(
                    new EmployeeCurrency(employee.getId(), payment.getCurrency()),
                    key -> new EmployeeTipAccumulator(employee, payment.getCurrency()));
            accumulator.add(payment);
        }

        return new ArrayList<>(tipsByEmployeeId.values()).stream()
                .sorted(Comparator
                        .comparing(EmployeeTipAccumulator::getLastName)
                        .thenComparing(EmployeeTipAccumulator::getFirstName))
                .map(EmployeeTipAccumulator::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CashlessTipPaymentListResponse getTipPayments(
            int year,
            int month,
            Boolean tipPaid,
            Pageable pageable) {
        DateRange dateRange = toDateRange(year, month);
        Page<Payment> payments = paymentRepository.findSucceededTipPaymentsBetween(
                Payment.PaymentStatus.SUCCEEDED, dateRange.from(), dateRange.to(),
                tipPaid, pageable);
        List<CashlessTipPaymentResponse> items = payments.getContent().stream()
                .map(cashlessTipPaymentMapper::toResponse)
                .toList();

        return new CashlessTipPaymentListResponse(
                items,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                payments.getTotalElements());
    }

    public CashlessTipPaymentResponse markTipPaid(Long paymentId) {
        Payment payment = findCashlessTipPayment(paymentId);
        if (payment.getRefund() != null && payment.getRefund().getStatus()
                != PaymentRefund.Status.FAILED) {
            throw new ItemIllegalStateException("Refunded or reserved tips cannot be paid out.");
        }
        payment.setTipPaid(true);
        payment.setTipPaidAt(LocalDateTime.now());

        return cashlessTipPaymentMapper.toResponse(paymentRepository.save(payment));
    }

    public CashlessTipPaymentResponse markTipUnpaid(Long paymentId) {
        Payment payment = findCashlessTipPayment(paymentId);
        payment.setTipPaid(false);
        payment.setTipPaidAt(null);

        return cashlessTipPaymentMapper.toResponse(paymentRepository.save(payment));
    }

    private Payment findCashlessTipPayment(Long paymentId) {
        Payment payment = settlement.lockById(paymentId);

        if (payment.getMethod() != Payment.PaymentMethod.ONLINE
                || payment.getStatus() != Payment.PaymentStatus.SUCCEEDED
                || payment.getTipAmount() == null
                || payment.getTipAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemIllegalStateException(
                    "Payment with id " + paymentId + " has no succeeded cashless tip.");
        }

        return payment;
    }

    private List<RevenueSummaryResponse.CurrencyRefunds> refundTotals(DateRange range) {
        Map<Restaurant.Currency, List<PaymentRefund>> groups =
                refunds.findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                        PaymentRefund.Status.SUCCEEDED,
                        range.from(), range.to()).stream().collect(Collectors
                        .groupingBy(refund -> refund.getPayment().getCurrency()));
        return groups.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .map(entry -> new RevenueSummaryResponse.CurrencyRefunds(entry.getKey(),
                        entry.getValue().stream().map(refund -> refund.getPayment().getAmount())
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().stream().map(refund -> refund.getAmount()
                                .subtract(refund.getPayment().getAmount()))
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                        entry.getValue().size())).toList();
    }

    private DateRange toDateRange(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        ZoneId zone = ZoneId.of(restaurantRepository.findSingleton()
                .orElseThrow().getTimeZone());
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay(zone)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone)
                .withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();

        return new DateRange(from, to);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {}

    private record EmployeeCurrency(Long employeeId, Restaurant.Currency currency) {}

    private static final class EmployeeTipAccumulator {
        private final Restaurant.Currency currency;
        private final Long employeeId;
        private final String firstName;
        private final String lastName;
        private BigDecimal totalTipAmount = BigDecimal.ZERO;
        private BigDecimal paidTipAmount = BigDecimal.ZERO;
        private BigDecimal unpaidTipAmount = BigDecimal.ZERO;
        private long paymentCount;

        private EmployeeTipAccumulator(Employee employee, Restaurant.Currency currency) {
            this.currency = currency;
            this.employeeId = employee.getId();
            this.firstName = employee.getFirstName();
            this.lastName = employee.getLastName();
        }

        private void add(Payment payment) {
            BigDecimal tipAmount = payment.getTipAmount();

            totalTipAmount = totalTipAmount.add(tipAmount);
            if (payment.isTipPaid()) {
                paidTipAmount = paidTipAmount.add(tipAmount);
            } else {
                unpaidTipAmount = unpaidTipAmount.add(tipAmount);
            }
            paymentCount++;
        }

        private EmployeeTipsResponse toResponse() {
            return new EmployeeTipsResponse(
                    employeeId,
                    firstName,
                    lastName,
                    totalTipAmount,
                    paidTipAmount,
                    unpaidTipAmount,
                    paymentCount, currency);
        }

        private String getFirstName() {
            return firstName;
        }

        private String getLastName() {
            return lastName;
        }
    }
}
