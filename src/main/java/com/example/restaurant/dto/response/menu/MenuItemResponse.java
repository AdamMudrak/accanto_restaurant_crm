package com.example.restaurant.dto.response.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Menu item returned by the API.")
public record MenuItemResponse(
        @Schema(description = "Menu item identifier.", example = "2")
        Long id,
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
        boolean isAllergenic,
        @Schema(description = "Whether this menu item is vegetarian.", example = "true")
        boolean isVegetarian,
        @Schema(description = "Whether this menu item is vegan.", example = "false")
        boolean isVegan,
        @Schema(description = "Whether this menu item is available for new orders.",
                example = "true")
        boolean isAvailable,
        @Schema(description = "Maximum quantity one guest can order.",
                example = "4",
                nullable = true)
        Integer maxQuantityPerPerson,
        @Schema(description = "Menu item weight in grams.",
                example = "250",
                nullable = true)
        Integer weightGrams,
        @Schema(description = "Menu item price.", example = "105.00")
        BigDecimal price,
        @Schema(description = "Menu item image identifier.",
                example = "4",
                nullable = true)
        Long imageId,
        @Schema(description = "Child menu category identifier.", example = "3")
        Long childCategoryId
) {}
