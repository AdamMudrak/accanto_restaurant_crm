package com.example.restaurant.controller.menu;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.BAD_REQUEST_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.EMPTY_MENU_ITEM_SUCCESS;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.MENU_ITEMS_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.MENU_ITEM_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.MENU_ITEM_VALIDATION_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;

import com.example.restaurant.dto.request.menu.MenuItemRequest;
import com.example.restaurant.dto.request.menu.UpdateMenuItemRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.menu.MenuItemListResponse;
import com.example.restaurant.dto.response.menu.MenuItemResponse;
import com.example.restaurant.service.menu.MenuItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/menu-items")
@Tag(
        name = "Menu items",
        description = "Menu item catalog management.")
public class MenuItemController {
    private final MenuItemService menuItemService;

    @Operation(
            summary = "Get menu item",
            description = "Returns a menu item by identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu item retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_ITEM_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Menu item was not found.",
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
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getMenuItem(
            @PathVariable @Positive Long id) {
        MenuItemResponse menuItemResponse = menuItemService.findMenuItemById(id);

        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved menu item", menuItemResponse), HttpStatus.OK);
    }

    @Operation(
            summary = "List menu items",
            description = "Returns paginated menu items.")
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Menu items per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Menu item sort in property,direction format.",
                    example = "nameDefaultLanguage,asc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu items retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_ITEMS_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            MenuItemResponse>>> getMenuItems(
                                    @Parameter(hidden = true) Pageable pageable) {
        MenuItemListResponse menuItemListResponse = menuItemService.findAllMenuItems(pageable);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Menu items retrieved successfully.",
                        menuItemListResponse.items(),
                        menuItemListResponse.pageNumber(),
                        menuItemListResponse.pageSize(),
                        menuItemListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "List menu items by child category",
            description = "Returns paginated menu items for one child menu category.")
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Menu items per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Menu item sort in property,direction format.",
                    example = "price,asc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu items retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_ITEMS_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Child category was not found or is not a child category.",
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
    @GetMapping("/category/{childCategoryId}")
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            MenuItemResponse>>> getMenuItems(
                                    @Parameter(hidden = true) Pageable pageable,
                                    @PathVariable @Positive Long childCategoryId) {
        MenuItemListResponse menuItemListResponse =
                menuItemService.findAllMenuItemsByCategoryId(pageable, childCategoryId);

        return new ResponseEntity<>(
                ApiResponse.pageSuccess(
                        "Menu items retrieved successfully.",
                        menuItemListResponse.items(),
                        menuItemListResponse.pageNumber(),
                        menuItemListResponse.pageSize(),
                        menuItemListResponse.totalElements()),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Create menu item",
            description = "Creates a menu item in a child menu category. The request is sent as "
                    + "multipart parts: menuItem JSON and optional itemImage.",
            security = @SecurityRequirement(name = "accessTokenCookie"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = @Encoding(
                                    name = "menuItem",
                                    contentType = MediaType.APPLICATION_JSON_VALUE))))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu item created.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_ITEM_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid image or request value.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = MENU_ITEM_VALIDATION_ERROR),
                                    @ExampleObject(value = BAD_REQUEST_ERROR)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Child category was not found or is not a child category.",
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
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MenuItemResponse>> createMenuItem(
            @RequestPart(value = "menuItem") @Valid MenuItemRequest menuItemRequest,
            @RequestPart(value = "itemImage", required = false) MultipartFile itemImage) {
        MenuItemResponse menuItemResponse =
                menuItemService.createMenuItem(menuItemRequest, itemImage);

        return new ResponseEntity<>(ApiResponse.success(
                "Successfully created menu item",
                menuItemResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Update menu item",
            description = "Updates present menu item fields. The request is sent as multipart "
                    + "parts: menuItem JSON and optional itemImage.",
            security = @SecurityRequirement(name = "accessTokenCookie"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                            encoding = @Encoding(
                                    name = "menuItem",
                                    contentType = MediaType.APPLICATION_JSON_VALUE))))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu item updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_ITEM_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid image or request value.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(value = MENU_ITEM_VALIDATION_ERROR),
                                    @ExampleObject(value = BAD_REQUEST_ERROR)
                            })),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Menu item or child category was not found.",
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
    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateMenuItem(
            @PathVariable @Positive Long id,
            @RequestPart(value = "menuItem", required = false)
            @Valid UpdateMenuItemRequest menuItemRequest,
            @RequestPart(value = "itemImage", required = false) MultipartFile itemImage) {
        MenuItemResponse menuItemResponse =
                menuItemService.updateMenuItem(id, menuItemRequest, itemImage);

        return new ResponseEntity<>(ApiResponse.success(
                "Successfully updated menu item",
                menuItemResponse),
                HttpStatus.OK);
    }

    @Operation(
            summary = "Delete menu item",
            description = "Marks a menu item as unavailable for new orders. "
                    + "Historical order references are preserved. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu item deleted.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPTY_MENU_ITEM_SUCCESS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Menu item was not found.",
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
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMenuItem(@PathVariable @Positive Long id) {
        menuItemService.deleteMenuItem(id);

        return ResponseEntity.ok(ApiResponse.emptySuccess("Successfully deleted menu item."));
    }
}
