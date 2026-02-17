package com.a508.onestep.domain.pet.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Entity;

@Entity
@Table(name = "pet_ownerships")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PetOwnership extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_main", nullable = false)
    private Boolean isMain = true;

    @Column(name = "pet_level", nullable = false)
    private Integer petLevel;

    @Column(name = "max_exp", nullable = false)
    private Integer maxExp;

    @Column(name = "current_exp")
    private Integer currentExp;

    @Builder.Default // 빌더 사용 시에도 이 기본값이 적용되도록 설정
    @Column(name = "pet_code")
    private String petCode = "petcode";

    @Column(name = "pet_nickname", nullable = false)
    private String petNickname;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private User user;

    @PrePersist
    public void prePersist() {
        // 펫 코드 기본값 처리 (petcode)
        if (this.petCode == null || this.petCode.isBlank()) {
            this.petCode = "petcode";
        }

        // 1. null이면 "토리"으로 대체
        this.petNickname = (this.petNickname == null || this.petNickname.isBlank())
                ? "토리"
                : this.petNickname.strip();
    }
    // 펫 닉네임 수정
    public void updatePetNickname(String petNickname) {
        if (petNickname != null && !petNickname.isBlank()) {
            this.petNickname = petNickname.strip();
        }
    }

    /**
     * 펫의 성장 정보(레벨, 경험치)를 업데이트합니다.
     *
     * @param petLevel   계산된 현재 레벨
     * @param currentExp 현재 레벨 내에서의 경험치
     * @param maxExp     현재 레벨의 목표 경험치
     */
    // 경험치 관련 수정
    public void updateExpInfo(Integer petLevel, Integer currentExp, Integer maxExp) {
        if (petLevel != null) this.petLevel = petLevel;
        if (currentExp != null) this.currentExp = currentExp;
        if (maxExp != null) this.maxExp = maxExp;
    }

    // 펫 코드 관련 수정
    public void updatePetCode(String petCode) {
        if (petCode != null && !petCode.isBlank()) {
            this.petCode = petCode;
        }
    }

    // 메인 펫 여부 수정
    public void updateMainStatus(Boolean isMain) {
        if (isMain != null) {
            this.isMain = isMain;
        }
    }
}