package com.example.restaurant.service.security;

import static com.example.restaurant.service.email.emailfactories.EmailStrategy.EmailFactories.REGISTRATION_CONFIRMATION_EMAIL;
import static com.example.restaurant.service.email.emailfactories.EmailStrategy.EmailFactories.RESET_PASSWORD_EMAIL;

import com.example.restaurant.dto.request.security.ChangePasswordRequest;
import com.example.restaurant.dto.request.security.FinalizeEmployeeActionRequest;
import com.example.restaurant.dto.request.security.LoginRequest;
import com.example.restaurant.dto.request.security.RegisterEmployeeRequest;
import com.example.restaurant.dto.request.security.TriggerEmailActionRequest;
import com.example.restaurant.dto.response.user.EmployeeResponse;
import com.example.restaurant.entity.image.Image;
import com.example.restaurant.entity.token.UuidToken;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.exception.types.conflict.EmployeeEmailAlreadyExistsException;
import com.example.restaurant.exception.types.forbidden.AccountNotActivatedException;
import com.example.restaurant.mapper.user.EmployeeMapper;
import com.example.restaurant.repository.user.EmployeeRepository;
import com.example.restaurant.service.email.ActionEmailService;
import com.example.restaurant.service.email.emailfactories.EmailStrategy;
import com.example.restaurant.service.image.ImageService;
import com.example.restaurant.service.utils.CookieRemover;
import com.example.restaurant.service.utils.CookieSetter;
import com.example.restaurant.service.utils.EmployeeUtils;
import com.example.restaurant.service.utils.TokenGenerator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private static final String REGISTRATION_CONFIRMATION_PATH =
            "/auth/finalize-employee-registration";
    private static final String PASSWORD_RESET_PATH = "/auth/finalize-password-reset";
    @Value("${security.registration.expiration}")
    private String registrationExpiration;
    @Value("${security.password.reset.expiration}")
    private String passwordResetExpiration;
    @Value("${security.jwt.access.expiration}")
    private String accessExpiration;
    @Value("${security.jwt.refresh.expiration}")
    private String refreshExpiration;
    private final EmailStrategy emailStrategy;
    private final EmployeeRepository employeeRepository;
    private final EmployeeMapper employeeMapper;
    private final AuthenticationManager authenticationManager;
    private final TokenGenerator tokenGenerator;
    private final CookieSetter cookieSetter;
    private final CookieRemover cookieRemover;
    private final PasswordEncoder passwordEncoder;
    private final ActionEmailService actionEmailService;
    private final EmployeeUtils employeeUtils;
    private final ImageService imageService;

    public EmployeeResponse registerEmployee(
            RegisterEmployeeRequest request, MultipartFile profilePicture) {
        if (employeeRepository.existsByEmail(request.email())) {
            throw new EmployeeEmailAlreadyExistsException("Email already used");
        }

        Employee employee = employeeMapper.toEmployee(request);
        employee.setRole(employeeUtils.getRoleByBoolean(request.isAdmin()));

        if (profilePicture != null) {
            Image image = imageService.save(profilePicture);
            employee.setProfilePicture(image);
        }

        EmployeeResponse employeeResponse = employeeMapper.toResponse(
                employeeRepository.save(employee));

        actionEmailService.sendConfirmationEmail(
                request.email(),
                request.firstName(),
                request.lastName(),
                employeeResponse.id(),
                registrationExpiration,
                REGISTRATION_CONFIRMATION_PATH,
                emailStrategy.getEmailFactory(REGISTRATION_CONFIRMATION_EMAIL),
                UuidToken.TokenType.REGISTRATION_CONFIRMATION);

        return employeeResponse;
    }

    public void triggerFinalizeRegistration(TriggerEmailActionRequest request) {
        Employee employee = employeeUtils.getEmployeeByEmail(request.email());

        employeeUtils.throwIfEmailConfirmed(employee);

        actionEmailService.sendConfirmationEmail(
                employee.getEmail(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getId(),
                registrationExpiration,
                REGISTRATION_CONFIRMATION_PATH,
                emailStrategy.getEmailFactory(REGISTRATION_CONFIRMATION_EMAIL),
                UuidToken.TokenType.REGISTRATION_CONFIRMATION);
    }

    public EmployeeResponse finalizeRegistration(FinalizeEmployeeActionRequest request) {
        Employee employee = employeeUtils.getEmployeeById(request.getEmployeeId());

        employeeUtils.throwIfEmailConfirmed(employee);

        employeeUtils.validateAndDeleteUuidToken(request.getUuid(), request.getEmployeeId(),
                UuidToken.TokenType.REGISTRATION_CONFIRMATION);

        createEncodedPassword(employee, request.getPassword());
        employee.setEmailConfirmed(true);

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    public void login(LoginRequest request, HttpServletResponse response) {
        Employee employee;

        try {
            employee = employeeUtils.getEmployeeByEmail(request.email());
        } catch (EntityNotFoundException e) {
            throw new AuthenticationServiceException("Either login or password is invalid");
        }

        checkIfHasAccess(employee, request.email());

        TokenResponse tokenResponse = getTokensAfterAuth(request);
        cookieSetter.addCookie("accessToken", tokenResponse.accessToken(),
                accessExpiration, response);
        cookieSetter.addCookie("refreshToken",
                tokenResponse.refreshToken(), refreshExpiration, response);
    }

    public void logout(HttpServletResponse response) {
        cookieRemover.removeAllTokenCookies(response);
    }

    public void startPasswordReset(TriggerEmailActionRequest request) {
        Employee employee;

        try {
            employee = employeeUtils.getEmployeeByEmail(request.email());
        } catch (EntityNotFoundException e) {
            //Do nothing to prevent guessing of email
            return;
        }

        checkIfHasAccess(employee, request.email());

        actionEmailService.sendConfirmationEmail(
                employee.getEmail(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getId(),
                passwordResetExpiration,
                PASSWORD_RESET_PATH,
                emailStrategy.getEmailFactory(RESET_PASSWORD_EMAIL),
                UuidToken.TokenType.PASSWORD_RESET_CONFIRMATION);
    }

    public void finalizePasswordReset(FinalizeEmployeeActionRequest request) {
        employeeUtils.validateAndDeleteUuidToken(request.getUuid(),
                request.getEmployeeId(), UuidToken.TokenType.PASSWORD_RESET_CONFIRMATION);

        Employee employee = employeeUtils.getEmployeeById(request.getEmployeeId());
        employeeUtils.throwIfEmployeeDeleted(employee);
        updateEncodedPassword(employee, request.getPassword());

        employeeRepository.save(employee);
    }

    public void changePassword(Long employeeId, ChangePasswordRequest request) {
        Employee employee = employeeUtils.getEmployeeById(employeeId);
        if (passwordEncoder.matches(request.getCurrentPassword(), employee.getPassword())) {
            employee.setPassword(passwordEncoder.encode(request.getNewPassword()));
            LocalDateTime now = LocalDateTime.now();
            employee.setUpdatedAt(now);
            employee.setPasswordUpdatedAt(now);
            employeeRepository.save(employee);
        } else {
            throw new AuthenticationServiceException("Current password does not match.");
        }
    }

    private void checkIfHasAccess(Employee employee, String email) {
        employeeUtils.throwIfEmployeeDeleted(employee);

        if (!employee.isEmailConfirmed()) {
            actionEmailService.sendConfirmationEmail(
                    employee.getEmail(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getId(),
                    registrationExpiration,
                    REGISTRATION_CONFIRMATION_PATH,
                    emailStrategy.getEmailFactory(REGISTRATION_CONFIRMATION_EMAIL),
                    UuidToken.TokenType.REGISTRATION_CONFIRMATION);

            throw new AccountNotActivatedException(
                    "Employee with email " + email
                            + " has been not been activated yet. Please find "
                            + "confirmation email in your email box.");
        }
    }

    private TokenResponse getTokensAfterAuth(LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.email(),
                                    loginRequest.password()));
        } catch (AuthenticationException authenticationException) {
            throw new AuthenticationServiceException("Either login or password is invalid");
        }

        return new TokenResponse(
                tokenGenerator.generateAccessToken(loginRequest.email()),
                tokenGenerator.generateRefreshToken(loginRequest.email())
        );
    }

    private void createEncodedPassword(Employee employee, String password) {
        employee.setPassword(passwordEncoder.encode(password));
        LocalDateTime now = LocalDateTime.now();
        employee.setUpdatedAt(now);
        employee.setPasswordCreatedAt(now);
        employee.setPasswordUpdatedAt(now);
    }

    private void updateEncodedPassword(Employee employee, String password) {
        employee.setPassword(passwordEncoder.encode(password));
        LocalDateTime now = LocalDateTime.now();
        employee.setUpdatedAt(now);
        employee.setPasswordUpdatedAt(now);
    }

    private record TokenResponse(String accessToken, String refreshToken) {}
}
