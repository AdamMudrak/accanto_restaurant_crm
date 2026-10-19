package com.example.restaurant.service.review;

import com.example.restaurant.dto.request.review.ReviewRequest;
import com.example.restaurant.dto.response.review.ReviewResponse;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.entity.payment.PaymentRefund;
import com.example.restaurant.entity.review.Review;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.repository.review.ReviewRepository;
import com.example.restaurant.service.payment.PaymentSettlementService;
import com.example.restaurant.service.utils.CookieExtractor;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {
    private static final int INVITATION_DAYS = 7;
    private final ReviewRepository reviewRepository;
    private final PaymentSettlementService settlementService;
    @Value("${front.base.url}")
    private String frontBaseUrl;

    public ReviewResponse create(ReviewRequest request, HttpServletRequest httpRequest) {
        Payment payment = settlementService.lockById(request.paymentId());
        validateAccess(payment, request.token(), httpRequest);
        if (reviewRepository.existsByGuestIdAndOrderId(
                payment.getPayerGuest().getId(), payment.getOrder().getId())) {
            throw new ItemIllegalStateException("This guest has already reviewed this order.");
        }
        Review review = new Review();
        review.setOrder(payment.getOrder());
        review.setGuest(payment.getPayerGuest());
        review.setPayment(payment);
        review.setRating(request.rating());
        review.setComment(request.comment() == null ? null : request.comment().strip());
        review.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        payment.setReviewToken(null);
        return ReviewResponse.from(reviewRepository.save(review));
    }

    public boolean eligible(Long paymentId, String token, HttpServletRequest request) {
        Payment payment = settlementService.lockById(paymentId);
        validateAccess(payment, token, request);
        return !reviewRepository.existsByGuestIdAndOrderId(
                payment.getPayerGuest().getId(), payment.getOrder().getId());
    }

    @Transactional(readOnly = true)
    public Page<ReviewResponse> list(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(ReviewResponse::from);
    }

    public Invitation invitation(Long paymentId) {
        Payment payment = settlementService.lockById(paymentId);
        if (!isReviewable(payment) || payment.getMethod() != Payment.PaymentMethod.CASH
                || payment.getReviewToken() == null
                || reviewRepository.existsByGuestIdAndOrderId(
                        payment.getPayerGuest().getId(), payment.getOrder().getId())) {
            throw new ItemIllegalStateException("No review invitation available for this payment.");
        }
        return new Invitation(paymentId, frontBaseUrl + "/review?paymentId=" + paymentId
                + "&token=" + payment.getReviewToken());
    }

    private void validateAccess(Payment payment, String token, HttpServletRequest request) {
        if (!isReviewable(payment)) {
            throw new ItemIllegalStateException("A recent successful payment is required.");
        }
        if (payment.getMethod() == Payment.PaymentMethod.CASH) {
            if (token == null || payment.getReviewToken() == null
                    || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                    payment.getReviewToken().getBytes(StandardCharsets.UTF_8))) {
                throw new InvalidTokenException("Invalid or used review invitation.");
            }
        } else if (!Objects.equals(payment.getPayerGuest().getUuid(),
                CookieExtractor.extractCookieValueByName(request, "guestUuid"))
                || !Objects.equals(payment.getOrder().getUuid(),
                CookieExtractor.extractCookieValueByName(request, "orderUuid"))) {
            throw new InvalidTokenException("Only the payer can review this payment.");
        }
    }

    private boolean isReviewable(Payment payment) {
        return payment.getStatus() == Payment.PaymentStatus.SUCCEEDED
                && payment.getReconciliationIssue() == null && !payment.isReconstructed()
                && (payment.getRefund() == null || payment.getRefund().getStatus()
                == PaymentRefund.Status.FAILED)
                && payment.getCompletedAt() != null
                && !payment.getCompletedAt().isBefore(
                        LocalDateTime.now(ZoneOffset.UTC).minusDays(INVITATION_DAYS));
    }

    public record Invitation(Long paymentId, String url) {}
}
