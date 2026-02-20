package com.a508.onestep.global.client.kakao;

import com.a508.onestep.domain.common.PlaceCategory;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.client.kakao.dto.KakaoMapResponseDto;
import com.a508.onestep.global.client.kakao.dto.KakaoUserInfoResponseDto;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class KakaoApiClient {

    private static final String KAKAO_AUTH_API_URL = "https://kapi.kakao.com";

    private final WebClient.Builder webClientBuilder;
    private final WebClient kakaoMapWebClient;

    /**
     * 액세스 토큰으로 사용자 정보 요청
     */
    public KakaoUserInfoResponseDto requestUserInfo(String accessToken) {
        LogUtils.info("카카오 사용자 정보 요청");

        try {
            KakaoUserInfoResponseDto userInfo = webClientBuilder
                    .baseUrl(KAKAO_AUTH_API_URL)
                    .build()
                    .get()
                    .uri("/v2/user/me")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                            .map(body -> new RuntimeException("카카오 사용자 정보 API 오류: " + body)))
                    .bodyToMono(KakaoUserInfoResponseDto.class)
                    .block();

            LogUtils.info("카카오 사용자 정보 조회 완료 : {}", userInfo);
            return userInfo;

        } catch (Exception e) {
            LogUtils.warn("카카오 사용자 정보 요청 실패: {}", e.getMessage());
            throw BusinessException.of(ErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    /**
     * 카카오맵 카테고리 기반 장소 검색
     */
    public KakaoMapResponseDto searchPlacesByCategory(
            Double latitude,
            Double longitude,
            Integer radius,
            Integer size,
            PlaceCategory placeCategory) {

        LogUtils.info("카카오맵 장소 검색 - 카테고리: {}, 위도: {}, 경도: {}, 반경: {}m",
                placeCategory.getDescription(), latitude, longitude, radius);

        try {
            KakaoMapResponseDto response = kakaoMapWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/local/search/category.json")
                            .queryParam("category_group_code", placeCategory.getCode())
                            .queryParam("x", longitude)
                            .queryParam("y", latitude)
                            .queryParam("radius", radius)
                            .queryParam("sort", "distance")
                            .queryParam("size", size)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .doOnNext(body -> LogUtils.error("카카오맵 API 에러 ({}): {}", clientResponse.statusCode(), body))
                            .map(body -> BusinessException.of(ErrorCode.KAKAO_API_ERROR)))
                    .bodyToMono(KakaoMapResponseDto.class)
                    .block();

            LogUtils.debug("카카오맵 API 응답 성공 - 총 {}건",
                    response != null ? response.getMeta().getTotalCount() : 0);
            return response;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            LogUtils.error("카카오맵 API 호출 실패", e);
            throw BusinessException.of(ErrorCode.KAKAO_API_ERROR);
        }
    }
}
