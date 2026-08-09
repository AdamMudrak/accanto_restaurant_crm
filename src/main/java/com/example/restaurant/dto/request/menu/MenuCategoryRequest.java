package com.example.restaurant.dto.request.menu;

import com.example.restaurant.dto.annotations.strings.category.CategoryName;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@CategoryName
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Payload used to create or rename a menu category.")
public record MenuCategoryRequest(
        @Schema(description = "Menu category display name in default language.", example = "Coffee")
        String nameDefaultLanguage,
        @Schema(description = "Menu category display name in second language.",
                example = "Кава")
        String nameSecondLanguage
){}
