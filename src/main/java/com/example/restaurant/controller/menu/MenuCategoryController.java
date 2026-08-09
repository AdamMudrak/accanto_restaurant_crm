package com.example.restaurant.controller.menu;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.CHILD_MENU_CATEGORIES_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.EMPTY_MENU_CATEGORY_SUCCESS;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.MENU_CATEGORY_CONFLICT_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.MENU_CATEGORY_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.MENU_CATEGORY_VALIDATION_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.PARENT_MENU_CATEGORIES_RESPONSE;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.PARENT_MENU_CATEGORY_RESPONSE;

import com.example.restaurant.dto.request.menu.MenuCategoryRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.list.menu.MenuCategoryListResponse;
import com.example.restaurant.dto.response.menu.MenuCategoryResponse;
import com.example.restaurant.service.menu.MenuCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/menu-categories")
@Tag(
        name = "Menu categories",
        description = "Parent and child menu category management.")
public class MenuCategoryController {
    private final MenuCategoryService menuCategoryService;

    @Operation(
            summary = "Get menu category",
            description = "Returns a parent or child menu category by identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Menu category retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Menu category was not found.",
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
    public ResponseEntity<
            ApiResponse<
                    MenuCategoryResponse>> getMenuCategoryById(@PathVariable Long id) {
        MenuCategoryResponse menuCategoryResponse = menuCategoryService.findById(id);

        return ResponseEntity.ok(ApiResponse.success(
                "Retrieved menu category successfully.", menuCategoryResponse));
    }

    @Operation(
            summary = "List parent menu categories",
            description = "Returns paginated root menu categories. Parent categories have no "
                    + "parentCategoryId.")
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Menu categories per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Menu category sort in property,direction format.",
                    example = "nameDefaultLanguage,asc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Parent menu categories retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PARENT_MENU_CATEGORIES_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping("/parents")
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            MenuCategoryResponse>>> getParentMenuCategories(
                                    @Parameter(hidden = true) Pageable pageable) {
        MenuCategoryListResponse menuCategoryListResponse =
                menuCategoryService.findAllParentCategories(pageable);

        return ResponseEntity.ok(ApiResponse.pageSuccess(
                "Retrieved parent menu categories successfully.",
                menuCategoryListResponse.menuCategoryResponses(),
                menuCategoryListResponse.pageNumber(),
                menuCategoryListResponse.pageSize(),
                menuCategoryListResponse.totalElements()));
    }

    @Operation(
            summary = "List child menu categories",
            description = "Returns paginated child categories for one parent menu category.")
    @Parameters({
            @Parameter(name = "page", description = "Zero-based page index.", example = "0"),
            @Parameter(name = "size", description = "Menu categories per page.", example = "20"),
            @Parameter(
                    name = "sort",
                    description = "Menu category sort in property,direction format.",
                    example = "nameSecondLanguage,asc")
    })
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Child menu categories retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = CHILD_MENU_CATEGORIES_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Parent category was not found or is not a parent category.",
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
    @GetMapping("/children/{parentCategoryId}")
    public ResponseEntity<
            ApiResponse<
                    ApiResponse.PageResponse<
                            MenuCategoryResponse>>> getChildrenMenuCategories(
                                    @PathVariable Long parentCategoryId,
                                    @Parameter(hidden = true) Pageable pageable) {
        MenuCategoryListResponse menuCategoryListResponse =
                menuCategoryService.findAllChildCategories(parentCategoryId, pageable);

        return ResponseEntity.ok(ApiResponse.pageSuccess(
                "Retrieved children menu categories successfully.",
                menuCategoryListResponse.menuCategoryResponses(),
                menuCategoryListResponse.pageNumber(),
                menuCategoryListResponse.pageSize(),
                menuCategoryListResponse.totalElements()));
    }

    @Operation(
            summary = "Create parent menu category",
            description = "Creates a root menu category. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Parent menu category created.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PARENT_MENU_CATEGORY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_VALIDATION_ERROR))),
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
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createParentCategory(
            @Valid @RequestBody MenuCategoryRequest request) {
        MenuCategoryResponse menuCategoryResponse =
                menuCategoryService.createParentCategory(request);

        return ResponseEntity.ok(ApiResponse.success(
                "Created parent menu category successfully.", menuCategoryResponse));
    }

    @Operation(
            summary = "Create child menu category",
            description = "Creates a child category under an existing parent category. "
                    + "Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Child menu category created.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Parent category was not found or is not a parent category.",
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
    @PostMapping("/parents/{parentCategoryId}/children")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> createChildCategory(
            @PathVariable Long parentCategoryId, @Valid @RequestBody MenuCategoryRequest request) {
        MenuCategoryResponse menuCategoryResponse =
                menuCategoryService.createChildCategory(parentCategoryId, request);

        return ResponseEntity.ok(ApiResponse.success(
                "Created child menu category successfully.", menuCategoryResponse));
    }

    @Operation(
            summary = "Update parent menu category",
            description = "Updates the name of a parent category. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Parent menu category updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = PARENT_MENU_CATEGORY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category was not found or is not a parent category.",
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
    @PutMapping("/parents/{parentCategoryId}")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> updateParentCategory(
            @PathVariable Long parentCategoryId, @Valid @RequestBody MenuCategoryRequest request) {
        MenuCategoryResponse menuCategoryResponse =
                menuCategoryService.updateParentCategory(parentCategoryId, request);

        return ResponseEntity.ok(ApiResponse.success(
                "Updated parent menu category successfully.", menuCategoryResponse));
    }

    @Operation(
            summary = "Update child menu category",
            description = "Updates the name of a child category without moving it to another "
                    + "parent. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Child menu category updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category was not found or is not a child category.",
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
    @PutMapping("/children/{childCategoryId}")
    public ResponseEntity<ApiResponse<MenuCategoryResponse>> updateChildCategory(
            @PathVariable Long childCategoryId, @Valid @RequestBody MenuCategoryRequest request) {
        MenuCategoryResponse menuCategoryResponse =
                menuCategoryService.updateChildCategory(childCategoryId, request);

        return ResponseEntity.ok(ApiResponse.success(
                "Updated child menu category successfully.", menuCategoryResponse));
    }

    @Operation(
            summary = "Delete parent menu category",
            description = "Deletes an empty parent category. Parent categories with children "
                    + "cannot be deleted. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Parent menu category deleted.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPTY_MENU_CATEGORY_SUCCESS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category was not found or is not a parent category.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Parent category has child categories.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @DeleteMapping("/parents/{parentCategoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteParentCategoryById(
            @PathVariable Long parentCategoryId) {
        menuCategoryService.deleteParentCategoryById(parentCategoryId);

        return ResponseEntity.ok(ApiResponse.emptySuccess(
                "Deleted parent menu category successfully."));
    }

    @Operation(
            summary = "Delete child menu category",
            description = "Deletes an empty child category. Child categories with menu items "
                    + "cannot be deleted. Admin access is required.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Child menu category deleted.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = EMPTY_MENU_CATEGORY_SUCCESS))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Category was not found or is not a child category.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = NOT_FOUND_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "Child category has menu items.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = MENU_CATEGORY_CONFLICT_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @DeleteMapping("/children/{childCategoryId}")
    public ResponseEntity<ApiResponse<Void>> deleteChildCategoryById(
            @PathVariable Long childCategoryId) {
        menuCategoryService.deleteChildCategoryById(childCategoryId);

        return ResponseEntity.ok(ApiResponse.emptySuccess(
                "Deleted child menu category successfully."));
    }
}
