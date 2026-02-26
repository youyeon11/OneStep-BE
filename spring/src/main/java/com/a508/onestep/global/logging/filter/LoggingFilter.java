package com.a508.onestep.global.logging.filter;

import com.a508.onestep.global.logging.utils.LogUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

@Component
public class LoggingFilter extends OncePerRequestFilter {


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        super.doFilter(request, response, filterChain);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        Long start = System.currentTimeMillis();

        // BusinessContext
        MDC.put("requestMethod", request.getMethod());
        MDC.put("requestPath", request.getRequestURI());
        MDC.put("clientIp", getClientIp(request));
        MDC.put("userAgent", Optional.ofNullable(request.getHeader("User-Agent")).orElse("-"));
        MDC.put("userId", extractUserId(request));

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        // log
        try {
            logRequest(requestWrapper);
            filterChain.doFilter(requestWrapper, responseWrapper);
        } catch (Exception e) {
            MDC.put("errorType", e.getClass().getSimpleName());
            MDC.put("errorMessage", e.getMessage());
            LogUtils.error(e, requestWrapper);
            throw e;
        } finally {
            Long tookMs = System.currentTimeMillis() - start;

            // Json
            MDC.put("responseStatus", String.valueOf(responseWrapper.getStatus()));
            MDC.put("duration_ms", String.valueOf(tookMs));

            logResponse(responseWrapper, tookMs);
            responseWrapper.copyBodyToResponse();

            // MDC 비우기
            MDC.remove("requestMethod");
            MDC.remove("requestPath");
            MDC.remove("clientIp");
            MDC.remove("userAgent");
            MDC.remove("userId");
            MDC.remove("responseStatus");
            MDC.remove("duration_ms");
            MDC.remove("errorType");
            MDC.remove("errorMessage");
        }

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return request.getServletPath().startsWith("/swagger-ui")
                || request.getServletPath().startsWith("/test")
                || request.getServletPath().startsWith("/swagger-ui.html")
                || request.getServletPath().startsWith("/v3/api-docs")
                || request.getServletPath().startsWith("/actuator/prometheus");
    }

        /*
     Request 기록
     */
    private void logRequest(HttpServletRequest request) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String query = request.getQueryString();

        LogUtils.info(
                "[REQ] {} {}{}", method, uri, (query != null ? query : "")
        );
    }

    /*
    Response 콘솔 로그
     */
    private void logResponse(ContentCachingResponseWrapper responseWrapper, long tookMs) {

        LogUtils.info("[RES] status={} took={}ms", responseWrapper.getStatus(), tookMs);

        byte[] body = responseWrapper.getContentAsByteArray();
        if (logger.isDebugEnabled()) {
            if (body.length > 0) {
                String bodyString = new String(body, StandardCharsets.UTF_8);
                if (bodyString.length() > 2000) {
                    bodyString = bodyString.substring(0, 2000) + "...(omission)";
                }
                LogUtils.debug("[RES-BODY] {}", bodyString);
            }
        }
    }


    /*
    JWT에서 userId 추출
     */
    private String extractUserId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String[] parts = authHeader.substring(7).split("\\.");
                String userCode = "";

                if (parts.length == 3) {
                    String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                    // payload
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(payload);
                        userCode = node.has("sub") ? node.get("sub").asText() : "anonymous";
                    } catch (Exception e) {
                        return "anonymous";
                    }
                    return userCode;
                }
            } catch (Exception e) {
                return "anonymous";
            }
        }
        return "anonymous";
    }

    /*
    Client의 IP를 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
