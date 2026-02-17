package com.a508.onestep.global.s3.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema(description = "산책 경로 이미지 업로드 DTO")
public class S3ImageResponseDto {

    @Schema(description = "루트 ID")
    private Long id;

    @Schema(description = "산책 경로 이미지 주소")
    private String url;
}
