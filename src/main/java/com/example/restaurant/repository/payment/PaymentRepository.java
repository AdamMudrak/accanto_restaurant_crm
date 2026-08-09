package com.example.restaurant.repository.payment;

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
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Page<Payment> findAllByOrderId(Long orderId, Pageable pageable);

    List<Payment> findAllByStatusAndProviderPaymentIdIsNotNullAndSyncAttemptsLessThan(
            Payment.PaymentStatus status, int syncAttempts);

    List<Payment> findAllByStatusAndProviderTypeAndCreatedAtBefore(
            Payment.PaymentStatus status,
            com.example.restaurant.service.payment.PaymentProviderType providerType,
            LocalDateTime createdBefore);

    @Query("""
            SELECT COALESCE(SUM(payment.amount), 0) FROM Payment payment
            WHERE payment.status = :status
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            """)
    BigDecimal sumSucceededAmountBetween(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COALESCE(SUM(payment.tipAmount), 0) FROM Payment payment
            WHERE payment.status = :status
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            """)
    BigDecimal sumSucceededTipAmountBetween(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT COUNT(payment) FROM Payment payment
            WHERE payment.status = :status
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            """)
    long countSucceededPaymentsBetween(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            SELECT payment FROM Payment payment
            WHERE payment.status = :status
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            AND payment.tipAmount IS NOT NULL
            AND payment.tipAmount > 0
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
            WHERE payment.status = :status
            AND payment.completedAt >= :from
            AND payment.completedAt < :to
            AND payment.tipAmount IS NOT NULL
            AND payment.tipAmount > 0
            """)
    List<Payment> findSucceededTipPaymentsBetween(
            @Param("status") Payment.PaymentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
