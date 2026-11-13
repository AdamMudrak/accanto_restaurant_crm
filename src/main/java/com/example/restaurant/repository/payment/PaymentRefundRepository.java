package com.example.restaurant.repository.payment;

import com.example.restaurant.entity.payment.PaymentRefund;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByPaymentId(Long paymentId);

    List<PaymentRefund> findByStatusIn(Collection<PaymentRefund.Status> statuses);

    List<PaymentRefund> findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
            PaymentRefund.Status status, LocalDateTime from, LocalDateTime to);
}
