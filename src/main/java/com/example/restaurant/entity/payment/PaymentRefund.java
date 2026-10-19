package com.example.restaurant.entity.payment;

import com.example.restaurant.entity.user.Employee;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payment_refunds")
@Getter
@Setter
@NoArgsConstructor
public class PaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Employee requestedBy;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.REQUESTED;
    @Column(nullable = false)
    private int attempts = 1;
    private String providerRefundId;
    @Column(length = 1024)
    private String error;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public enum Status {
        REQUESTED, PENDING, SUCCEEDED, FAILED, UNKNOWN
    }
}
