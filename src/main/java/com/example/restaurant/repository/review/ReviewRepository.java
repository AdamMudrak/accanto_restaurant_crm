package com.example.restaurant.repository.review;

import com.example.restaurant.entity.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByGuestIdAndOrderId(Long guestId, Long orderId);
}
