package com.example.restaurant.entity.order;

import com.example.restaurant.entity.menu.MenuItem;
import com.example.restaurant.entity.payment.Payment;
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
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItem menuItem;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;
    @Column(nullable = false)
    private String itemNameSnapshot;
    @Positive
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal itemPriceSnapshot;
    @Positive
    @Column(nullable = false)
    private Integer quantity;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderItemStatus status = OrderItemStatus.ORDERED;
    private String clientNote;
    private String employeeNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FulfillmentStatus fulfillmentStatus = FulfillmentStatus.ORDERED;
    @Column(length = 128)
    private String requestKey;
    @Column(length = 64)
    private String requestFingerprint;

    public OrderItem(Long id) {
        this.id = id;
    }

    public void setStatus(OrderItemStatus status) {
        this.status = status;
        if (status != OrderItemStatus.PAID_CARD && status != OrderItemStatus.PAID_CASH) {
            this.fulfillmentStatus = FulfillmentStatus.valueOf(status.name());
        } else if (fulfillmentStatus == FulfillmentStatus.ORDERED) {
            this.fulfillmentStatus = FulfillmentStatus.CONFIRMED;
        }
    }

    public enum FulfillmentStatus {
        ORDERED, CONFIRMED, SERVED, CANCELED
    }

    public enum OrderItemStatus {
        ORDERED,
        CONFIRMED,
        CANCELED,
        SERVED,
        PAID_CASH,
        PAID_CARD
    }
}
