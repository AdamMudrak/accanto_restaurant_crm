package com.example.restaurant.repository.order;

import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.order.OrderItem;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    Optional<OrderItem> findByGuestIdAndRequestKey(Long guestId, String requestKey);

    @Query("SELECT i.order.id FROM OrderItem i WHERE i.id = :id")
    Optional<Long> findOrderIdByItemId(@Param("id") Long id);

    Optional<OrderItem> findByIdAndOrder_Status(Long orderItemId, Order.OrderStatus orderStatus);

    List<OrderItem> findAllByOrderId(Long orderId);

    Optional<OrderItem> findByIdAndGuestIdAndOrderId(Long id, Long guestId, Long orderId);

    List<OrderItem> findAllByOrderIdAndGuestIdAndMenuItemIdAndStatusNot(
            Long orderId, Long guestId, Long menuItemId, OrderItem.OrderItemStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderItem> findByIdAndOrderId(Long id, Long orderId);
}
