package com.example.restaurant.repository.payment;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.payment.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) AS revenue,
                COALESCE(SUM(p.tipAmount), 0) AS tips, COUNT(p) AS paymentCount
            FROM Payment p WHERE p.status = 'SUCCEEDED' AND p.method = 'ONLINE'
                AND p.currency = :currency AND p.completedAt >= :from AND p.completedAt < :to
            """)
    OnlineTotals summarizeOnlineBetween(@Param("currency") Restaurant.Currency currency,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    interface OnlineTotals {
        BigDecimal getRevenue();

        BigDecimal getTips();

        long getPaymentCount();
    }

    @Query("""
            SELECT p.currency AS currency,
                SUM(CASE WHEN p.method = 'CASH' THEN p.amount ELSE 0 END) AS cashRevenue,
                SUM(CASE WHEN p.method = 'ONLINE' THEN p.amount ELSE 0 END) AS onlineRevenue,
                SUM(CASE WHEN p.method = 'ONLINE' THEN p.tipAmount ELSE 0 END) AS onlineTips,
                COUNT(p) AS paymentCount, COUNT(DISTINCT p.order.id) AS orderCount
            FROM Payment p WHERE p.status = 'SUCCEEDED'
                AND p.completedAt >= :from AND p.completedAt < :to
            GROUP BY p.currency ORDER BY p.currency
            """)
    List<RevenueTotals> summarizeBetween(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    interface RevenueTotals {
        Restaurant.Currency getCurrency();

        BigDecimal getCashRevenue();

        BigDecimal getOnlineRevenue();

        BigDecimal getOnlineTips();

        long getPaymentCount();

        long getOrderCount();
    }

    @Query("SELECT p.order.id FROM Payment p WHERE p.providerPaymentId = :providerId")
    Optional<Long> findOrderIdByProviderId(@Param("providerId") String providerId);

    @Query("SELECT p.order.id FROM Payment p WHERE p.id = :id")
    Optional<Long> findOrderIdByPaymentId(@Param("id") Long id);

    List<Payment> findAllByOrderIdAndStatus(Long orderId, Payment.PaymentStatus status);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'SUCCEEDED' "
            + "AND (p.currency IS NULL OR p.completedAt IS NULL)")
    long countUnresolvedHistory();

    long countByReconciliationIssueIsNotNull();

    @Query("""
            SELECT p FROM Payment p LEFT JOIN p.refund r
            WHERE p.reconciliationIssue IS NOT NULL
                OR (p.status = 'PENDING' AND p.method = 'ONLINE'
                    AND p.providerPaymentId IS NULL)
                OR r.status IN ('UNKNOWN', 'FAILED')
            """)
    Page<Payment> findNeedingReconciliation(Pageable pageable);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Page<Payment> findAllByOrderId(Long orderId, Pageable pageable);

    List<Payment> findAllByStatusAndMethodAndProviderPaymentIdIsNotNull(
            Payment.PaymentStatus status, Payment.PaymentMethod method);

    @Query("""
            SELECT payment FROM Payment payment
            WHERE payment.status = :status AND payment.method = 'ONLINE'
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            AND payment.tipAmount IS NOT NULL
            AND payment.tipAmount > 0
            AND NOT EXISTS (SELECT r.id FROM PaymentRefund r
                WHERE r.payment = payment AND r.status = 'SUCCEEDED')
            AND (:tipPaid IS NULL OR payment.tipPaid = :tipPaid)
            """)
    Page<Payment> findSucceededTipPaymentsBetween(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("tipPaid") Boolean tipPaid,
            Pageable pageable);

    @Query("""
            SELECT payment
            FROM Payment payment
            WHERE payment.status = :status AND payment.method = 'ONLINE'
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            AND payment.tipAmount IS NOT NULL
            AND payment.tipAmount > 0
            AND NOT EXISTS (SELECT r.id FROM PaymentRefund r
                WHERE r.payment = payment AND r.status = 'SUCCEEDED')
            """)
    List<Payment> findSucceededTipPaymentsBetween(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
