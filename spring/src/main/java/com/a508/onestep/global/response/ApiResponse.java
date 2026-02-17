package com.a508.onestep.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private final boolean isSuccess;
    private final int code;
    private final String message;
    private final T result;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(
                true,
                200,
                "요청에 성공하였습니다.",
                data
        );
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(
                true,
                204,
                "요청에 성공하였습니다.",
                null
        );
    }

    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(
                true,
                code,
                message,
                data
        );
    }

    public static ApiResponse<Void> fail(BaseCode code) {
        return new ApiResponse<>(false, code.getHttpStatus(), code.getMessage(), null);
    }

    public static ApiResponse<Void> fail(int code, String message) {
        return new ApiResponse<>(
                false,
                code,
                message,
                null
        );
    }
}
