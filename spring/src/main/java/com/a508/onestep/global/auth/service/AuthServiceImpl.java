package com.a508.onestep.global.auth.service;

import com.a508.onestep.domain.common.RoleType;
import com.a508.onestep.domain.common.UserStatus;
import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.pet.util.PetLevelCalculator;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.auth.dto.request.KakaoLoginRequestDto;
import com.a508.onestep.global.auth.dto.request.LoginRequestDto;
import com.a508.onestep.global.auth.dto.response.LoginResponseDto;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.client.kakao.KakaoApiClient;
import com.a508.onestep.global.client.kakao.dto.KakaoUserInfoResponseDto;
import com.a508.onestep.global.kafka.producer.KafkaProducer;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.auth.utils.JwtUtils;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.auth.utils.UserCodeGenerator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Value("${spring.jwt.refresh.expiration}")
    private long refreshTokenExpirationTime;

    private final UserRepository userRepository;
    private final PetOwnershipRepository petOwnershipRepository;
    private final JwtUtils jwtUtils;
    private final RedisTemplate<String, String> redisTemplate;
    private final KakaoApiClient kakaoApiClient;
    private final KafkaProducer kafkaProducer;

    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * 카카오 로그인하기
     * 안드로이드 앱에서 카카오 OAuth 토큰을 받아 로그인/회원가입 처리
     */
    @Transactional
    public LoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        LogUtils.info("카카오 사용자 정보 요청");
        KakaoUserInfoResponseDto userInfo = kakaoApiClient.requestUserInfo(requestDto.getKakaoToken());
        LogUtils.info("카카오 사용자 정보 : {}", userInfo);

        // userInfo 검증
        if (userInfo == null || userInfo.getKakaoAccount().getEmail() == null) {
            throw BusinessException.of(ErrorCode.KAKAO_USER_INFO_FAILED);
        }

        String email = userInfo.getKakaoAccount().getEmail();
        Optional<User> existingUser = userRepository.findByEmail(email);

        final User user;
        final boolean isNew;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            // pet 존재 유무로 신규 여부 판단
            isNew = !petOwnershipRepository.existsByUserId(user.getId());

            LogUtils.info("카카오 기존 회원 로그인: userCode={}, email={}",
                    user.getUserCode(), user.getEmail());
        } else {
            // 신규 회원 - User와 Pet을 함께 생성
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

            // Pet 생성
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

        // JWT 토큰 생성
        String accessToken = jwtUtils.generateAccessToken(
                user.getUserCode(), RoleType.ROLE_USER.name());
        String refreshToken = jwtUtils.generateRefreshToken(
                user.getUserCode(), RoleType.ROLE_USER.name());

        saveRefreshToken(user.getUserCode(), refreshToken);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNew(isNew)
                .build();
    }

    /**
     * 게스트 로그인하기
     * 임시 계정으로 앱 사용 (닉네임만 입력)
     */
    @Transactional
    public LoginResponseDto guestLogin(LoginRequestDto requestDto) {
        String userCode = requestDto.getUserCode();

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

        // JWT 토큰 생성
        String accessToken = jwtUtils.generateAccessToken(userCode, RoleType.ROLE_USER.name());
        String refreshToken = jwtUtils.generateRefreshToken(userCode, RoleType.ROLE_USER.name());

        // Redis에 Refresh Token 저장
        saveRefreshToken(userCode, refreshToken);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .isNew(isNew)
                .build();
    }

    /**
     * 로그아웃하기
     * Redis에서 Refresh Token 삭제
     */
    @Transactional
    public void logout() {
        String userCode = UserContextHolder.getUserCode();
        String key = REFRESH_TOKEN_PREFIX + userCode;

        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            LogUtils.info("로그아웃 성공: userCode={}", userCode);
        } else {
            LogUtils.warn("로그아웃 - Redis에 토큰 없음: userCode={}", userCode);
        }
    }

    /**
     * 토큰 재발급하기
     * Refresh Token으로 새로운 Access Token 발급
     */
    @Transactional
    public LoginResponseDto reissue(String refreshToken) {

        // Refresh Token 유효성 검증
        try {
            if (!jwtUtils.validateToken(refreshToken)) {
                throw BusinessException.of(ErrorCode.TOKEN_INVALID);
            }
        } catch (Exception e) {
            LogUtils.error("Refresh Token 검증 실패: {}", e.getMessage());
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        }

        // Refresh Token에서 UserCode 추출
        String userCode = jwtUtils.getUserCode(refreshToken);
        LogUtils.info("토큰 재발급 요청: userCode={}", userCode);

        // 사용자 존재 여부 확인
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // Redis에 저장된 Refresh Token과 비교
        String key = REFRESH_TOKEN_PREFIX + userCode;
        String savedRefreshToken = redisTemplate.opsForValue().get(key);

        if (savedRefreshToken == null) {
            LogUtils.warn("Redis에 저장된 Refresh Token 없음: userCode={}", userCode);
            throw BusinessException.of(ErrorCode.TOKEN_NOT_FOUND);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            LogUtils.warn("Refresh Token 불일치: userCode={}", userCode);
            throw BusinessException.of(ErrorCode.UNMATCHED_REFRESH_TOKEN);
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtUtils.generateAccessToken(userCode, RoleType.ROLE_USER.name());
        LogUtils.info("Access Token 재발급 성공: userCode={}", userCode);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .isNew(false)
                .build();
    }

    /**
     * Refresh Token을 Redis에 저장
     */
    private void saveRefreshToken(String userCode, String refreshToken) {
        String key = REFRESH_TOKEN_PREFIX + userCode;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                refreshTokenExpirationTime,
                TimeUnit.MILLISECONDS
        );
        LogUtils.debug("Refresh Token 저장 완료: userCode={}, TTL={}ms", userCode, refreshTokenExpirationTime);
    }

    /**
     * 기존의 회원을 kakao 계정과 연동
     */
    @Transactional
    public void linkToKakao(KakaoLoginRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        LogUtils.info("{} 의 카카오톡 연동 시작...", userCode);

        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        // 회원 카카오 계정 얻기
        KakaoUserInfoResponseDto userInfoResponseDto = kakaoApiClient.requestUserInfo(requestDto.getKakaoToken());
        String email = userInfoResponseDto.getKakaoAccount().getEmail();
        if (email == null) {
            LogUtils.warn("카카오로부터 이메일 정보를 얻는 데에 실패하였습니다.");
            throw BusinessException.of(ErrorCode.KAKAO_USER_INFO_FAILED);
        }

        if (user.getEmail() != null) {
            LogUtils.warn("이미 회원의 이메일이 존재합니다. : {}", user.getEmail());
            throw BusinessException.of(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        user.updateInfo(email);
    }

    /**
     * 회원탈퇴
     */
    @Transactional
    public void inactive() {
        String userCode = UserContextHolder.getUserCode();
        LogUtils.info("{} 의 회원 탈퇴...", userCode);

        LogUtils.info("inactive called, userCode={}", userCode);

        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        LogUtils.info("db userStatus={}, inactivatedAt={}", user.getUserStatus(), user.getInactivatedAt());

        if (UserStatus.INACTIVE.equals(user.getUserStatus())) {
            throw BusinessException.of(ErrorCode.INVALID_USER_STATUS);
        }

        String key = REFRESH_TOKEN_PREFIX + userCode;
        Boolean deleted = redisTemplate.delete(key);
        if (Boolean.TRUE.equals(deleted)) {
            LogUtils.debug("Refresh Token 삭제 성공: userCode={}", userCode);
        } else if (Boolean.FALSE.equals(deleted)) {
            LogUtils.debug("Refresh Token 없음: userCode={}", userCode);
        } else {
            LogUtils.warn("Refresh Token 삭제 결과 null: userCode={}", userCode);
        }
        user.inactivateUser();
    }
}
