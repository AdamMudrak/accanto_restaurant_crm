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

    public OrderItem(Long id) {
        this.id = id;
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
