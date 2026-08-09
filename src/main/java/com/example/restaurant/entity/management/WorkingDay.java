package com.example.restaurant.entity.management;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "working_days")
@Getter
@Setter
@NoArgsConstructor
public class WorkingDay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek day;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isActive = false;
    @Column(nullable = false)
    private LocalTime openAt;
    @Column(nullable = false)
    private LocalTime closeAt;
}
