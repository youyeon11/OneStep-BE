package com.a508.onestep.global.config;

import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.response.ErrorResponse;
import com.a508.onestep.global.auth.filter.JwtAuthenticationFilter;
import com.a508.onestep.global.auth.filter.UserContextFilter;
import com.a508.onestep.global.auth.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    /*
     * Security Filter 등록
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtUtils jwtUtils) throws Exception {
        http
                .cors(httpSecurityCorsConfigurer -> {
                    httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());
                })
                .csrf(csrf -> csrf.disable())
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(sessionManagerConfigurer -> {
                    sessionManagerConfigurer.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                // 비동기 요청에서도 SecurityContext 유지
                .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                .exceptionHandling(exception -> {
                    exception.authenticationEntryPoint(unauthorizedEntryPoint());
                    exception.accessDeniedHandler(accessDeniedHandler());
                })
                .authorizeHttpRequests(authorizeRequests -> authorizeRequests
                        // Async Dispatcher
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // 인증 불필요
                        .requestMatchers("/test/**", "/health/**", "/actuator/**", "/topic/**", "/ws/**", "/query/**", "/targets/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/guest", "/api/v1/auth/kakao").permitAll()

                        // 인증 필요
                        .anyRequest().authenticated());
        setJwtFilters(http);
        return http.build();
    }

    /**
     * 설정한 JWT Filter 적용
     */
    private void setJwtFilters(HttpSecurity httpSecurity) {
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtUtils);
        UserContextFilter userContextFilter = new UserContextFilter(jwtUtils);
        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        httpSecurity.addFilterAfter(userContextFilter, JwtAuthenticationFilter.class);
    }

    /**
     * CORS 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * filter 차원에서의 예외 처리를 위한 EntryPoint
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return ((request, response, authException) -> {
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(ErrorCode.UNAUTHORIZED.getCode())
                    .message(ErrorCode.UNAUTHORIZED.getMessage())
                    .build();
            response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        });
    }

    /**
     * 접근 권한에 대한 예외
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
            ErrorResponse errorResponse = ErrorResponse.builder()
                    .code(errorCode.getCode())
                    .message(errorCode.getMessage())
                    .build();

            response.setStatus(errorCode.getHttpStatus());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        };
    }
}