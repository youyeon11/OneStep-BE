package com.a508.onestep.global.s3.service;

import com.a508.onestep.domain.route.entity.RouteSession;
import com.a508.onestep.domain.route.event.ImageSaveEvent;
import com.a508.onestep.domain.route.repository.RouteSessionRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.s3.response.S3ImageResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;


@RequiredArgsConstructor
@Service
public class S3ServiceImpl implements S3Service{
    private final S3Client s3Client;
    private final RouteSessionRepository routeSessionRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    private static final String S3_PREFIX = "https://onestep-bucket-a508.s3.ap-northeast-2.amazonaws.com/";
    /**
     * 프론트에서 제공해주는 지도 이미지를
     * S3에 업로드하고, 주소를 반환하는 함수
     */
    public S3ImageResponseDto saveImage(
            MultipartFile mapImage
    ) {
        // 유저 확인
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // 업로드한 파일이 정상적인지 확인
        String originalFilename = mapImage != null ? mapImage.getOriginalFilename() : null;
        if (mapImage == null || mapImage.isEmpty() || originalFilename == null || originalFilename.isEmpty()) {
            throw BusinessException.of(ErrorCode.EMPTY_FILE_UPLOAD);
        }

        // 파일 확장자 검증
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == originalFilename.length() - 1) {
            throw BusinessException.of(ErrorCode.EMPTY_FILE_UPLOAD);
        }
        String fileExtension = originalFilename.substring(lastDotIndex);

        // RouteSession 먼저 확인 (S3 업로드 전에 검증)
        LocalDate logDate = LocalDate.now();
        RouteSession route = routeSessionRepository.findByUserIdAndLogDate(
                user.getId(),
                logDate
        ).orElseThrow(() -> BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND));

        // 중복 방지를 위한 난수 이름 생성
        String uploadFileName = "images/" + userCode + "/" + UUID.randomUUID() + fileExtension;

        LogUtils.info("url경로: " + uploadFileName);

        // 이미지 업로드
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(uploadFileName)
                            .contentType(mapImage.getContentType())
                            .build(),
                    RequestBody.fromInputStream(mapImage.getInputStream(), mapImage.getSize())
            );
        } catch (IOException e) {
            throw BusinessException.of(ErrorCode.FILE_UPLOAD_FAIL);
        }

        // DB 수정 이벤트 발행
        String saveUrl = S3_PREFIX + uploadFileName;

        ImageSaveEvent event = ImageSaveEvent.builder()
                .routeSessionId(route.getId())
                .imageUrl(saveUrl)
                .build();
        eventPublisher.publishEvent(event);

        return S3ImageResponseDto.builder()
                .id(route.getId())
                .url(saveUrl)
                .build();
    }
}
