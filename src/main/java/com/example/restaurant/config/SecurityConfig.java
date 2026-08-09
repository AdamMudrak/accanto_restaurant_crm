package com.example.restaurant.config;

import com.example.restaurant.service.security.JwtAuthenticationFilter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;

@EnableMethodSecurity
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {
    private static final int BCRYPT_STRENGTH = 10;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    @Value("${server.path}")
    private String serverPath;
    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder getPasswordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    @Bean
    @Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) {
        return http
                .securityMatcher(publicEndpointsMatcher())
                .cors(cors -> cors.configurationSource(request -> getCorsConfiguration()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http) {
        return http
                .cors(cors -> cors.configurationSource(request -> getCorsConfiguration()))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest()
                        .authenticated())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .userDetailsService(userDetailsService)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    private List<String> getAllAllowedOrigins() {
        List<String> allowedOriginsList = new ArrayList<>(List.of(allowedOrigins.split("&")));
        allowedOriginsList.add(serverPath);

        return allowedOriginsList;
    }

    private CorsConfiguration getCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(getAllAllowedOrigins());
        config.setAllowCredentials(true);
        config.setAllowedMethods(List.of("*"));
        config.setAllowedHeaders(List.of("*"));
        return config;
    }

    private RequestMatcher publicEndpointsMatcher() {
        return new OrRequestMatcher(
                PathPatternRequestMatcher.pathPattern(HttpMethod.OPTIONS, "/**"),
                PathPatternRequestMatcher.pathPattern("/auth/finalize-employee-registration"),
                PathPatternRequestMatcher.pathPattern("/auth/login"),
                PathPatternRequestMatcher.pathPattern("/auth/start-password-reset"),
                PathPatternRequestMatcher.pathPattern("/auth/finalize-password-reset"),
                PathPatternRequestMatcher.pathPattern("/employees/finalize-email-change"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/restaurant/this"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/schedule"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/schedule/{id}"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/menu-categories/{id}"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/menu-categories/parents"),
                PathPatternRequestMatcher.pathPattern(
                        HttpMethod.GET, "/menu-categories/children/{parentCategoryId}"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/menu-items"),
                PathPatternRequestMatcher.pathPattern(
                        HttpMethod.GET, "/menu-items/category/{childCategoryId}"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/menu-items/{id}"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/images/{id}"),
                PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
                PathPatternRequestMatcher.pathPattern("/swagger-ui.html"),
                PathPatternRequestMatcher.pathPattern("/v3/api-docs"),
                PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
                PathPatternRequestMatcher.pathPattern("/error"),
                PathPatternRequestMatcher.pathPattern("/test/no-auth"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/dining-tables/guest"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/orders/guest/items"),
                PathPatternRequestMatcher.pathPattern(
                        HttpMethod.PUT, "/orders/guest/items/{id}"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/orders/guest/current"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/payments"),
                PathPatternRequestMatcher.pathPattern(
                        HttpMethod.POST, "/payments/monobank/webhook"),
                PathPatternRequestMatcher.pathPattern(
                        HttpMethod.POST, "/payments/stripe/webhook")

        );
    }
}
