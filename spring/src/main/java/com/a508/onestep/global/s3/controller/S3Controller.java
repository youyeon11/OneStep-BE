package com.a508.onestep.global.s3.controller;

import com.a508.onestep.global.s3.response.S3ImageResponseDto;
import com.a508.onestep.global.s3.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/s3/images")
@Tag(name = "S3 Image Controller", description = "S3 이미지 관련 API")
public class S3Controller {
    private final S3Service s3Service;

    @PostMapping("/upload")
    @Operation(summary = "산책 이미지 업로드", description = "현재 로그인한 사용자의 산책 경로 이미지를 업로드합니다.")
    public S3ImageResponseDto saveImage (
            @RequestHeader("Authorization") String token,
            @RequestPart("file") MultipartFile file
    ){
        return s3Service.saveImage(file);
    }

}
