package com.example.restaurant.entity.order;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dining_tables")
@Getter
@Setter
@NoArgsConstructor
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String qrUuid;
    @Column(nullable = false)
    private boolean isActive = true;

    public DiningTable(Long id) {
        this.id = id;
    }
}
