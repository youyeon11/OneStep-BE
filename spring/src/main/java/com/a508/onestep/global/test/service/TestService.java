package com.a508.onestep.global.test.service;

import com.a508.onestep.global.test.dto.request.TestRequestDto;
import com.a508.onestep.global.test.dto.response.TestResponseDto;
import org.springframework.stereotype.Service;

@Service
public class TestService {

    // 메서드
    public TestResponseDto testCode(TestRequestDto requestDto) {
        TestResponseDto dto = TestResponseDto.builder()
                .name(requestDto.getName())
                .content(requestDto.getContent())
                .build();
        return dto;
    }
}
