package com.example.restaurant.dto.response.list.menu;

import com.example.restaurant.dto.response.menu.MenuItemResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Internal service result for a paginated menu item list.")
public record MenuItemListResponse(
        @Schema(description = "Menu items on the current page.")
        List<MenuItemResponse> items,
        @Schema(description = "Zero-based page index.", example = "0")
        int pageNumber,
        @Schema(description = "Requested page size.", example = "20")
        int pageSize,
        @Schema(description = "Total number of matching menu items.", example = "42")
        long totalElements
) {}
