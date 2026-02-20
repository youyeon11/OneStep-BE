package com.a508.onestep.domain.challenge.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "daily_routine_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class RecommendedRoutine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Field(targetType = FieldType.OBJECT_ID)
    private String id;

    private String userCode;

    private List<Recommendation> recommendations;

    private Metadata metadata;

    private CreatedAt createdAt;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class Recommendation {
        private Long challengeCode;
        private Double weight;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class Metadata {
        private LocalDateTime generatedAt;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class CreatedAt {
        private LocalDateTime date;
    }
}