package com.example.restaurant.dto.request.menu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Payload used to update present menu item fields.")
public record UpdateMenuItemRequest(
        @Schema(description = "Menu item display name in default language.",
                example = "Chocolate Milkshake")
        String nameDefaultLanguage,
        @Schema(description = "Menu item display name in second language.",
                example = "Шоколадний мілкшейк")
        String nameSecondLanguage,
        @Schema(description = "Short menu item description in default language.",
                example = "Cold cocoa milkshake topped with whipped cream.")
        String descriptionDefaultLanguage,
        @Schema(description = "Short menu item description in second language.",
                example = "Холодний какао-мілкшейк зі збитими вершками.")
        String descriptionSecondLanguage,
        @Schema(description = "Ingredients shown to guests in default language.",
                example = "[\"Milk\", \"Cocoa\", \"Ice cream\", \"Whipped cream\"]")
        List<String> ingredientsDefaultLanguage,
        @Schema(description = "Ingredients shown to guests in second language.",
                example = "[\"Молоко\", \"Какао\", \"Морозиво\", \"Збиті вершки\"]")
        List<String> ingredientsSecondLanguage,
        @Schema(description = "Whether this menu item contains allergens.", example = "true")
        Boolean isAllergenic,
        @Schema(description = "Whether this menu item is vegetarian.", example = "true")
        Boolean isVegetarian,
        @Schema(description = "Whether this menu item is vegan.", example = "false")
        Boolean isVegan,
        @Schema(description = "Whether this menu item is available for new orders.",
                example = "true")
        Boolean isAvailable,
        @Schema(description = "Maximum quantity one guest can order.",
                example = "3",
                nullable = true)
        @Positive
        Integer maxQuantityPerPerson,
        @Schema(description = "Menu item weight in grams.",
                example = "250",
                nullable = true)
        @Positive
        Integer weightGrams,
        @Schema(description = "Menu item price.", example = "130.00")
        @Positive
        BigDecimal price,
        @Schema(description = "Child menu category identifier.", example = "5")
        @Positive
        Long childCategoryId
) {}
