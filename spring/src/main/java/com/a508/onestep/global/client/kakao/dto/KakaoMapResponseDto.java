package com.a508.onestep.global.client.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoMapResponseDto {

    private Meta meta;

    private List<Document> documents;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {

        @JsonProperty("total_count")
        private Integer totalCount;

        @JsonProperty("pageable_count")
        private Integer pageableCount;

        @JsonProperty("is_end")
        private Boolean isEnd;

        @JsonProperty("same_name")
        private SameName sameName;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SameName {

            private List<String> region;

            private String keyword;

            @JsonProperty("selected_region")
            private String selectedRegion;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {

        private String id;

        @JsonProperty("place_name")
        private String placeName;

        @JsonProperty("category_name")
        private String categoryName;

        @JsonProperty("category_group_code")
        private String categoryGroupCode;

        @JsonProperty("category_group_name")
        private String categoryGroupName;

        private String phone;

        @JsonProperty("address_name")
        private String addressName;

        @JsonProperty("road_address_name")
        private String roadAddressName;

        private String x;

        private String y;

        @JsonProperty("place_url")
        private String placeUrl;

        // 카카오 API에서 x,y 파라미터 제공 시 반환됨 (단위: meter)
        private String distance;
    }
}