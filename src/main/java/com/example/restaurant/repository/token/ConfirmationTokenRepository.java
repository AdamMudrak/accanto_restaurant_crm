package com.example.restaurant.repository.token;

import com.example.restaurant.entity.token.UuidToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfirmationTokenRepository extends JpaRepository<UuidToken, Long> {

    Optional<UuidToken> findByUuidAndEmployeeId(String uuid, Long employeeId);

    void deleteAllByEmployeeId(Long employeeId);
}
