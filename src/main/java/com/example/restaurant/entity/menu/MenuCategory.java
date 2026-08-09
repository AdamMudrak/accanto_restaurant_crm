package com.example.restaurant.entity.menu;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu_categories")
@Getter
@Setter
@NoArgsConstructor
public class MenuCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nameDefaultLanguage;
    private String nameSecondLanguage;
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private MenuCategory parentCategory;

    public MenuCategory(Long id) {
        this.id = id;
    }
}
