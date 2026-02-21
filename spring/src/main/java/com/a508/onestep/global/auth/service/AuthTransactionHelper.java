package com.a508.onestep.global.auth.service;

import com.a508.onestep.domain.common.UserStatus;
import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.pet.util.PetLevelCalculator;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.utils.UserCodeGenerator;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.response.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthTransactionHelper {

    private final UserRepository userRepository;
    private final PetOwnershipRepository petOwnershipRepository;

    @Getter
    public static class KakaoLoginResult {
        private final String userCode;
        private final boolean isNew;

        public KakaoLoginResult(String userCode, boolean isNew) {
            this.userCode = userCode;
            this.isNew = isNew;
        }
    }

    @Transactional
    public Boolean executeGuestLogin(String userCode) {

        Optional<User> existingUser = userRepository.findByUserCode(userCode);

        final User user;
        final boolean isNew;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            isNew = !petOwnershipRepository.existsByUserId(user.getId());

            LogUtils.info("게스트 기존 회원 재로그인: userCode={}", userCode);
        } else {
            // 신규 게스트 - User 생성
            user = User.builder()
                    .userCode(userCode)
                    .nickname("김싸피")
                    .recoveryLevel(1)
                    .totalExp(0)
                    .userStatus(UserStatus.ACTIVE)
                    .termsAgree(true)
                    .gpsOptIn(false)
                    .notifOptIn(false)
                    .build();

            // User 저장
            userRepository.save(user);

            // Pet 생성 및 저장
            PetOwnership pet = PetOwnership.builder()
                    .currentExp(0)
                    .petLevel(1)
                    .isMain(true)
                    .maxExp(PetLevelCalculator.getMaxExpForLevel(1))
                    .user(user)
                    .build();

            petOwnershipRepository.save(pet);

            isNew = true;
            LogUtils.info("게스트 신규 회원가입: userCode={}", userCode);
        }

        return isNew;
    }

    @Transactional
    public KakaoLoginResult executeKakaoLogin(String email) {
        Optional<User> existingUser = userRepository.findByEmail(email);

        final User user;
        final boolean isNew;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            isNew = !petOwnershipRepository.existsByUserId(user.getId());

            LogUtils.info("카카오 기존 회원 로그인: userCode={}, email={}",
                    user.getUserCode(), user.getEmail());
        } else {
            String userCode = UserCodeGenerator.generate();

            user = User.builder()
                    .userCode(userCode)
                    .email(email)
                    .nickname("김싸피")
                    .recoveryLevel(1)
                    .totalExp(0)
                    .userStatus(UserStatus.ACTIVE)
                    .termsAgree(true)
                    .gpsOptIn(false)
                    .notifOptIn(false)
                    .build();

            PetOwnership pet = PetOwnership.builder()
                    .currentExp(0)
                    .petLevel(1)
                    .isMain(true)
                    .user(user)
                    .maxExp(PetLevelCalculator.getMaxExpForLevel(1))
                    .build();

            userRepository.save(user);
            petOwnershipRepository.save(pet);

            isNew = true;
            LogUtils.info("카카오 신규 회원가입: userCode={}, email={}", userCode, email);
        }

        return new KakaoLoginResult(user.getUserCode(), isNew);
    }

    @Transactional
    public void executeLinkToKakao(String userCode, String email) {
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        if (user.getEmail() != null) {
            LogUtils.warn("이미 회원의 이메일이 존재합니다. : {}", user.getEmail());
            throw BusinessException.of(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        user.updateInfo(email);
    }

}
