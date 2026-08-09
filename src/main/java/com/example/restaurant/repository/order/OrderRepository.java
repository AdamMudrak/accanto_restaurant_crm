package com.example.restaurant.repository.order;

import com.example.restaurant.entity.order.Order;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByStatus(Order.OrderStatus orderStatus, Pageable pageable);

    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
            AND (:from IS NULL OR o.createdAt >= :from)
            AND (:to IS NULL OR o.createdAt <= :to)
            """)
    Page<Order> findHistory(
            @Param("status") Order.OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    Optional<Order> findByIdAndStatus(Long id, Order.OrderStatus status);

    Optional<Order> findByUuidAndStatus(String uuid, Order.OrderStatus status);

    Optional<Order> findByDiningTableIdAndStatus(Long diningTableId, Order.OrderStatus status);

    boolean existsByDiningTableIdAndStatus(Long diningTableId, Order.OrderStatus status);
}
