package com.example.restaurant.service.user;

import static com.example.restaurant.service.email.emailfactories.EmailStrategy.EmailFactories.CHANGE_EMAIL_CONFIRMATION_EMAIL;

import com.example.restaurant.dto.request.user.FinalizeEmailChangeRequest;
import com.example.restaurant.dto.request.user.UpdateEmployeeRequest;
import com.example.restaurant.dto.response.list.user.EmployeeListResponse;
import com.example.restaurant.dto.response.user.EmployeeResponse;
import com.example.restaurant.entity.image.Image;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.entity.token.UuidToken;
import com.example.restaurant.entity.user.Employee;
import com.example.restaurant.entity.user.Role;
import com.example.restaurant.exception.types.conflict.EmployeeEmailAlreadyExistsException;
import com.example.restaurant.exception.types.conflict.NoPendingEmailException;
import com.example.restaurant.exception.types.conflict.OrderIllegalStateException;
import com.example.restaurant.exception.types.conflict.PendingEmailAlreadyExistsException;
import com.example.restaurant.exception.types.forbidden.SelfModificationNotAllowedException;
import com.example.restaurant.mapper.user.EmployeeMapper;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.repository.user.EmployeeRepository;
import com.example.restaurant.service.email.ActionEmailService;
import com.example.restaurant.service.email.emailfactories.EmailStrategy;
import com.example.restaurant.service.image.ImageService;
import com.example.restaurant.service.utils.CookieRemover;
import com.example.restaurant.service.utils.EmployeeUtils;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService {
    private static final String CHANGE_EMAIL_PATH = "/employees/finalize-email-change";
    private final EmployeeRepository employeeRepository;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final EmployeeMapper employeeMapper;
    private final EmployeeUtils employeeUtils;
    private final ActionEmailService actionEmailService;
    private final EmailStrategy emailStrategy;
    private final CookieRemover cookieRemover;
    private final ImageService imageService;
    @Value("${security.change.email.expiration}")
    private String changeEmailExpiration;

    public EmployeeResponse getEmployeeById(Long id) {
        return employeeMapper.toResponse(employeeUtils.getEmployeeById(id));
    }

    public EmployeeListResponse getEmployees(
            Pageable pageable) {
        Page<Employee> employees = employeeRepository.findAllByIsDeletedFalse(pageable);
        List<EmployeeResponse> employeeResponses = employeeMapper.toResponse(employees.toList());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = employees.getTotalElements();

        return new EmployeeListResponse(
                employeeResponses,
                pageNumber,
                pageSize,
                totalElements
        );
    }

    public EmployeeResponse updateEmployeeById(
            Long authenticatedUserId, Long id,
            UpdateEmployeeRequest request, MultipartFile profilePicture) {
        restaurantRepository.lockSingleton().orElseThrow();
        Employee employee = employeeUtils.getEmployeeById(id);
        if (request != null) {
            validateRemoval(employee, Boolean.TRUE.equals(request.isDeleted()),
                    Boolean.FALSE.equals(request.isAdmin()));
            updatePresentFields(authenticatedUserId, id, request, employee);
        }
        employee.setUpdatedAt(LocalDateTime.now());

        if (profilePicture != null) {
            Image image = imageService.save(profilePicture);

            Long prevPfpId = null;
            if (employee.getProfilePicture() != null) {
                prevPfpId = employee.getProfilePicture().getId();
            }
            employee.setProfilePicture(image);
            employeeRepository.save(employee);
            employeeRepository.flush();

            if (prevPfpId != null) {
                imageService.delete(prevPfpId);
            }
        }

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    public void deleteEmployee(Long id) {
        restaurantRepository.lockSingleton().orElseThrow();
        Employee employee = employeeUtils.getEmployeeById(id);
        if (employee.isDeleted()) {
            return;
        }
        validateRemoval(employee, true, false);
        employee.setDeleted(true);
        employee.setUpdatedAt(LocalDateTime.now());
        employeeRepository.save(employee);
    }

    private void validateRemoval(Employee employee, boolean deleting, boolean demoting) {
        if (employee.isDeleted() || (!deleting && !demoting)) {
            return;
        }
        if (employee.isEmailConfirmed()
                && employee.getRole().getName() == Role.RoleType.ROLE_ADMIN_EMPLOYEE
                && employeeRepository.countByIsDeletedFalseAndEmailConfirmedTrueAndRole_Name(
                        Role.RoleType.ROLE_ADMIN_EMPLOYEE) <= 1) {
            throw new OrderIllegalStateException("Cannot remove the last active administrator.");
        }
        if (deleting && orderRepository.existsByEmployeeIdAndStatus(
                employee.getId(), Order.OrderStatus.OPENED)) {
            throw new OrderIllegalStateException(
                    "Reassign or close the employee's open orders before deletion.");
        }
    }

    public EmployeeResponse finalizeEmailChange(
            FinalizeEmailChangeRequest request,
            HttpServletResponse response) {
        employeeUtils.validateAndDeleteUuidToken(request.getUuid(), request.getEmployeeId(),
                UuidToken.TokenType.CHANGE_EMAIL_CONFIRMATION);

        Employee employee = employeeUtils.getEmployeeById(request.getEmployeeId());
        if (employee.getPendingEmail() == null) {
            throw new NoPendingEmailException("No pending email found.");
        }
        if (employeeRepository.existsByEmail(employee.getPendingEmail())) {
            throw new EmployeeEmailAlreadyExistsException(
                    "Employee with email " + employee.getPendingEmail() + " already exists.");
        }
        employee.setEmail(employee.getPendingEmail());
        employee.setPendingEmail(null);
        cookieRemover.removeAllTokenCookies(response);

        return employeeMapper.toResponse(employeeRepository.save(employee));
    }

    private void updatePresentFields(Long authenticatedUserId,
                                     Long id,
                                     UpdateEmployeeRequest request,
                                     Employee employee) {
        if (request.firstName() != null) {
            employee.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            employee.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            employee.setPhone(request.phone());
        }

        boolean isMe = authenticatedUserId.equals(id);
        if (isMe && request.isAdmin() != null) {
            throw new SelfModificationNotAllowedException("Can't update your own admin status.");
        } else if (!isMe && request.isAdmin() != null) {
            employee.setRole(employeeUtils.getRoleByBoolean(request.isAdmin()));
        }
        if (isMe && request.isDeleted() != null) {
            throw new SelfModificationNotAllowedException("Can't delete or restore yourself.");
        } else if (!isMe && request.isDeleted() != null) {
            employee.setDeleted(request.isDeleted());
        }

        if (request.email() == null || request.email().equals(employee.getEmail())) {
            return;
        }

        if (employeeRepository.existsByEmail(request.email())) {
            Employee employeeByUpdatedEmail = employeeUtils.getEmployeeByEmail(request.email());
            if (!employeeByUpdatedEmail.getId().equals(employee.getId())) {
                throw new EmployeeEmailAlreadyExistsException(
                        "Employee with email " + request.email() + " already exists.");
            }
        } else {
            if (employeeRepository.existsByPendingEmail(request.email())) {
                throw new PendingEmailAlreadyExistsException(
                        "Employee with email " + request.email() + " already pending.");
            }
            employee.setPendingEmail(request.email());
            actionEmailService.sendConfirmationEmail(
                    employee.getPendingEmail(),
                    employee.getFirstName(),
                    employee.getLastName(),
                    employee.getId(),
                    changeEmailExpiration,
                    CHANGE_EMAIL_PATH,
                    emailStrategy.getEmailFactory(CHANGE_EMAIL_CONFIRMATION_EMAIL),
                    UuidToken.TokenType.CHANGE_EMAIL_CONFIRMATION
            );
        }
    }
}
