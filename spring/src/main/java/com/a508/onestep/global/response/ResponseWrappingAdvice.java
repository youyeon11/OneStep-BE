package com.a508.onestep.global.response;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

@Hidden
@RestControllerAdvice
public class ResponseWrappingAdvice implements ResponseBodyAdvice<Object> {

    private static final List<String> EXCLUDED_PATHS = List.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/api-docs"
    );

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return !ResponseEntity.class.isAssignableFrom(returnType.getParameterType())
                && !String.class.isAssignableFrom(returnType.getParameterType())
                && !ApiResponse.class.isAssignableFrom(returnType.getParameterType());
    }

    @Nullable
    @Override
    public Object beforeBodyWrite(@Nullable Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {

        // Swagger 관련 경로는 wrapping 제외
        String path = request.getURI().getPath();
        if (EXCLUDED_PATHS.stream().anyMatch(path::startsWith)) {
            return body;
        }

        // ApiResponse 그대로 반환
        if (body instanceof ApiResponse<?>) {
            return body;
        }

        // body가 null인 경우 204 No Content
        if (body == null) {
            return ApiResponse.success();
        }

        // body가 있는 경우 200 OK 응답
        return ApiResponse.success(body);
    }
}