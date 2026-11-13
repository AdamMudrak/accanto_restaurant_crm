package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.review.ReviewRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.order.OrderItem;
import com.example.restaurant.entity.payment.Payment;
import com.example.restaurant.exception.types.conflict.ItemIllegalStateException;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class ReviewIntegrationTest extends AbstractIntegrationTest {
    @Test
    void employeeIssuesCashInvitationAndGuestSubmitsViaItsUrl() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        Long id = payments.findAll().getFirst().getId();
        var invitation = request("GET", "/reviews/invitations/" + id, null, employeeCookie());
        assertThat(invitation.status()).isEqualTo(200);
        String url = invitation.data().path("url").asText();
        String token = url.substring(url.indexOf("&token=") + 7);
        assertThat(request("GET", "/reviews/eligibility?paymentId=" + id + "&token=" + token, null, null)
                .data().asBoolean()).isTrue();
        assertThat(request("POST", "/reviews", new ReviewRequest(id, 5, null, token), null).status()).isEqualTo(200);
        assertThat(request("GET", "/reviews/invitations/" + id, null, employeeCookie()).status()).isEqualTo(409);
    }
    @Test
    void publicReviewAndEligibilityAndPrivateAdminListingWorkThroughApi() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.SUCCEEDED);
        assertThat(request("GET", "/reviews/eligibility?paymentId=" + id, null, guestCookie(fixture))
                .data().asBoolean()).isTrue();
        var submitted = request("POST", "/reviews", new ReviewRequest(id, 5, "<b>Good</b>", null),
                guestCookie(fixture));
        assertThat(submitted.status()).isEqualTo(200);
        assertThat(submitted.data().path("comment").asText()).isEqualTo("<b>Good</b>");
        assertThat(request("GET", "/reviews/eligibility?paymentId=" + id, null, guestCookie(fixture))
                .data().asBoolean()).isFalse();
        assertThat(request("GET", "/reviews?page=0&size=1", null, adminCookie()).data()
                .path("meta").path("total").asLong()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "failed", "expired", "reconstructed", "conflict", "unknown-time"})
    void reviewsRequireVerifiedRecentUnambiguousPayment(String problem) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Payment.PaymentStatus status = switch (problem) {
            case "pending" -> Payment.PaymentStatus.PENDING;
            case "failed" -> Payment.PaymentStatus.FAILED;
            case "expired" -> Payment.PaymentStatus.EXPIRED;
            default -> Payment.PaymentStatus.SUCCEEDED;
        };
        Long id = addOnlinePayment(fixture, status);
        if (problem.equals("reconstructed")) {
            jdbc.update("UPDATE payments SET reconstructed = true WHERE id = ?", id);
        } else if (problem.equals("conflict")) {
            jdbc.update("UPDATE payments SET reconciliation_issue = 'Check' WHERE id = ?", id);
        } else if (problem.equals("unknown-time")) {
            jdbc.update("UPDATE payments SET completed_at = NULL WHERE id = ?", id);
        }
        assertThat(request("POST", "/reviews", new ReviewRequest(id, 5, null, null), guestCookie(fixture))
                .status()).isEqualTo(409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"zero", "six", "missing", "long-comment"})
    void invalidReviewPayloadsDoNotConsumeCashInvitation(String problem) throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        Payment payment = payments.findAll().getFirst();
        Map<String, Object> body = new HashMap<>();
        body.put("paymentId", payment.getId());
        body.put("token", payment.getReviewToken());
        if (!problem.equals("missing")) {
            body.put("rating", problem.equals("zero") ? 0 : problem.equals("six") ? 6 : 5);
        }
        body.put("comment", problem.equals("long-comment") ? "x".repeat(2001) : "Fine");
        assertThat(request("POST", "/reviews", body, null).status()).isEqualTo(400);
        assertThat(payments.findById(payment.getId()).orElseThrow().getReviewToken()).isNotNull();
    }

    @Test
    void reviewLimitIsPerGuestAndOrderEvenWhenGuestMadeSeveralPayments() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long first = addOnlinePayment(fixture, Payment.PaymentStatus.SUCCEEDED);
        Long second = addOnlinePayment(additionalItem(fixture, OrderItem.OrderItemStatus.CONFIRMED),
                Payment.PaymentStatus.SUCCEEDED);
        reviews.create(new ReviewRequest(first, 5, null, null), guestRequest(fixture));
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(second, 4, null, null), guestRequest(fixture)))
                .isInstanceOf(ItemIllegalStateException.class);
    }

    @Test
    void cashInvitationCannotBeIssuedForOnlinePaymentAndCannotBeUsedForOtherCashReceipt() throws Exception {
        Fixture online = fixture(Restaurant.Currency.UAH);
        Long onlineId = addOnlinePayment(online, Payment.PaymentStatus.SUCCEEDED);
        assertThat(request("GET", "/reviews/invitations/" + onlineId, null, employeeCookie()).status()).isEqualTo(409);
        closeCash(fixture(Restaurant.Currency.UAH));
        Payment first = payments.findAll().stream().filter(payment -> payment.getMethod() == Payment.PaymentMethod.CASH)
                .findFirst().orElseThrow();
        closeCash(fixture(Restaurant.Currency.UAH));
        Payment other = payments.findAll().stream().filter(payment -> payment.getMethod() == Payment.PaymentMethod.CASH
                && !payment.getId().equals(first.getId())).findFirst().orElseThrow();
        assertThat(request("POST", "/reviews", new ReviewRequest(other.getId(), 5, null, first.getReviewToken()), null)
                .status()).isEqualTo(401);
    }

    @Test
    void concurrentCashReviewSubmissionsConsumeInvitationOnlyOnce() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        Payment payment = payments.findAll().getFirst();
        var command = new ReviewRequest(payment.getId(), 5, null, payment.getReviewToken());
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> submit = () -> {
                start.await();
                return request("POST", "/reviews", command, null).status();
            };
            var first = executor.submit(submit);
            var second = executor.submit(submit);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(200, 401);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reviews", Integer.class)).isEqualTo(1);
    }
    @Test
    void cashReviewRequiresOneUseTokenAndDoesNotExposePublicReviewList() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        Payment payment = payments.findAll().getFirst();
        String token = payment.getReviewToken();
        assertThat(reviews.invitation(payment.getId()).url()).contains(token);
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(payment.getId(), 5, "Good", null),
                guestRequest(fixture))).isInstanceOf(InvalidTokenException.class);
        assertThat(reviews.create(new ReviewRequest(payment.getId(), 5, " Good ", token),
                new MockHttpServletRequest()).comment()).isEqualTo("Good");
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(payment.getId(), 4, null, token),
                new MockHttpServletRequest())).isInstanceOf(InvalidTokenException.class);
        assertThat(http("GET", "/reviews", null, null)).isEqualTo(403);
    }

    @Test
    void onlineReviewRequiresSuccessfulPaymentAndPayerAndIsUniquePerOrder() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.PENDING);
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(id, 4, null, null),
                guestRequest(fixture))).isInstanceOf(ItemIllegalStateException.class);
        transactions.executeWithoutResult(ignored -> settlement.applyStatus(
                settlement.lockById(id), Payment.PaymentStatus.SUCCEEDED,
                OffsetDateTime.now(ZoneOffset.UTC)));
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(id, 4, null, null),
                new MockHttpServletRequest())).isInstanceOf(InvalidTokenException.class);
        reviews.create(new ReviewRequest(id, 4, null, null), guestRequest(fixture));
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(id, 4, null, null),
                guestRequest(fixture))).isInstanceOf(ItemIllegalStateException.class);
    }

    @Test
    void expiredReviewInvitationIsRejected() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        closeCash(fixture);
        Payment payment = payments.findAll().getFirst();
        jdbc.update("UPDATE payments SET completed_at = ? WHERE id = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(8), payment.getId());
        assertThatThrownBy(() -> reviews.create(new ReviewRequest(payment.getId(), 5, null,
                payment.getReviewToken()), new MockHttpServletRequest()))
                .isInstanceOf(ItemIllegalStateException.class);
    }

    @Test
    void reviewApiRejectsInvalidRatingAndEmployeeCannotDeleteEmployee() throws Exception {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        Long id = addOnlinePayment(fixture, Payment.PaymentStatus.SUCCEEDED);
        assertThat(http("POST", "/reviews", "{\"paymentId\":" + id + ",\"rating\":6}",
                "guestUuid=" + fixture.guestUuid() + "; orderUuid=" + fixture.orderUuid()))
                .isEqualTo(400);
        String access = tokens.generateAccessToken(employees.findById(2L).orElseThrow().getEmail());
        assertThat(http("DELETE", "/employees/3", null, "accessToken=" + access)).isEqualTo(403);
        assertThat(http("GET", "/reviews/eligibility?paymentId=9223372036854775000", null, null))
                .isEqualTo(404);
    }

}
