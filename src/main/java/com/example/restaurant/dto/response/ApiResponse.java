package com.example.restaurant.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Standard API response wrapper.")
public record ApiResponse<T>(
        @Schema(description = "Whether the operation completed successfully.", example = "true")
        boolean success,
        @Schema(description = "Human-readable success message.", example = "Operation completed.")
        String message,
        @Schema(description = "Response payload. Null for empty success or error responses.")
        T data,
        @Schema(description = "Validation or business errors. Null for successful responses.")
        List<String> errors
) {
    @Schema(description = "Paginated response payload.")
    public record PageResponse<T>(
            @Schema(description = "Page items.")
            List<T> items,
            @Schema(description = "Pagination metadata.")
            Meta meta
    ) {
        @Schema(description = "Pagination metadata.")
        public record Meta(
                @Schema(description = "Zero-based page index.", example = "0")
                int page,
                @Schema(description = "Requested page size.", example = "20")
                int size,
                @Schema(description = "Total number of matching items.", example = "42")
                long total
        ) {}
    }

    public static ApiResponse<Void> emptySuccess(String message) {
        return new ApiResponse<>(true, message,null,null);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<PageResponse<T>> pageSuccess(
            String message,
            List<T> items,
            int page,
            int size,
            long total
    ) {
        PageResponse.Meta meta = new PageResponse.Meta(page, size, total);
        PageResponse<T> pageResponse = new PageResponse<>(items, meta);

        return new ApiResponse<>(true, message, pageResponse, null);
    }

    public static <T> ApiResponse<T> error(String error) {
        return new ApiResponse<>(false,null, null, List.of(error));
    }

    public static <T> ApiResponse<T> error(List<String> errors) {
        return new ApiResponse<>(false,null, null, errors);
    }
}
