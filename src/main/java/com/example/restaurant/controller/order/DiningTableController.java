package com.example.restaurant.controller.order;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.RESTAURANT_TABLES_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.RESTAURANT_TABLE_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.VALIDATION_ERROR;

import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.order.DiningTableListResponse;
import com.example.restaurant.dto.response.order.DiningTableResponse;
import com.example.restaurant.service.order.diningtable.DiningTableAdminService;
import com.example.restaurant.service.order.diningtable.DiningTableGuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/dining-tables")
@Tag(
        name = "Dining tables",
        description = "Dining table management and guest QR access.")
public class DiningTableController {
    private final DiningTableAdminService diningTableAdminService;
    private final DiningTableGuestService diningTableGuestService;

    @Operation(
            summary = "Create dining table",
            description = "Creates an active dining table with a generated QR UUID. "
                    + "Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dining table created.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_TABLE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @PostMapping
    public ResponseEntity<ApiResponse<DiningTableResponse>> createDiningTable() {
        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully created dining table.",
                        diningTableAdminService.createDiningTable()),
                        HttpStatus.OK);
    }

    @Operation(
            summary = "Update dining table activity",
            description = "Updates whether one dining table is active. "
                    + "Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dining table updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_TABLE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Dining table was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DiningTableResponse>> updateDiningTable(
            @PathVariable @Positive Long id,
            @Parameter(
                    description = "Whether the dining table should be active.",
                    example = "true")
            @RequestParam boolean isActive) {
        DiningTableResponse diningTableResponse =
                diningTableAdminService.updateDiningTableById(id, isActive);

        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully updated dining table.",
                        diningTableResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Get dining table",
            description = "Returns one dining table by identifier. "
                    + "Set activeRequired=true to require an active table.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dining table retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_TABLE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Dining table was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiningTableResponse>> getDiningTable(
            @PathVariable @Positive Long id,
            @Parameter(
                    description = "Whether only an active dining table should be returned.",
                    example = "false")
            @RequestParam(defaultValue = "false") boolean activeRequired) {
        DiningTableResponse diningTableResponse;

        if (activeRequired) {
            diningTableResponse = diningTableAdminService.getActiveDiningTableById(id);
        } else {
            diningTableResponse = diningTableAdminService.getDiningTableById(id);
        }

        return new ResponseEntity<>(
                ApiResponse.success("Successfully retrieved dining table.",
                        diningTableResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "List dining tables",
            description = "Returns paginated dining tables. Set activeRequired=true to "
                    + "return only active tables.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Dining tables per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Dining table sort in property,direction format.",
                    example = "id,asc"),
            @Parameter(
                    name = "activeRequired",
                    description = "Whether only active dining tables should be returned.",
                    example = "false")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dining tables retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_TABLES_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have access.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            DiningTableResponse>>> getAllDiningTables(
            @Parameter(hidden = true) Pageable pageable,
            @RequestParam(defaultValue = "false") boolean activeRequired) {
        DiningTableListResponse diningTableListResponse;

        if (activeRequired) {
            diningTableListResponse = diningTableAdminService.getAllActiveDiningTables(pageable);
        } else {
            diningTableListResponse = diningTableAdminService.getAllDiningTables(pageable);
        }

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Successfully retrieved dining tables.",
                        diningTableListResponse.items(),
                        diningTableListResponse.pageNumber(),
                        diningTableListResponse.pageSize(),
                        diningTableListResponse.totalElements()), HttpStatus.OK);
    }

    @Operation(
            summary = "Get dining table QR code",
            description = "Generates a PNG QR code for an active dining table UUID.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dining table QR code generated.",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Active dining table was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @GetMapping("/qr/{uuid}")
    public ResponseEntity<byte[]> getDiningTableQr(@PathVariable String uuid) {
        byte[] qrCode = diningTableAdminService.generateDiningTableQrByUuid(uuid);

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCode);
    }

    @Operation(
            summary = "Get guest dining table",
            description = "Returns an active dining table by QR UUID for guest flows.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Dining table retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_TABLE_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Active dining table was not found.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping("/guest")
    public ResponseEntity<ApiResponse<Void>> interactWithTable(
            @RequestParam String uuid,
            HttpServletRequest request,
            HttpServletResponse response) {
        diningTableGuestService.interactWithTableByQrUuid(uuid, request, response);

        return new ResponseEntity<>(
                ApiResponse.emptySuccess("Successfully retrieved dining table."),
                HttpStatus.OK);
    }
}
