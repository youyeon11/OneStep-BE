package com.a508.onestep.global.s3.service;


import com.a508.onestep.global.s3.response.S3ImageResponseDto;
import org.springframework.web.multipart.MultipartFile;

public interface S3Service {
    // 사용자 코드
    S3ImageResponseDto saveImage(MultipartFile mapImage);

}
