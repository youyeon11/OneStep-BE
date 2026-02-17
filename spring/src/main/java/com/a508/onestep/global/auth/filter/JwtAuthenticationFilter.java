package com.a508.onestep.global.auth.filter;

import com.a508.onestep.global.auth.context.JwtAuthenticationToken;
import com.a508.onestep.global.auth.utils.JwtUtils;
import com.a508.onestep.global.logging.utils.LogUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws java.io.IOException, jakarta.servlet.ServletException {

        try {
            String token = jwtUtils.resolveToken(request);
            if (token != null && jwtUtils.validateToken(token)) {
                // 토큰 유효 -> Spring Security Context 인증 정보 설정
                String userCode = jwtUtils.getUserCode(token);
                String role = jwtUtils.getRole(token);

                // Spring Security Authentication 객체 생성 및 등록
                JwtAuthenticationToken authentication = new JwtAuthenticationToken(userCode, role);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            // Context 비움
            LogUtils.warn("JWT 인증 필터 실패 : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/test")
                || path.startsWith("/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-resources")
                || path.equals("/swagger-ui.html");
    }
}