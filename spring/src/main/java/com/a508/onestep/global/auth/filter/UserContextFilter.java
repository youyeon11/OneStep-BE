package com.a508.onestep.global.auth.filter;

import com.a508.onestep.global.auth.context.UserContext;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.auth.utils.JwtUtils;
import com.a508.onestep.global.logging.utils.LogUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws IOException, ServletException {

        try {
            UserContext context = jwtUtils.extractUserContext(request);
            if (context != null) {
                UserContextHolder.set(context);
                LogUtils.debug("UserContext 설정 완료 - UserCode: {}", context.getUserCode());
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
            LogUtils.debug("UserContext 정리 완료");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith("/test")
                || request.getServletPath().startsWith("/health")
                || request.getServletPath().startsWith("/swagger-ui")
                || request.getServletPath().startsWith("/swagger-ui.html")
                || request.getServletPath().startsWith("/v3/api-docs");
    }
}