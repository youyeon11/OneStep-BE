package com.a508.onestep.domain.user.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.UserStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.web.bind.annotation.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    @Column(name = "user_code", nullable = false)
    private String userCode;

    @Column(name = "recovery_level", nullable = false)
    private Integer recoveryLevel;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "total_exp", nullable = false)
    private Integer totalExp;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus;

    @Column(name = "inactivated_at")
    private LocalDateTime inactivatedAt;

    @Column(name = "terms_agree")
    private Boolean termsAgree;

    @Column(name = "gps_opt_in")
    private Boolean gpsOptIn;

    @Column(name = "notif_opt_in")
    private Boolean notifOptIn;

    @Builder.Default
    @Column(name = "is_open", nullable = false)
    private Boolean isOpen = false;

    // 닉네임 수정 메서드
    public void updateNickname(String nickname) {
        // 예: 닉네임이 비어있으면 안 된다는 규칙이 있다면?
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        this.nickname = nickname;
    }

    // GPS, 알림 동의 메서드
    public void updateAgreements(Boolean gpsOptIn, Boolean notifOptIn) {
        if (gpsOptIn != null) {
            this.gpsOptIn = gpsOptIn;
        }
        if (notifOptIn != null) {
            this.notifOptIn = notifOptIn;
        }
    }

    // 설문조사 결과 반영 (총점 → 회복 레벨 변환)
    public void updateSurveyResult(int totalScore) {
        if (totalScore <= 20) {
            this.recoveryLevel = 1;
        } else if (totalScore <= 30) {
            this.recoveryLevel = 2;
        } else {
            this.recoveryLevel = 3;
        }
    }


    // 회원 탈퇴 메서드
    public void inactivateUser() {
        this.userStatus = UserStatus.INACTIVE;
        this.inactivatedAt = LocalDateTime.now();
    }

    // 회원 정보 업데이트
    public void updateInfo(String email) {
        if (email != null) {
            this.email = email;
        }
    }
    /**
     * 사용자의 누적 경험치를 추가합니다.
     * @param amount 획득한 경험치 양
     */
    // 경험치 추가 메서드
    public void addTotalExp(int amount) {
        if (amount > 0) {
            if (this.totalExp == null) this.totalExp = 0;
            this.totalExp += amount;
        }
    }

    // 챌린지(할 일) 완료 후 포인트 업데이트
    public void updateTotalExp(Integer exp) {
        this.totalExp += exp;
    }

    // 편지 수신 모드(isOpen)를 토글합니다.
    public boolean toggleIsOpen() {
        this.isOpen = !this.isOpen;
        return this.isOpen;
    }
}
