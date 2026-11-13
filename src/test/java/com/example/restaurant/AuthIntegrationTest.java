package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.example.restaurant.dto.request.security.ChangePasswordRequest;
import com.example.restaurant.dto.request.security.FinalizeEmployeeActionRequest;
import com.example.restaurant.dto.request.security.LoginRequest;
import com.example.restaurant.dto.request.security.RegisterEmployeeRequest;
import com.example.restaurant.dto.request.security.TriggerEmailActionRequest;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.token.UuidToken;
import com.example.restaurant.exception.types.conflict.EmailAlreadyConfirmedException;
import com.example.restaurant.exception.types.conflict.EmployeeEmailAlreadyExistsException;
import com.example.restaurant.exception.types.forbidden.AccountNotActivatedException;
import com.example.restaurant.exception.types.forbidden.DeletedEmployeeException;
import com.example.restaurant.exception.types.gone.ExpiredTokenException;
import com.example.restaurant.exception.types.unauthorized.InvalidTokenException;
import com.example.restaurant.service.email.dtos.EmailContent;
import com.example.restaurant.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationServiceException;

class AuthIntegrationTest extends AbstractIntegrationTest {
    @Test
    void registrationAndConfirmationAreAvailableThroughMultipartAndPublicJsonEndpoints() throws Exception {
        var created = multipart("POST", "/auth/register-employee", Map.of("employeeRequest",
                registration("api@example.com", false)), false, adminCookie());
        assertThat(created.status()).isEqualTo(201);
        long id = created.id();
        assertThat(request("POST", "/auth/trigger-finalize-employee-registration",
                new TriggerEmailActionRequest("api@example.com"), adminCookie()).status()).isEqualTo(204);
        String uuid = confirmationTokens.findAll().getLast().getUuid();
        assertThat(request("POST", "/auth/finalize-employee-registration", action(id, uuid), null).status())
                .isEqualTo(200);
        assertThat(request("POST", "/auth/login", new LoginRequest("api@example.com", PASSWORD), null).status())
                .isEqualTo(200);
    }

    @Test
    void resetAndAuthenticatedPasswordChangeWorkThroughHttp() throws Exception {
        String email = employees.findById(2L).orElseThrow().getEmail();
        assertThat(request("POST", "/auth/start-password-reset", new TriggerEmailActionRequest(email), null)
                .status()).isEqualTo(200);
        String uuid = confirmationTokens.findAll().getFirst().getUuid();
        assertThat(request("POST", "/auth/finalize-password-reset", action(2L, uuid), null).status()).isEqualTo(200);
        var body = Map.of("currentPassword", PASSWORD, "newPassword", "NewStr0ngPass!2026",
                "confirmPassword", "NewStr0ngPass!2026");
        assertThat(request("POST", "/auth/change-password", body, employeeCookie()).status()).isEqualTo(200);
    }

    @Test
    void englishRestaurantRendersEnglishEmailWithoutUnresolvedTemplatePlaceholders() {
        restaurantService.updateRestaurant(null,
                Restaurant.DefaultRegion.EUROPE, true, null, null);
        auth.registerEmployee(registration("english@example.com", false), null);
        var content = ArgumentCaptor.forClass(
                EmailContent.class);
        verify(emailSender).send(eq("english@example.com"), content.capture());
        assertThat(content.getValue().html()).contains("Olena", "http://localhost:3000")
                .doesNotContain("{{", "}}");
        assertThat(content.getValue().subject()).isNotBlank();
    }
    @Test
    void registrationPersistsInactiveEmployeePictureTokenAndRenderedEmail() {
        var response = auth.registerEmployee(registration("new@example.com", false), png());
        var employee = employees.findById(response.id()).orElseThrow();
        assertThat(employee.isEmailConfirmed()).isFalse();
        assertThat(employee.getProfilePicture().getId()).isNotNull();
        assertThat(employee.getRole().getName().name()).isEqualTo("ROLE_EMPLOYEE");
        var token = confirmationTokens.findAll().getFirst();
        assertThat(token.getEmployeeId()).isEqualTo(employee.getId());
        assertThat(token.getTokenType()).isEqualTo(UuidToken.TokenType.REGISTRATION_CONFIRMATION);
        assertThat(token.getExpiresAt()).isAfter(token.getCreatedAt());
        var content = ArgumentCaptor.forClass(
                EmailContent.class);
        verify(emailSender).send(eq(employee.getEmail()), content.capture());
        assertThat(content.getValue().html()).contains(token.getUuid(), "Olena");
        assertThat(content.getValue().subject()).isNotBlank();
    }

    @Test
    void registrationCanCreateAdministratorWithoutPicture() {
        var response = auth.registerEmployee(registration("admin2@example.com", true), null);
        assertThat(employees.findById(response.id()).orElseThrow().getRole().getName().name())
                .isEqualTo("ROLE_ADMIN_EMPLOYEE");
    }

    @Test
    void duplicateRegistrationDoesNotSendAnotherEmail() {
        assertThatThrownBy(() -> auth.registerEmployee(registration(
                employees.findById(1L).orElseThrow().getEmail(), false), null))
                .isInstanceOf(EmployeeEmailAlreadyExistsException.class);
        verifyNoInteractions(emailSender);
    }

    @Test
    void mailFailureRollsBackRegistrationAndToken() {
        doThrow(new IllegalStateException("mail unavailable")).when(emailSender).send(any(), any());
        assertThatThrownBy(() -> auth.registerEmployee(registration("failed@example.com", false), null))
                .isInstanceOf(IllegalStateException.class);
        assertThat(employees.existsByEmail("failed@example.com")).isFalse();
        assertThat(confirmationTokens.count()).isZero();
    }

    @Test
    void registrationConfirmationHashesPasswordConsumesTokenAndEnablesLogin() {
        var employee = auth.registerEmployee(registration("new@example.com", false), null);
        String token = confirmationTokens.findAll().getFirst().getUuid();
        auth.finalizeRegistration(action(employee.id(), token));
        var saved = employees.findById(employee.id()).orElseThrow();
        assertThat(saved.isEmailConfirmed()).isTrue();
        assertThat(saved.getPassword()).isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, saved.getPassword())).isTrue();
        assertThat(saved.getPasswordUpdatedAt()).isNotNull();
        assertThat(confirmationTokens.count()).isZero();
        MockHttpServletResponse response = new MockHttpServletResponse();
        auth.login(new LoginRequest(saved.getEmail(), PASSWORD), response);
        assertThat(response.getCookie("accessToken")).isNotNull();
        assertThat(response.getCookie("refreshToken")).isNotNull();
        assertThatThrownBy(() -> auth.finalizeRegistration(action(employee.id(), token)))
                .isInstanceOf(EmailAlreadyConfirmedException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "expired", "wrong-owner", "wrong-type"})
    void invalidRegistrationConfirmationDoesNotActivateEmployee(String problem) {
        var employee = auth.registerEmployee(registration("new@example.com", false), null);
        UuidToken token = confirmationTokens.findAll().getFirst();
        if (problem.equals("expired")) {
            token.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        } else if (problem.equals("wrong-owner")) {
            token.setEmployeeId(1L);
        } else if (problem.equals("wrong-type")) {
            token.setTokenType(UuidToken.TokenType.PASSWORD_RESET_CONFIRMATION);
        }
        confirmationTokens.save(token);
        String supplied = problem.equals("unknown") ? UUID.randomUUID().toString() : token.getUuid();
        assertThatThrownBy(() -> auth.finalizeRegistration(action(employee.id(), supplied)))
                .isInstanceOf(problem.equals("expired") ? ExpiredTokenException.class
                        : InvalidTokenException.class);
        assertThat(employees.findById(employee.id()).orElseThrow().isEmailConfirmed()).isFalse();
        assertThat(confirmationTokens.count()).isEqualTo(1);
    }

    @Test
    void administratorCanResendActivationButNotForAlreadyActivatedEmployee() {
        var employee = auth.registerEmployee(registration("new@example.com", false), null);
        auth.triggerFinalizeRegistration(new TriggerEmailActionRequest("new@example.com"));
        assertThat(confirmationTokens.count()).isEqualTo(2);
        assertThatThrownBy(() -> auth.triggerFinalizeRegistration(new TriggerEmailActionRequest(
                employees.findById(1L).orElseThrow().getEmail())))
                .isInstanceOf(EmailAlreadyConfirmedException.class);
        assertThat(employee.id()).isPositive();
    }

    @Test
    void inactiveLoginSendsUsableActivationTokenWhileRejectingAuthentication() {
        var employee = auth.registerEmployee(registration("inactive@example.com", false), null);
        confirmationTokens.deleteAll();
        assertThatThrownBy(() -> auth.login(new LoginRequest("inactive@example.com", PASSWORD),
                new MockHttpServletResponse())).isInstanceOf(AccountNotActivatedException.class);
        assertThat(confirmationTokens.findAll()).anyMatch(token ->
                token.getEmployeeId().equals(employee.id()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", "wrong-password", "deleted"})
    void invalidLoginDoesNotIssueCookies(String problem) {
        if (problem.equals("deleted")) {
            employeeService.deleteEmployee(2L);
        }
        String email = problem.equals("unknown") ? "missing@example.com"
                : employees.findById(2L).orElseThrow().getEmail();
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertThatThrownBy(() -> auth.login(new LoginRequest(email,
                problem.equals("wrong-password") ? "bad" : PASSWORD), response))
                .isInstanceOf(problem.equals("deleted") ? DeletedEmployeeException.class
                        : AuthenticationServiceException.class);
        assertThat(response.getCookies()).isEmpty();
    }

    @Test
    void publicLoginProtectedMeRefreshAndLogoutWorkOverHttp() throws Exception {
        String email = employees.findById(2L).orElseThrow().getEmail();
        var login = request("POST", "/auth/login", new LoginRequest(email, PASSWORD), null);
        assertThat(login.status()).isEqualTo(200);
        String access = login.headers().allValues("set-cookie").stream()
                .filter(value -> value.startsWith("accessToken="))
                .findFirst().orElseThrow().split(";")[0];
        String refresh = login.headers().allValues("set-cookie").stream()
                .filter(value -> value.startsWith("refreshToken="))
                .findFirst().orElseThrow().split(";")[0];
        assertThat(request("GET", "/employees/me", null, access).data().path("id").asLong())
                .isEqualTo(2L);
        var renewed = request("GET", "/employees/me", null, refresh);
        assertThat(renewed.status()).isEqualTo(200);
        assertThat(renewed.headers().allValues("set-cookie"))
                .anyMatch(value -> value.startsWith("accessToken="));
        var logout = request("POST", "/auth/logout", null, access);
        assertThat(logout.status()).isEqualTo(200);
        assertThat(logout.headers().allValues("set-cookie"))
                .anyMatch(value -> value.startsWith("accessToken=") && value.contains("Max-Age=0"));
    }

    @Test
    void unknownPasswordResetDoesNotRevealWhetherEmployeeExists() throws Exception {
        assertThat(request("POST", "/auth/start-password-reset",
                new TriggerEmailActionRequest("unknown@example.com"), null).status()).isEqualTo(200);
        assertThat(confirmationTokens.count()).isZero();
        verifyNoInteractions(emailSender);
    }

    @Test
    void passwordResetChangesCredentialsAndConsumesAllEmployeeTokens() {
        String email = employees.findById(2L).orElseThrow().getEmail();
        auth.startPasswordReset(new TriggerEmailActionRequest(email));
        UuidToken token = confirmationTokens.findAll().getFirst();
        assertThat(token.getTokenType()).isEqualTo(UuidToken.TokenType.PASSWORD_RESET_CONFIRMATION);
        FinalizeEmployeeActionRequest request = action(2L, token.getUuid());
        request.setPassword("NewStr0ngPass!2026");
        request.setConfirmPassword(request.getPassword());
        auth.finalizePasswordReset(request);
        assertThat(passwordEncoder.matches(request.getPassword(),
                employees.findById(2L).orElseThrow().getPassword())).isTrue();
        assertThat(confirmationTokens.count()).isZero();
        assertThatThrownBy(() -> auth.finalizePasswordReset(request))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void deletedEmployeeCannotResetPassword() {
        auth.startPasswordReset(new TriggerEmailActionRequest(
                employees.findById(2L).orElseThrow().getEmail()));
        String token = confirmationTokens.findAll().getFirst().getUuid();
        employeeService.deleteEmployee(2L);
        assertThatThrownBy(() -> auth.finalizePasswordReset(action(2L, token)))
                .isInstanceOf(DeletedEmployeeException.class);
        assertThat(confirmationTokens.count()).isEqualTo(1);
    }

    @Test
    void authenticatedPasswordChangeRequiresCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("incorrect");
        request.setNewPassword("NewStr0ngPass!2026");
        request.setConfirmPassword(request.getNewPassword());
        assertThatThrownBy(() -> auth.changePassword(2L, request))
                .isInstanceOf(AuthenticationServiceException.class);
        request.setCurrentPassword(PASSWORD);
        auth.changePassword(2L, request);
        assertThat(passwordEncoder.matches(request.getNewPassword(),
                employees.findById(2L).orElseThrow().getPassword())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"email\":\"bad\",\"password\":\"x\"}", "{"})
    void invalidLoginRequestsReturnBadRequest(String body) throws Exception {
        assertThat(request("POST", "/auth/login", body, null).status()).isEqualTo(400);
    }

    @Test
    void passwordConfirmationAndStrengthAreValidatedByPublicApi() throws Exception {
        var employee = auth.registerEmployee(registration("new@example.com", false), null);
        String token = confirmationTokens.findAll().getFirst().getUuid();
        var body = Map.of("employeeId", employee.id(), "uuid", token, "password", "weak",
                "confirmPassword", "different");
        assertThat(request("POST", "/auth/finalize-employee-registration", body, null).status())
                .isEqualTo(400);
        assertThat(confirmationTokens.count()).isEqualTo(1);
    }

    private RegisterEmployeeRequest registration(String email, boolean admin) {
        return new RegisterEmployeeRequest("Olena", "Kovalenko", email, "0501112233", admin);
    }

    private FinalizeEmployeeActionRequest action(Long employeeId, String token) {
        FinalizeEmployeeActionRequest request = new FinalizeEmployeeActionRequest();
        request.setEmployeeId(employeeId);
        request.setUuid(token);
        request.setPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        return request;
    }
}
