package com.example.restaurant.service.security;

import com.example.restaurant.repository.user.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final EmployeeRepository employeeRepository;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String email) {
        return employeeRepository.findByEmail(email).orElseThrow(()
                -> new UsernameNotFoundException(
                "Can't find user by email " + email));
    }
}
