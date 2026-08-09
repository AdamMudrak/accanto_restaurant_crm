package com.example.restaurant.repository.order;

import com.example.restaurant.entity.order.DiningTable;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    boolean existsByQrUuidAndIsActiveTrue(String qrUuid);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DiningTable> findByQrUuidAndIsActiveTrue(String qrUuid);

    Optional<DiningTable> findByIdAndIsActiveTrue(Long id);

    Page<DiningTable> findAllByIsActiveTrue(Pageable pageable);
}
