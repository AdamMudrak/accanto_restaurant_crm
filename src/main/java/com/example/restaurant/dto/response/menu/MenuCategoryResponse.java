package com.example.restaurant.dto.response.menu;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Menu category returned by the API.")
public record MenuCategoryResponse(
        @Schema(description = "Menu category identifier.", example = "3")
        Long id,
        @Schema(description = "Menu category display name in default language.", example = "Coffee")
        String nameDefaultLanguage,
        @Schema(description = "Menu category display name in second language.",
                example = "Кава")
        String nameSecondLanguage,
        @Schema(
                description = "Parent menu category identifier. "
                        + "Null when this category is a parent.",
                example = "1",
                nullable = true)
        Long parentCategoryId) {}
