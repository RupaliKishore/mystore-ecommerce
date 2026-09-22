package com.order.ecommerceshop.config;

import com.order.ecommerceshop.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig
{
    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder()
    {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception
    {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpStatus.UNAUTHORIZED.value());
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Unauthorized. Please login.\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpStatus.FORBIDDEN.value());
                            res.setContentType("application/json");
                            res.getWriter().write("{\"error\":\"Access denied. Admin only.\"}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // ============ PUBLIC ENDPOINTS ============
                        .requestMatchers(
                                "/graphql",              // GraphQL endpoint (queries public, mutations locked by @PreAuthorize)
                                "/graphiql",             // GraphiQL UI (dev only)
                                "/graphiql/**",
                                "/vendor/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health"
                        ).permitAll()

                        // User register + login (public)
                        .requestMatchers(HttpMethod.POST, "/api/rest/users/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/rest/auth/login").permitAll()

                        // Product browsing (public read)
                        .requestMatchers(HttpMethod.GET, "/api/rest/product/**").permitAll()

                        // ============ ADMIN-ONLY (write products) ============
                        .requestMatchers(HttpMethod.POST,   "/api/rest/product/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT,    "/api/rest/product/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rest/product/**").hasRole("ADMIN")

                        // ============ AUTHENTICATED ============
                        // Order APIs - any logged in user
                        .requestMatchers("/api/rest/order/**").authenticated()

                        // User profile / list
                        .requestMatchers("/api/rest/users/**").authenticated()

                        // ============ EVERYTHING ELSE REQUIRES LOGIN ============
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource()
    {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "http://localhost:8081",
                "http://localhost:5500",   // VSCode Live Server
                "http://127.0.0.1:8080",
                "http://127.0.0.1:8081",
                "http://127.0.0.1:5500",
                "http://localhost:8085",
                "http://127.0.0.1:8085"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}