package com.example.restaurant.entity.menu;

import com.example.restaurant.entity.image.Image;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nameDefaultLanguage;
    private String nameSecondLanguage;
    private String descriptionDefaultLanguage;
    private String descriptionSecondLanguage;
    @ElementCollection
    @CollectionTable(
            name = "menu_item_ingredients_default_language",
            joinColumns = @JoinColumn(name = "menu_item_id")
    )
    @Column(name = "ingredient")
    private List<String> ingredientsDefaultLanguage;
    @ElementCollection
    @CollectionTable(
            name = "menu_item_ingredients_second_language",
            joinColumns = @JoinColumn(name = "menu_item_id")
    )
    @Column(name = "ingredient")
    private List<String> ingredientsSecondLanguage;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isAllergenic = false;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isVegetarian = false;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isVegan = false;
    @Column(nullable = false, columnDefinition = "TINYINT(1)")
    private boolean isAvailable = true;
    private Integer maxQuantityPerPerson;
    private Integer weightGrams;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_image_id")
    private Image itemImage;
    @ManyToOne
    @JoinColumn(name = "menu_category_id", nullable = false)
    private MenuCategory menuCategory;
}
