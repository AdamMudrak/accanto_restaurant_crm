package com.example.restaurant.repository.management;

import com.example.restaurant.entity.management.WorkingDay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkingDayRepository extends JpaRepository<WorkingDay, Long> {
}
