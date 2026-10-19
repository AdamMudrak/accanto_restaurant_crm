package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.dto.request.user.FinalizeEmailChangeRequest;
import com.example.restaurant.dto.request.user.UpdateEmployeeRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.token.UuidToken;
import com.example.restaurant.exception.types.conflict.EmployeeEmailAlreadyExistsException;
import com.example.restaurant.exception.types.conflict.NoPendingEmailException;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.conflict.PendingEmailAlreadyExistsException;
import com.example.restaurant.exception.types.forbidden.SelfModificationNotAllowedException;
import com.example.restaurant.exception.types.notfound.ImageNotFoundException;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class EmployeeIntegrationTest extends AbstractIntegrationTest {
    @Test
    void profileUpdatePersistsNamesPhoneAndReplacesPictureWithoutDeletingEmployeeHistory() {
        var first = employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest("New", "Name", null, "0501112234", null, null), png());
        Long firstPicture = employees.findById(2L).orElseThrow().getProfilePicture().getId();
        var second = employeeService.updateEmployeeById(1L, 2L, null, png());
        assertThat(second.firstName()).isEqualTo("New");
        assertThat(second.lastName()).isEqualTo("Name");
        assertThat(second.phone()).isEqualTo("0501112234");
        assertThatThrownBy(() -> images.findById(firstPicture))
                .isInstanceOf(ImageNotFoundException.class);
        assertThat(employeeService.getEmployeeById(first.id()).id()).isEqualTo(second.id());
    }

    @Test
    void emailChangeRequiresConfirmationAndClearsAuthenticationCookies() {
        employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, "new-email@example.com", null, null, null), null);
        var pending = employees.findById(2L).orElseThrow();
        assertThat(pending.getEmail()).isNotEqualTo("new-email@example.com");
        assertThat(pending.getPendingEmail()).isEqualTo("new-email@example.com");
        var token = confirmationTokens.findAll().getFirst();
        assertThat(token.getTokenType()).isEqualTo(
                UuidToken.TokenType.CHANGE_EMAIL_CONFIRMATION);
        var request = new FinalizeEmailChangeRequest();
        request.setEmployeeId(2L);
        request.setUuid(token.getUuid());
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThat(employeeService.finalizeEmailChange(request, response).email())
                .isEqualTo("new-email@example.com");
        assertThat(employees.findById(2L).orElseThrow().getPendingEmail()).isNull();
        assertThat(confirmationTokens.count()).isZero();
        assertThat(response.getCookie("accessToken").getMaxAge()).isZero();
        assertThat(response.getCookie("refreshToken").getMaxAge()).isZero();
    }

    @Test
    void existingAndPendingEmailAddressesCannotBeClaimedByAnotherEmployee() {
        String used = employees.findById(1L).orElseThrow().getEmail();
        assertThatThrownBy(() -> employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, used, null, null, null), null))
                .isInstanceOf(EmployeeEmailAlreadyExistsException.class);
        employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, "pending@example.com", null, null, null), null);
        assertThatThrownBy(() -> employeeService.updateEmployeeById(1L, 3L,
                new UpdateEmployeeRequest(null, null, "pending@example.com", null, null, null), null))
                .isInstanceOf(PendingEmailAlreadyExistsException.class);
        assertThat(confirmationTokens.count()).isEqualTo(1);
    }

    @Test
    void unchangedEmailDoesNotGenerateConfirmationAndEmployeeCanUpdateOwnProfile() throws Exception {
        String email = employees.findById(2L).orElseThrow().getEmail();
        var updated = multipart("PUT", "/employees/me", Map.of("employeeRequest",
                Map.of("firstName", "Updated", "email", email)), false, employeeCookie());
        assertThat(updated.status()).isEqualTo(200);
        assertThat(updated.data().path("firstName").asText()).isEqualTo("Updated");
        assertThat(confirmationTokens.count()).isZero();
        assertThat(request("GET", "/employees/2", null, adminCookie()).data().path("email").asText())
                .isEqualTo(email);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "deleted"})
    void employeesCannotChangeOwnAdministrativeFlags(String flag) {
        assertThatThrownBy(() -> employeeService.updateEmployeeById(2L, 2L,
                new UpdateEmployeeRequest(null, null, null, null,
                        flag.equals("admin") ? true : null, flag.equals("deleted") ? true : null), null))
                .isInstanceOf(SelfModificationNotAllowedException.class);
    }

    @Test
    void administratorCanPromoteDemoteDeleteAndRestoreOtherEmployee() throws Exception {
        employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, null, null, true, null), null);
        assertThat(request("GET", "/employees", null, employeeCookie()).status()).isEqualTo(200);
        employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, null, null, false, null), null);
        assertThat(request("GET", "/employees", null, employeeCookie()).status()).isEqualTo(403);
        assertThat(request("DELETE", "/employees/2", null, adminCookie()).status()).isEqualTo(200);
        employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, null, null, null, false), null);
        assertThat(request("GET", "/employees/me", null, employeeCookie()).status()).isEqualTo(200);
    }

    @Test
    void administratorCanBeRemovedWhenAnotherActivatedAdministratorExists() {
        employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, null, null, true, null), null);
        employeeService.deleteEmployee(1L);
        assertThat(employees.findById(1L).orElseThrow().isDeleted()).isTrue();
        assertThat(employeeService.getEmployees(PageRequest.of(0, 1)).totalElements()).isEqualTo(3);
    }

    @Test
    void emailConfirmationRejectsMissingPendingEmailWithoutConsumingToken() {
        var token = new UuidToken();
        token.setEmployeeId(2L);
        token.setUuid(UUID.randomUUID().toString());
        token.setTokenType(UuidToken.TokenType.CHANGE_EMAIL_CONFIRMATION);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        confirmationTokens.save(token);
        var request = new FinalizeEmailChangeRequest();
        request.setEmployeeId(2L);
        request.setUuid(token.getUuid());
        assertThatThrownBy(() -> employeeService.finalizeEmailChange(request, new MockHttpServletResponse()))
                .isInstanceOf(NoPendingEmailException.class);
        assertThat(confirmationTokens.count()).isEqualTo(1);
    }

    @Test
    void missingEmployeeReadsUpdatesAndDeletionReturnNotFound() throws Exception {
        assertThat(request("GET", "/employees/" + MISSING_ID, null, adminCookie()).status()).isEqualTo(404);
        assertThat(request("DELETE", "/employees/" + MISSING_ID, null, adminCookie()).status()).isEqualTo(404);
        assertThat(multipart("PUT", "/employees/" + MISSING_ID, Map.of(), false,
                adminCookie()).status()).isEqualTo(404);
    }
    @Test
    void employeeDeletionRequiresReassignmentAndPreservesHistory() {
        Fixture fixture = fixture(Restaurant.Currency.UAH);
        assertThatThrownBy(() -> employeeService.deleteEmployee(2L))
                .isInstanceOf(OrderIllegalStateException.class);
        orderService.reassignEmployee(fixture.orderId(), 3L);
        employeeService.deleteEmployee(2L);
        employeeService.deleteEmployee(2L);
        assertThat(employees.findById(2L).orElseThrow().isDeleted()).isTrue();
        assertThat(employeeService.getEmployees(PageRequest.of(0, 100)).employees())
                .noneMatch(employee -> employee.id().equals(2L));
        assertThat(orders.existsById(fixture.orderId())).isTrue();
    }

    @Test
    void lastAdministratorCannotBeDeleted() {
        assertThatThrownBy(() -> employeeService.deleteEmployee(1L))
                .isInstanceOf(OrderIllegalStateException.class);
    }

    @Test
    void updateRouteCannotBypassEmployeeRemovalGuards() {
        fixture(Restaurant.Currency.UAH);
        assertThatThrownBy(() -> employeeService.updateEmployeeById(1L, 2L,
                new UpdateEmployeeRequest(null, null, null, null, null, true), null))
                .isInstanceOf(OrderIllegalStateException.class);
        assertThatThrownBy(() -> employeeService.updateEmployeeById(2L, 1L,
                new UpdateEmployeeRequest(null, null, null, null, false, null), null))
                .isInstanceOf(OrderIllegalStateException.class);
    }

    @Test
    void deletedEmployeeCannotUseExistingAccessOrRefreshToken() throws Exception {
        String access = tokens.generateAccessToken(employees.findById(2L).orElseThrow().getEmail());
        String refresh = tokens.generateRefreshToken(employees.findById(2L).orElseThrow().getEmail());
        employeeService.deleteEmployee(2L);
        assertThat(http("GET", "/employees/me", null, "accessToken=" + access)).isEqualTo(403);
        assertThat(http("GET", "/employees/me", null, "refreshToken=" + refresh)).isEqualTo(403);
    }

}
