package com.a508.onestep.global.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.ResponseEntity;

@Builder
@AllArgsConstructor
@Getter
public class ErrorResponse {

    private final String code;
    private final String message;

    public static ResponseEntity<ErrorResponse> of(BaseCode code) {
        return ResponseEntity.status(code.getHttpStatus())
                .body(ErrorResponse.builder()
                        .code(code.getCode())
                        .message(code.getMessage())
                        .build()
                );
    }
}
