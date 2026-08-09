package com.example.restaurant.service.management;

import com.example.restaurant.dto.response.analytics.CashlessTipPaymentResponse;
import com.example.restaurant.dto.response.analytics.EmployeeTipsResponse;
import com.example.restaurant.dto.response.analytics.MonthlyAnalyticsResponse;
import com.example.restaurant.dto.response.list.analytics.CashlessTipPaymentListResponse;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.mapper.payment.CashlessTipPaymentMapper;
import com.example.restaurant.repository.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Transactional(readOnly = true)
    public MonthlyAnalyticsResponse getMonthlySummary(int year, int month) {
        DateRange dateRange = toDateRange(year, month);

        return new MonthlyAnalyticsResponse(
                year,
                month,
                paymentRepository.sumSucceededAmountBetween(
                        Payment.PaymentStatus.SUCCEEDED, dateRange.from(), dateRange.to()),
                paymentRepository.sumSucceededTipAmountBetween(
                        Payment.PaymentStatus.SUCCEEDED, dateRange.from(), dateRange.to()),
                paymentRepository.countSucceededPaymentsBetween(
                        Payment.PaymentStatus.SUCCEEDED, dateRange.from(), dateRange.to()));
    }

    @Transactional(readOnly = true)
    public List<EmployeeTipsResponse> getEmployeeTips(int year, int month) {
        DateRange dateRange = toDateRange(year, month);
        List<Payment> payments = paymentRepository.findSucceededTipPaymentsBetween(
                Payment.PaymentStatus.SUCCEEDED, dateRange.from(), dateRange.to());
        Map<Long, EmployeeTipAccumulator> tipsByEmployeeId = new HashMap<>();

        for (Payment payment : payments) {
            Employee employee = payment.getOrder().getEmployee();
            if (employee == null) {
                continue;
            }
            EmployeeTipAccumulator accumulator = tipsByEmployeeId.computeIfAbsent(
                    employee.getId(), employeeId -> new EmployeeTipAccumulator(employee));
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
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Payment with id " + paymentId + " not found."));

        if (payment.getStatus() != Payment.PaymentStatus.SUCCEEDED
                || payment.getTipAmount() == null
                || payment.getTipAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ItemIllegalStateException(
                    "Payment with id " + paymentId + " has no succeeded cashless tip.");
        }

        return payment;
    }

    private DateRange toDateRange(int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime from = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime to = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        return new DateRange(from, to);
    }

    private record DateRange(LocalDateTime from, LocalDateTime to) {}

    private static final class EmployeeTipAccumulator {
        private final Long employeeId;
        private final String firstName;
        private final String lastName;
        private BigDecimal totalTipAmount = BigDecimal.ZERO;
        private BigDecimal paidTipAmount = BigDecimal.ZERO;
        private BigDecimal unpaidTipAmount = BigDecimal.ZERO;
        private long paymentCount;

        private EmployeeTipAccumulator(Employee employee) {
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
                    paymentCount);
        }

        private String getFirstName() {
            return firstName;
        }

        private String getLastName() {
            return lastName;
        }
    }
}
