package com.example.restaurant.dto.response.list.menu;

import com.example.restaurant.dto.response.menu.MenuCategoryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Internal service result for a paginated menu category list.")
public record MenuCategoryListResponse(
        @Schema(description = "Menu categories on the current page.")
        List<MenuCategoryResponse> menuCategoryResponses,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching menu categories.", example = "42")
        long totalElements
) {}
