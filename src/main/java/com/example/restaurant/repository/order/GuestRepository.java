package com.example.restaurant.repository.order;

import com.example.restaurant.entity.order.Guest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    Optional<Guest> findByUuid(String uuid);
}
