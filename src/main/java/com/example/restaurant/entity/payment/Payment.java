package com.example.restaurant.entity.payment;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.service.payment.PaymentProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private Restaurant.Currency currency;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method = PaymentMethod.ONLINE;
    @ManyToOne(fetch = FetchType.LAZY)
    private Employee recordedBy;
    @ManyToOne(fetch = FetchType.LAZY)
    private Employee tipEmployee;
    @Column(nullable = false)
    private boolean reconstructed = false;
    @Column(length = 1024)
    private String reconciliationIssue;
    @Column(unique = true, length = 64)
    private String reviewToken;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payer_guest_id", nullable = false)
    private Guest payerGuest;
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(precision = 12, scale = 2)
    private BigDecimal tipAmount;
    @Enumerated(EnumType.STRING)
    private PaymentProviderType providerType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;
    @Column(unique = true)
    private String providerPaymentId;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private OffsetDateTime providerModifiedAt;
    private String failureReason;
    private String providerErrorCode;
    private String providerErrorText;
    @Column(nullable = false)
    private int syncAttempts = 0;
    private LocalDateTime lastSyncAttemptAt;
    @Column(length = 1024)
    private String lastSyncError;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean tipPaid = false;
    private LocalDateTime tipPaidAt;
    @Column(length = 2048)
    private String paymentPageUrl;
    @ManyToMany
    @JoinTable(name = "payment_items",
            joinColumns = @JoinColumn(name = "payment_id"),
            inverseJoinColumns = @JoinColumn(name = "order_item_id"))
    private Set<OrderItem> orderItems = new HashSet<>();

    @Column(length = 2000)
    private String resolutionNote;
    private LocalDateTime resolvedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    private Employee resolvedBy;
    @OneToOne(mappedBy = "payment", fetch = FetchType.LAZY)
    private PaymentRefund refund;

    public enum PaymentStatus {
        PENDING,
        SUCCEEDED,
        FAILED,
        EXPIRED
    }

    public enum PaymentMethod {
        ONLINE,
        CASH
    }
}
