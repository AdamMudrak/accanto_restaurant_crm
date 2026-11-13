package com.example.restaurant.controller.review;

import com.example.restaurant.dto.request.review.ReviewRequest;
import com.example.restaurant.dto.response.ApiResponse;
import com.example.restaurant.dto.response.review.ReviewResponse;
import com.example.restaurant.service.review.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Validated
@Tag(name = "Reviews", description = "Verified post-payment guest reviews; private to admins.")
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Review a paid visit",
            description = "Rating 1-5, comment up to 2000 characters. One review per guest "
                    + "and order within seven days. Requires payer cookies or cash token.")
    public ApiResponse<ReviewResponse> create(@Valid @RequestBody ReviewRequest request,
                                             HttpServletRequest httpRequest) {
        return ApiResponse.success("Review submitted.", reviewService.create(request, httpRequest));
    }

    @GetMapping("/eligibility")
    @Operation(summary = "Check whether a verified payer can submit a review")
    public ApiResponse<Boolean> eligible(@RequestParam @Positive Long paymentId,
                                        @RequestParam(required = false)
                                        @Size(max = 64) String token,
                                        HttpServletRequest request) {
        return ApiResponse.success("Review eligibility.",
                reviewService.eligible(paymentId, token, request));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_EMPLOYEE')")
    @Operation(summary = "List guest reviews for administration")
    public ApiResponse<ApiResponse.PageResponse<ReviewResponse>> list(Pageable pageable) {
        Page<ReviewResponse> page = reviewService.list(pageable);
        return ApiResponse.pageSuccess("Reviews retrieved.", page.getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @GetMapping("/invitations/{paymentId}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPLOYEE', 'EMPLOYEE')")
    @Operation(summary = "Get one-use review link for a cash receipt")
    public ApiResponse<ReviewService.Invitation> invitation(
            @PathVariable @Positive Long paymentId) {
        return ApiResponse.success("Review invitation.", reviewService.invitation(paymentId));
    }
}
