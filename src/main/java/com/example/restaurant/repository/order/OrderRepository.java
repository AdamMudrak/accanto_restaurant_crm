package com.example.restaurant.repository.order;

import com.example.restaurant.entity.order.Order;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> lockById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.uuid = :uuid AND o.status = :status")
    Optional<Order> lockByUuidAndStatus(@Param("uuid") String uuid,
                                      @Param("status") Order.OrderStatus status);

    boolean existsByStatus(Order.OrderStatus status);

    boolean existsByEmployeeIdAndStatus(Long employeeId, Order.OrderStatus status);

    @Query("SELECT o.id FROM Order o WHERE o.diningTable.id = :tableId AND o.status = 'OPENED'")
    Optional<Long> findOpenOrderIdByTableId(@Param("tableId") Long tableId);

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

    boolean existsByDiningTableIdAndStatus(Long diningTableId, Order.OrderStatus status);
}
