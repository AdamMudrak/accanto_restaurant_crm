package com.example.restaurant.dto.response.review;

import com.example.restaurant.entity.review.Review;
import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long orderId, Long guestId, Long paymentId,
                             int rating, String comment, LocalDateTime createdAt) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getOrder().getId(),
                review.getGuest().getId(), review.getPayment().getId(), review.getRating(),
                review.getComment(), review.getCreatedAt());
    }
}
