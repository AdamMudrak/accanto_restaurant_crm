package com.example.restaurant.repository.payment;

import com.example.restaurant.entity.payment.MonobankPublicKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonobankPublicKeyRepository extends JpaRepository<MonobankPublicKey, Long> {
    Optional<MonobankPublicKey> findFirstByOrderByIdAsc();
}
