package com.example.restaurant.entity.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "uuid_tokens")
@Getter
@Setter
@NoArgsConstructor
public class UuidToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType tokenType;
    @Column(nullable = false, unique = true)
    private String uuid;
    @Column(nullable = false)
    private Long employeeId;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public enum TokenType {
        REGISTRATION_CONFIRMATION,
        PASSWORD_RESET_CONFIRMATION,
        CHANGE_EMAIL_CONFIRMATION
    }
}
