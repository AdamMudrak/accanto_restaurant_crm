package com.example.restaurant.dto.request.menu;

import com.example.restaurant.dto.annotations.strings.item.ItemName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@ItemName
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Payload used to create a menu item.")
public record MenuItemRequest(
        @Schema(description = "Menu item display name in default language.", example = "Latte")
        String nameDefaultLanguage,
        @Schema(description = "Menu item display name in second language.", example = "Лате")
        String nameSecondLanguage,
        @Schema(description = "Short menu item description in default language.",
                example = "Espresso with steamed milk and a light milk foam layer.")
        String descriptionDefaultLanguage,
        @Schema(description = "Short menu item description in second language.",
                example = "Еспресо з пареним молоком та легкою молочною пінкою.")
        String descriptionSecondLanguage,
        @Schema(description = "Ingredients shown to guests in default language.",
                example = "[\"Espresso\", \"Milk\", \"Milk foam\"]")
        List<String> ingredientsDefaultLanguage,
        @Schema(description = "Ingredients shown to guests in second language.",
                example = "[\"Еспресо\", \"Молоко\", \"Молочна пінка\"]")
        List<String> ingredientsSecondLanguage,
        @Schema(description = "Whether this menu item contains allergens.", example = "true")
        @NotNull
        boolean isAllergenic,
        @Schema(description = "Whether this menu item is vegetarian.", example = "false")
        @NotNull
        boolean isVegetarian,
        @Schema(description = "Whether this menu item is vegan.", example = "false")
        @NotNull
        boolean isVegan,
        @Schema(description = "Maximum quantity one guest can order.",
                example = "4",
                nullable = true)
        @Positive
        Integer maxQuantityPerPerson,
        @Schema(description = "Menu item weight in grams.",
                example = "250",
                nullable = true)
        @Positive
        Integer weightGrams,
        @Schema(description = "Menu item price.", example = "105.00")
        @NotNull
        @Positive
        BigDecimal price,
        @Schema(description = "Child menu category identifier.", example = "3")
        @NotNull
        @Positive
        Long childCategoryId
) {}
