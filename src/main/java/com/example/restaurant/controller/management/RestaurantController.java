package com.example.restaurant.controller.management;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.BAD_REQUEST_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.FORBIDDEN_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.NOT_FOUND_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.RESTAURANT_RESPONSE;

import com.example.restaurant.dto.request.management.TimeZoneRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.management.RestaurantResponse;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.service.management.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/restaurant")
@Tag(
        name = "Restaurant",
        description = "Restaurant profile, branding images, and language settings.")
public class RestaurantController {
    private final RestaurantService restaurantService;

    @Operation(
            summary = "Get restaurant",
            description = "Returns the single restaurant profile used by the application.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Restaurant retrieved.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Restaurant was not found.",
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
    @GetMapping("/this")
    public ResponseEntity<ApiResponse<RestaurantResponse>> getRestaurant() {
        RestaurantResponse restaurantResponse = restaurantService.getRestaurant();

        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully retrieved restaurant.", restaurantResponse), HttpStatus.OK);
    }

    @Operation(
            summary = "Update restaurant",
            description = "Updates present restaurant fields. Logo and background image are "
                    + "sent as multipart parts.",
            security = @SecurityRequirement(name = "accessTokenCookie"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Restaurant updated.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = RESTAURANT_RESPONSE))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid image or request value.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = BAD_REQUEST_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Authenticated user does not have admin privileges.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = FORBIDDEN_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Restaurant was not found.",
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
    @PutMapping(path = "/this", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<RestaurantResponse>> updateRestaurant(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Restaurant.DefaultRegion defaultRegion,
            @Parameter(description = "Confirms menu prices in the new currency; no conversion.")
            @RequestParam(defaultValue = "false") boolean confirmMenuPrices,
            @RequestPart(value = "logo", required = false) MultipartFile logo,
            @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage
    ) {
        RestaurantResponse restaurantResponse = restaurantService.updateRestaurant(
                name, defaultRegion, confirmMenuPrices, logo, backgroundImage);

        return new ResponseEntity<>(
                ApiResponse.success(
                        "Successfully updated restaurant.", restaurantResponse), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @PatchMapping("/this/time-zone")
    @Operation(summary = "Select restaurant IANA time zone for business reporting")
    public ApiResponse<RestaurantResponse> updateTimeZone(
            @Valid @RequestBody
            TimeZoneRequest request) {
        return ApiResponse.success("Restaurant time zone updated.",
                restaurantService.updateTimeZone(request.timeZone()));
    }

}
