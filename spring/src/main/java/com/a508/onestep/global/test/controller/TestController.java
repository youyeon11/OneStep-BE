package com.a508.onestep.global.test.controller;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ApiResponse;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.response.ErrorResponse;
import com.a508.onestep.global.test.TestData;
import com.a508.onestep.global.test.dto.request.TestRequestDto;
import com.a508.onestep.global.test.dto.response.TestResponseDto;
import com.a508.onestep.global.test.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Tag(name = "Test Controller")
public class TestController {

    private final TestService testService;

    @Operation(summary = "데이터 포함 응답 API", description = "데이터 포함 응답 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "응답 성공",
            content = @Content(schema = @Schema(implementation = TestData.class))
    )
    @GetMapping("/success-with-data")
    public TestData successWithData() {
        return new TestData(1L, "테스트");
    }

    @Operation(summary = "에러 응답 API", description = "ErrorResponse를 반환하는 실패 응답 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "내부 서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping("/fail")
    public ResponseEntity<ErrorResponse> fail() {
        return ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Operation(summary = "커스텀 예외 발생 API", description = "BusinessException을 발생시키는 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "테스트 에러 발생",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping("/custom-fail")
    public void customFail() {
        throw BusinessException.of(ErrorCode.TEST_ERROR);
    }

    @Operation(summary = "리소스 없음 예외 API", description = "RESOURCE_NOT_FOUND 예외를 발생시키는 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "리소스를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @GetMapping("/extra-exception")
    public void extraException() {
        throw BusinessException.of(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Operation(summary = "Null 데이터 응답 API", description = "데이터가 null인 응답 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "응답 성공 (데이터 없음)",
            content = @Content(schema = @Schema(implementation = TestData.class))
    )
    @GetMapping("/success-null-data")
    public TestData successWithNullData() {
        return null;
    }

    @Operation(summary = "복잡한 데이터 응답 API", description = "중첩된 객체와 배열을 포함한 복잡한 데이터 구조 응답 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "응답 성공",
            content = @Content(
                    schema = @Schema(implementation = ApiResponse.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                        "success": true,
                                        "data": {
                                            "user": {
                                                "name": "홍길동",
                                                "age": 30
                                            },
                                            "items": ["item1", "item2"]
                                        },
                                        "error": null
                                    }
                                    """
                    )
            )
    )
    @GetMapping("/success-complex-data")
    public ApiResponse<Map<String, Object>> successWithComplexData() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> user = new HashMap<>();
        user.put("name", "홍길동");
        user.put("age", 30);
        data.put("user", user);
        data.put("items", new String[]{"item1", "item2"});
        return ApiResponse.success(data);
    }

    @Operation(summary = "DTO 테스트 API", description = "RequestDto를 받아 ResponseDto를 반환하는 예시 API 입니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "응답 성공",
            content = @Content(schema = @Schema(implementation = TestResponseDto.class))
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "테스트 요청 데이터",
            required = true,
            content = @Content(
                    schema = @Schema(implementation = TestRequestDto.class),
                    examples = @ExampleObject(
                            name = "테스트 요청 예시",
                            value = """
                                    {
                                        "name": "테스트",
                                        "value": "sample"
                                    }
                                    """
                    )))
    @PostMapping("/dto-test")
    public TestResponseDto test(
            @RequestBody TestRequestDto requestDto
    ) {
        return testService.testCode(requestDto);
    }

    @Operation(summary = "파이프라인 테스트 API", description = "배포 자동화 테스트를 위해 true를 반환하는 API입니다.")
    @GetMapping("/pipeline-test")
    public boolean pipelineTest() {
        return true;
    }
}
