package com.a508.onestep.global.auth.service;

import com.a508.onestep.global.auth.dto.request.KakaoLoginRequestDto;
import com.a508.onestep.global.auth.dto.request.LoginRequestDto;
import com.a508.onestep.global.auth.dto.response.LoginResponseDto;
import com.a508.onestep.domain.common.RoleType;
import com.a508.onestep.domain.common.UserStatus;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.auth.utils.JwtUtils;
import com.a508.onestep.global.auth.utils.UserCodeGenerator;
import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.client.kakao.KakaoApiClient;
import com.a508.onestep.global.client.kakao.dto.KakaoUserInfoResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith({MockitoExtension.class})
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PetOwnershipRepository petOwnershipRepository;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private KakaoApiClient kakaoApiClient;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthServiceImpl authService;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_CODE = "USER123456";
    private static final String TEST_EMAIL = "test@kakao.com";
    private static final String TEST_NICKNAME = "테스트닉네임";
    private static final String TEST_KAKAO_TOKEN = "kakao_access_token";
    private static final String TEST_ACCESS_TOKEN = "access_token";
    private static final String TEST_REFRESH_TOKEN = "refresh_token";
    private static final long REFRESH_TOKEN_EXPIRATION = 604800000L; // 7일

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationTime", REFRESH_TOKEN_EXPIRATION);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Nested
    @DisplayName("카카오 로그인")
    class KakaoLoginTest {

        private KakaoLoginRequestDto requestDto;
        private KakaoUserInfoResponseDto userInfoDto;

        @BeforeEach
        void setUp() {
            requestDto = KakaoLoginRequestDto.builder()
                    .kakaoToken(TEST_KAKAO_TOKEN)
                    .build();

            userInfoDto = createKakaoUserInfo(TEST_EMAIL);
        }

        @Test
        @DisplayName("신규 회원 - 카카오 로그인 성공")
        void kakaoLogin_NewUser_Success() {
            // given
            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(userInfoDto);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.empty());

            User savedUser = createUser(TEST_USER_CODE, TEST_EMAIL, TEST_NICKNAME);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            given(jwtUtils.generateAccessToken(anyString(), eq(RoleType.ROLE_USER.name())))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtUtils.generateRefreshToken(anyString(), eq(RoleType.ROLE_USER.name())))
                    .willReturn(TEST_REFRESH_TOKEN);

            try (MockedStatic<UserCodeGenerator> mockedGenerator = mockStatic(UserCodeGenerator.class)) {
                mockedGenerator.when(UserCodeGenerator::generate).thenReturn(TEST_USER_CODE);

                // when
                LoginResponseDto result = authService.kakaoLogin(requestDto);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
                assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
                assertThat(result.getIsNew()).isTrue();

                verify(userRepository).save(any(User.class));
                verify(valueOperations).set(
                        eq("refresh_token:" + TEST_USER_CODE),
                        eq(TEST_REFRESH_TOKEN),
                        eq(REFRESH_TOKEN_EXPIRATION),
                        eq(TimeUnit.MILLISECONDS)
                );
            }
        }

        @Test
        @DisplayName("기존 회원 - Pet 있음 (isNew = false)")
        void kakaoLogin_ExistingUserWithPet_Success() {
            // given
            User existingUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .nickname(TEST_NICKNAME)
                    .userCode(TEST_USER_CODE)
                    .build();
            PetOwnership petOwnership = createPetOwnership(existingUser);

            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(userInfoDto);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(existingUser));
            given(petOwnershipRepository.findByUserId(existingUser.getId())).willReturn(Optional.of(petOwnership));
            given(jwtUtils.generateAccessToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtUtils.generateRefreshToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_REFRESH_TOKEN);

            // when
            LoginResponseDto result = authService.kakaoLogin(requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.getIsNew()).isFalse();

            verify(userRepository, never()).save(any(User.class));
            verify(petOwnershipRepository).findByUserId(existingUser.getId());
            verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("기존 회원 - Pet 없음 (isNew = true)")
        void kakaoLogin_ExistingUserWithoutPet_Success() {
            // given
            User existingUser = User.builder()
                    .id(TEST_USER_ID)
                    .email(TEST_EMAIL)
                    .nickname(TEST_NICKNAME)
                    .userCode(TEST_USER_CODE)
                    .build();

            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(userInfoDto);
            given(userRepository.findByEmail(TEST_EMAIL)).willReturn(Optional.of(existingUser));
            given(petOwnershipRepository.findByUserId(existingUser.getId())).willReturn(Optional.empty());
            given(jwtUtils.generateAccessToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtUtils.generateRefreshToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_REFRESH_TOKEN);

            // when
            LoginResponseDto result = authService.kakaoLogin(requestDto);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
            assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.getIsNew()).isTrue(); // Pet이 없으므로 true

            verify(userRepository, never()).save(any(User.class));
            verify(petOwnershipRepository).findByUserId(existingUser.getId());
            verify(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("카카오 사용자 정보 조회 실패")
        void kakaoLogin_UserInfoNull_ThrowsException() {
            // given
            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.kakaoLogin(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.KAKAO_USER_INFO_FAILED);
        }

        @Test
        @DisplayName("카카오 이메일 정보 없음")
        void kakaoLogin_EmailNull_ThrowsException() {
            // given
            KakaoUserInfoResponseDto invalidUserInfo = createKakaoUserInfo(null);
            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(invalidUserInfo);

            // when & then
            assertThatThrownBy(() -> authService.kakaoLogin(requestDto))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.KAKAO_USER_INFO_FAILED);
        }
    }

    @Nested
    @DisplayName("게스트 로그인")
    class GuestLoginTest {

        private LoginRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = LoginRequestDto.builder()
                    .userCode(TEST_USER_CODE)
                    .build();
        }

        @Test
        @DisplayName("신규 게스트 로그인 성공")
        void guestLogin_NewGuest_Success() {
            // given
            User savedUser = createGuestUser(TEST_USER_CODE, TEST_NICKNAME);

            ReflectionTestUtils.setField(savedUser, "id", 1L);

            given(userRepository.save(any(User.class))).willReturn(savedUser);
            given(jwtUtils.generateAccessToken(anyString(), eq(RoleType.ROLE_USER.name())))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtUtils.generateRefreshToken(anyString(), eq(RoleType.ROLE_USER.name())))
                    .willReturn(TEST_REFRESH_TOKEN);

            try (MockedStatic<UserCodeGenerator> mockedGenerator = mockStatic(UserCodeGenerator.class);
                 MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {

                mockedGenerator.when(UserCodeGenerator::generate).thenReturn(TEST_USER_CODE);
                mockedContext.when(UserContextHolder::getUserCode)
                        .thenThrow(BusinessException.of(ErrorCode.USER_NOT_FOUND));

                // when
                LoginResponseDto result = authService.guestLogin(requestDto);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
                assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
                assertThat(result.getIsNew()).isTrue();

                verify(userRepository).save(any(User.class));
            }
        }

        @Test
        @DisplayName("기존 게스트 - Pet 있음 (isNew = false)")
        void guestLogin_ExistingGuestWithPet_Success() {
            // given
            User existingUser = User.builder()
                    .id(TEST_USER_ID)
                    .nickname(TEST_NICKNAME)
                    .userCode(TEST_USER_CODE)
                    .build();
            PetOwnership petOwnership = createPetOwnership(existingUser);

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(existingUser));
            given(petOwnershipRepository.findByUserId(TEST_USER_ID)).willReturn(Optional.of(petOwnership));
            given(jwtUtils.generateAccessToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtUtils.generateRefreshToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_REFRESH_TOKEN);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when
                LoginResponseDto result = authService.guestLogin(requestDto);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
                assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
                assertThat(result.getIsNew()).isFalse();

                verify(userRepository, never()).save(any(User.class));
                verify(petOwnershipRepository).findByUserId(TEST_USER_ID);
            }
        }

        @Test
        @DisplayName("기존 게스트 - Pet 없음 (isNew = true)")
        void guestLogin_ExistingGuestWithoutPet_Success() {
            // given
            User existingUser = User.builder()
                    .id(TEST_USER_ID)
                    .nickname(TEST_NICKNAME)
                    .userCode(TEST_USER_CODE)
                    .build();

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(existingUser));
            given(petOwnershipRepository.findByUserId(TEST_USER_ID)).willReturn(Optional.empty());
            given(jwtUtils.generateAccessToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_ACCESS_TOKEN);
            given(jwtUtils.generateRefreshToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn(TEST_REFRESH_TOKEN);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when
                LoginResponseDto result = authService.guestLogin(requestDto);

                // then
                assertThat(result).isNotNull();
                assertThat(result.getAccessToken()).isEqualTo(TEST_ACCESS_TOKEN);
                assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
                assertThat(result.getIsNew()).isTrue(); // Pet이 없으므로 true

                verify(userRepository, never()).save(any(User.class));
                verify(petOwnershipRepository).findByUserId(TEST_USER_ID);
            }
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() {
            // given
            given(redisTemplate.delete("refresh_token:" + TEST_USER_CODE)).willReturn(true);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when
                authService.logout();

                // then
                verify(redisTemplate).delete("refresh_token:" + TEST_USER_CODE);
            }
        }

        @Test
        @DisplayName("Redis에 토큰 없음 - 경고 로그")
        void logout_TokenNotFound_LogWarning() {
            // given
            given(redisTemplate.delete("refresh_token:" + TEST_USER_CODE)).willReturn(false);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when
                authService.logout();

                // then
                verify(redisTemplate).delete("refresh_token:" + TEST_USER_CODE);
            }
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class ReissueTest {

        @Test
        @DisplayName("토큰 재발급 성공")
        void reissue_Success() throws Exception {
            // given
            User user = createUser(TEST_USER_CODE, TEST_EMAIL, TEST_NICKNAME);

            given(jwtUtils.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
            given(jwtUtils.getUserCode(TEST_REFRESH_TOKEN)).willReturn(TEST_USER_CODE);
            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));
            given(valueOperations.get("refresh_token:" + TEST_USER_CODE))
                    .willReturn(TEST_REFRESH_TOKEN);
            given(jwtUtils.generateAccessToken(TEST_USER_CODE, RoleType.ROLE_USER.name()))
                    .willReturn("new_access_token");

            // when
            LoginResponseDto result = authService.reissue(TEST_REFRESH_TOKEN);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("new_access_token");
            assertThat(result.getRefreshToken()).isEqualTo(TEST_REFRESH_TOKEN);
            assertThat(result.getIsNew()).isFalse();
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token")
        void reissue_InvalidToken_ThrowsException() throws Exception {
            // given
            given(jwtUtils.validateToken(TEST_REFRESH_TOKEN)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.TOKEN_INVALID);
        }

        @Test
        @DisplayName("사용자를 찾을 수 없음")
        void reissue_UserNotFound_ThrowsException() throws Exception {
            // given
            given(jwtUtils.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
            given(jwtUtils.getUserCode(TEST_REFRESH_TOKEN)).willReturn(TEST_USER_CODE);
            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("Redis에 저장된 토큰 없음")
        void reissue_TokenNotFoundInRedis_ThrowsException() throws Exception {
            // given
            User user = createUser(TEST_USER_CODE, TEST_EMAIL, TEST_NICKNAME);

            given(jwtUtils.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
            given(jwtUtils.getUserCode(TEST_REFRESH_TOKEN)).willReturn(TEST_USER_CODE);
            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));
            given(valueOperations.get("refresh_token:" + TEST_USER_CODE)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("Refresh Token 불일치")
        void reissue_TokenMismatch_ThrowsException() throws Exception {
            // given
            User user = createUser(TEST_USER_CODE, TEST_EMAIL, TEST_NICKNAME);

            given(jwtUtils.validateToken(TEST_REFRESH_TOKEN)).willReturn(true);
            given(jwtUtils.getUserCode(TEST_REFRESH_TOKEN)).willReturn(TEST_USER_CODE);
            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));
            given(valueOperations.get("refresh_token:" + TEST_USER_CODE))
                    .willReturn("different_refresh_token");

            // when & then
            assertThatThrownBy(() -> authService.reissue(TEST_REFRESH_TOKEN))
                    .isInstanceOf(BusinessException.class)
                    .extracting("baseCode")
                    .isEqualTo(ErrorCode.UNMATCHED_REFRESH_TOKEN);
        }
    }

    // 테스트용 헬퍼 메서드
    private User createUser(String userCode, String email, String nickname) {
        return User.builder()
                .userCode(userCode)
                .email(email)
                .nickname(nickname)
                .recoveryLevel(1)
                .totalExp(0)
                .userStatus(UserStatus.ACTIVE)
                .termsAgree(true)
                .gpsOptIn(false)
                .notifOptIn(false)
                .build();
    }

    private User createGuestUser(String userCode, String nickname) {
        return User.builder()
                .userCode(userCode)
                .nickname(nickname)
                .recoveryLevel(1)
                .totalExp(0)
                .userStatus(UserStatus.ACTIVE)
                .termsAgree(true)
                .gpsOptIn(false)
                .notifOptIn(false)
                .build();
    }

    private KakaoUserInfoResponseDto createKakaoUserInfo(String email) {
        KakaoUserInfoResponseDto.KakaoAccount account = KakaoUserInfoResponseDto.KakaoAccount.builder()
                .email(email)
                .build();
        return KakaoUserInfoResponseDto.builder()
                .kakaoAccount(account)
                .build();
    }

    private PetOwnership createPetOwnership(User user) {
        return PetOwnership.builder()
                .user(user)
                .build();
    }

    @Nested
    @DisplayName("카카오 계정 연동")
    class LinkToKakaoTest {

        private KakaoLoginRequestDto requestDto;

        @BeforeEach
        void setUp() {
            requestDto = KakaoLoginRequestDto.builder()
                    .kakaoToken(TEST_KAKAO_TOKEN)
                    .build();
        }

        @Test
        @DisplayName("카카오 연동 성공")
        void linkToKakao_Success() {
            // given
            User user = createGuestUser(TEST_USER_CODE, TEST_NICKNAME);

            KakaoUserInfoResponseDto userInfo = createKakaoUserInfo(TEST_EMAIL);

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));
            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(userInfo);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when
                authService.linkToKakao(requestDto);

                // then
                assertThat(user.getEmail()).isEqualTo(TEST_EMAIL);
                verify(kakaoApiClient).requestUserInfo(TEST_KAKAO_TOKEN);
            }
        }

        @Test
        @DisplayName("카카오 연동 실패 - 사용자를 찾을 수 없음")
        void linkToKakao_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.empty());

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when & then
                assertThatThrownBy(() -> authService.linkToKakao(requestDto))
                        .isInstanceOf(BusinessException.class)
                        .extracting("baseCode")
                        .isEqualTo(ErrorCode.USER_NOT_FOUND);

                verify(kakaoApiClient, never()).requestUserInfo(anyString());
            }
        }

        @Test
        @DisplayName("카카오 연동 실패 - 카카오로부터 이메일 정보를 얻지 못함")
        void linkToKakao_EmailNull_ThrowsException() {
            // given
            User user = createGuestUser(TEST_USER_CODE, TEST_NICKNAME);

            KakaoUserInfoResponseDto userInfo = createKakaoUserInfo(null); // 이메일 없음

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));
            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(userInfo);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when & then
                assertThatThrownBy(() -> authService.linkToKakao(requestDto))
                        .isInstanceOf(BusinessException.class)
                        .extracting("baseCode")
                        .isEqualTo(ErrorCode.KAKAO_USER_INFO_FAILED);

                assertThat(user.getEmail()).isNull(); // 이메일이 업데이트되지 않음
            }
        }

        @Test
        @DisplayName("카카오 연동 실패 - 이미 이메일이 존재함")
        void linkToKakao_EmailAlreadyExists_ThrowsException() {
            // given
            String existingEmail = "existing@example.com";
            User user = User.builder()
                    .userCode(TEST_USER_CODE)
                    .email(existingEmail) // 이미 이메일이 있음
                    .nickname(TEST_NICKNAME)
                    .recoveryLevel(1)
                    .totalExp(0)
                    .userStatus(UserStatus.ACTIVE)
                    .termsAgree(true)
                    .gpsOptIn(false)
                    .notifOptIn(false)
                    .build();

            KakaoUserInfoResponseDto userInfo = createKakaoUserInfo(TEST_EMAIL);

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));
            given(kakaoApiClient.requestUserInfo(TEST_KAKAO_TOKEN)).willReturn(userInfo);

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when & then
                assertThatThrownBy(() -> authService.linkToKakao(requestDto))
                        .isInstanceOf(BusinessException.class)
                        .extracting("baseCode")
                        .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

                assertThat(user.getEmail()).isEqualTo(existingEmail); // 이메일이 변경되지 않음
            }
        }
    }

    @Nested
    @DisplayName("회원 탈퇴")
    class InactiveTest {

        @Test
        @DisplayName("회원 탈퇴 성공")
        void inactive_Success() {
            // given
            User user = createUser(TEST_USER_CODE, TEST_EMAIL, TEST_NICKNAME);

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when
                authService.inactive();

                // then
                assertThat(user.getUserStatus()).isEqualTo(UserStatus.INACTIVE);
            }
        }

        @Test
        @DisplayName("회원 탈퇴 실패 - 사용자를 찾을 수 없음")
        void inactive_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.empty());

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when & then
                assertThatThrownBy(() -> authService.inactive())
                        .isInstanceOf(BusinessException.class)
                        .extracting("baseCode")
                        .isEqualTo(ErrorCode.USER_NOT_FOUND);
            }
        }

        @Test
        @DisplayName("회원 탈퇴 실패 - 이미 비활성화된 사용자")
        void inactive_AlreadyInactive_ThrowsException() {
            // given
            User user = User.builder()
                    .userCode(TEST_USER_CODE)
                    .email(TEST_EMAIL)
                    .nickname(TEST_NICKNAME)
                    .recoveryLevel(1)
                    .totalExp(0)
                    .userStatus(UserStatus.INACTIVE) // 이미 비활성 상태
                    .termsAgree(true)
                    .gpsOptIn(false)
                    .notifOptIn(false)
                    .build();

            given(userRepository.findByUserCode(TEST_USER_CODE)).willReturn(Optional.of(user));

            try (MockedStatic<UserContextHolder> mockedContext = mockStatic(UserContextHolder.class)) {
                mockedContext.when(UserContextHolder::getUserCode).thenReturn(TEST_USER_CODE);

                // when & then
                assertThatThrownBy(() -> authService.inactive())
                        .isInstanceOf(BusinessException.class)
                        .extracting("baseCode")
                        .isEqualTo(ErrorCode.INVALID_USER_STATUS);

                assertThat(user.getUserStatus()).isEqualTo(UserStatus.INACTIVE); // 상태 변경 없음
            }
        }
    }
}
