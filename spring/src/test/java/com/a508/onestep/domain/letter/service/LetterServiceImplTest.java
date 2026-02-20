package com.a508.onestep.domain.letter.service;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.letter.dto.request.LetterCreateRequestDto;
import com.a508.onestep.domain.letter.dto.response.*;
import com.a508.onestep.domain.letter.entity.FilterStatus;
import com.a508.onestep.domain.letter.entity.Letter;
import com.a508.onestep.domain.letter.entity.LetterDelivery;
import com.a508.onestep.domain.letter.entity.StorageStatus;
import com.a508.onestep.domain.letter.repository.LetterDeliveryRepository;
import com.a508.onestep.domain.letter.repository.LetterRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LetterServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private LetterRepository letterRepository;
    @Mock private LetterDeliveryRepository letterDeliveryRepository;
    @Mock private GoogleGenAiChatModel googleGenAiChatModel;
    @Mock private ApplicationEventPublisher eventPublisher;
    @InjectMocks private LetterServiceImpl letterService;

    private User testUser;
    private Letter testLetter;
    private LetterDelivery testDelivery;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .userCode("USER001")
                .isOpen(true)
                .build();

        testLetter = Letter.builder()
                .id(1L)
                .user(testUser)
                .title("테스트 제목")
                .content("테스트 내용입니다.")
                .filterStatus(FilterStatus.PASS)
                .build();

        testDelivery = LetterDelivery.builder()
                .id(1L)
                .receiver(testUser)
                .letter(testLetter)
                .deliveredAt(LocalDateTime.now())
                .storageStatus(StorageStatus.UNSAVED)
                .isRead(false)
                .build();
    }

    @Nested
    @DisplayName("편지 작성 테스트")
    class CreateLetterTest {

        @Test
        @DisplayName("편지 작성 성공")
        void createLetter_Success() {
            // given
            LetterCreateRequestDto requestDto = new LetterCreateRequestDto("인사", "좋은 하루 보내세요!");

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterRepository.existsByUserAndCreatedAtBetween(any(), any(), any())).willReturn(false);

                // AI 응답 Mock
                ChatResponse mockResponse = mock(ChatResponse.class);
                Generation mockGeneration = mock(Generation.class);
                AssistantMessage mockMessage = new AssistantMessage("{\"title\": \"좋은 하루\", \"status\": \"PASS\"}");

                given(googleGenAiChatModel.call(any(Prompt.class))).willReturn(mockResponse);
                given(mockResponse.getResult()).willReturn(mockGeneration);
                given(mockGeneration.getOutput()).willReturn(mockMessage);
                given(letterRepository.save(any(Letter.class))).willReturn(testLetter);

                // when
                Long letterId = letterService.createLetter(requestDto);

                // then
                assertThat(letterId).isEqualTo(1L);
                verify(letterRepository).save(any(Letter.class));
                verify(eventPublisher).publishEvent(any(ChallengeCompletedEvent.class));
            }
        }

        @Test
        @DisplayName("오늘 이미 작성한 경우 예외 발생")
        void createLetter_AlreadyWrittenToday_ThrowsException() {
            // given
            LetterCreateRequestDto requestDto = new LetterCreateRequestDto("제목", "테스트 내용");

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterRepository.existsByUserAndCreatedAtBetween(any(), any(), any())).willReturn(true);

                // when & then
                assertThatThrownBy(() -> letterService.createLetter(requestDto))
                        .isInstanceOf(BusinessException.class)
                        .extracting("baseCode")
                        .isEqualTo(ErrorCode.LETTER_ALREADY_WRITTEN_TODAY);
            }
        }

        @Test
        @DisplayName("AI가 FAIL 판정한 경우 저장은 되지만 FAIL 상태")
        void createLetter_AiFilterFail() {
            // given
            LetterCreateRequestDto requestDto = new LetterCreateRequestDto("제목", "비속어가 포함된 내용");

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterRepository.existsByUserAndCreatedAtBetween(any(), any(), any())).willReturn(false);

                // AI 응답 Mock
                ChatResponse mockResponse = mock(ChatResponse.class);
                Generation mockGeneration = mock(Generation.class);
                AssistantMessage mockMessage = new AssistantMessage("{\"title\": \"부적절한 내용\", \"status\": \"FAIL\"}");

                given(googleGenAiChatModel.call(any(Prompt.class))).willReturn(mockResponse);
                given(mockResponse.getResult()).willReturn(mockGeneration);
                given(mockGeneration.getOutput()).willReturn(mockMessage);

                Letter failedLetter = Letter.builder()
                        .id(1L)
                        .user(testUser)
                        .title("부적절한 내용")
                        .content(requestDto.getContent())
                        .filterStatus(FilterStatus.FAIL)
                        .build();

                given(letterRepository.save(any(Letter.class))).willReturn(failedLetter);

                // when
                Long letterId = letterService.createLetter(requestDto);

                // then
                assertThat(letterId).isEqualTo(1L);
                verify(letterRepository).save(argThat(letter ->
                        letter.getFilterStatus() == FilterStatus.FAIL
                ));
            }
        }
    }

    @Nested
    @DisplayName("편지 수신 테스트")
    class ReceiveLetterTest {

        @Test
        @DisplayName("편지 수신 성공")
        void receiveTodayLetter_Success() {
            // given
            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findTodayDelivery(anyLong(), any(), any()))
                        .willReturn(Optional.empty());
                given(letterRepository.findCandidateLetterIds(anyLong()))
                        .willReturn(new ArrayList<>(List.of(1L, 2L, 3L)));

                given(letterRepository.findById(anyLong())).willReturn(Optional.of(testLetter));
                given(letterDeliveryRepository.save(any(LetterDelivery.class))).willReturn(testDelivery);

                // when
                LetterReceiveResponseDto response = letterService.receiveTodayLetter();

                // then
                assertThat(response).isNotNull();
                assertThat(response.getLetterId()).isEqualTo(1L);
                assertThat(response.getTitle()).isEqualTo("테스트 제목");
                assertThat(response.getContent()).isEqualTo("테스트 내용입니다.");
                assertThat(response.getDeliveredAt()).isNotNull();

                verify(letterDeliveryRepository).save(any(LetterDelivery.class));
            }
        }

        @Test
        @DisplayName("오늘 이미 수신한 경우 예외 발생")
        void receiveTodayLetter_AlreadyReceivedToday_ThrowsException() {
            // given
            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findTodayDelivery(anyLong(), any(), any()))
                        .willReturn(Optional.of(testDelivery));

                // when & then
                assertThatThrownBy(() -> letterService.receiveTodayLetter())
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.LETTER_ALREADY_RECEIVED_TODAY.getMessage());
            }
        }

        @Test
        @DisplayName("수신 가능한 편지가 없으면 null 반환")
        void receiveTodayLetter_NoAvailableLetters_ReturnsNull() {
            // given
            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findTodayDelivery(anyLong(), any(), any()))
                        .willReturn(Optional.empty());
                given(letterRepository.findCandidateLetterIds(anyLong()))
                        .willReturn(Collections.emptyList());

                // when
                LetterReceiveResponseDto response = letterService.receiveTodayLetter();

                // then
                assertThat(response).isNotNull();
                assertThat(response.getLetterId()).isNull();
                assertThat(response.getTitle()).isNull();
                assertThat(response.getContent()).isNull();
                assertThat(response.getDeliveredAt()).isNull();

                verify(letterDeliveryRepository, times(0)).save(any());
            }
        }
    }

    @Nested
    @DisplayName("편지 보관 테스트")
    class SaveLetterTest {

        @Test
        @DisplayName("편지 보관 성공 (UNSAVED → SAVED)")
        void saveLetter_Success() {
            // given
            Long letterId = 1L;

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndLetterIdAndStorageStatus(
                        anyLong(), anyLong(), eq(StorageStatus.UNSAVED)))
                        .willReturn(Optional.of(testDelivery));

                // when
                LetterStatusResponseDto response = letterService.saveLetter(letterId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getLetterId()).isEqualTo(letterId);
                assertThat(response.getStorageStatus()).isEqualTo(StorageStatus.SAVED);
            }
        }

        @Test
        @DisplayName("존재하지 않는 편지 보관 시도 시 예외 발생")
        void saveLetter_LetterNotFound_ThrowsException() {
            // given
            Long letterId = 999L;

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndLetterIdAndStorageStatus(
                        anyLong(), anyLong(), eq(StorageStatus.UNSAVED)))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> letterService.saveLetter(letterId))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.LETTER_NOT_FOUND.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("편지 삭제 테스트")
    class DeleteLetterTest {

        @Test
        @DisplayName("편지 삭제 성공")
        void deleteLetter_Success() {
            // given
            Long letterId = 1L;

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndLetterId(anyLong(), anyLong()))
                        .willReturn(Optional.of(testDelivery));

                // when
                LetterStatusResponseDto response = letterService.deleteLetter(letterId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getLetterId()).isEqualTo(letterId);
                assertThat(response.getStorageStatus()).isEqualTo(StorageStatus.DELETED);
            }
        }

        @Test
        @DisplayName("존재하지 않는 편지 삭제 시도 시 예외 발생")
        void deleteLetter_LetterNotFound_ThrowsException() {
            // given
            Long letterId = 999L;

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndLetterId(anyLong(), anyLong()))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> letterService.deleteLetter(letterId))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.LETTER_NOT_FOUND.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("보관함 조회 테스트")
    class GetSavedLettersTest {

        @Test
        @DisplayName("보관함 편지 목록 조회 성공")
        void getSavedLetters_Success() {
            // given
            List<LetterDelivery> savedDeliveries = List.of(testDelivery);

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndStorageStatusWithLetter(
                        anyLong(), eq(StorageStatus.SAVED)))
                        .willReturn(savedDeliveries);

                // when
                List<SavedLetterListItemResponseDto> response = letterService.getSavedLetters();

                // then
                assertThat(response).isNotEmpty();
                assertThat(response).hasSize(1);
            }
        }

        @Test
        @DisplayName("보관함이 비어있으면 빈 리스트 반환")
        void getSavedLetters_EmptyList() {
            // given
            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndStorageStatusWithLetter(
                        anyLong(), eq(StorageStatus.SAVED)))
                        .willReturn(Collections.emptyList());

                // when
                List<SavedLetterListItemResponseDto> response = letterService.getSavedLetters();

                // then
                assertThat(response).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("보관함 상세 조회 테스트")
    class GetSavedLetterDetailTest {

        @Test
        @DisplayName("보관함 편지 상세 조회 성공")
        void getSavedLetterDetail_Success() {
            // given
            Long letterId = 1L;

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndLetterIdAndStorageStatus(
                        anyLong(), anyLong(), eq(StorageStatus.SAVED)))
                        .willReturn(Optional.of(testDelivery));

                // when
                SavedLetterDetailResponseDto response = letterService.getSavedLetterDetail(letterId);

                // then
                assertThat(response).isNotNull();
                assertThat(response.getContent()).isEqualTo("테스트 내용입니다.");
            }
        }

        @Test
        @DisplayName("보관되지 않은 편지 조회 시 예외 발생")
        void getSavedLetterDetail_NotSaved_ThrowsException() {
            // given
            Long letterId = 1L;

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));
                given(letterDeliveryRepository.findByReceiverIdAndLetterIdAndStorageStatus(
                        anyLong(), anyLong(), eq(StorageStatus.SAVED)))
                        .willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> letterService.getSavedLetterDetail(letterId))
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.LETTER_NOT_FOUND.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("수신 상태 테스트")
    class ReceiveStatusTest {

        @Test
        @DisplayName("현재 수신 상태 조회 성공")
        void getReceiveStatus_Success() {
            // given
            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("USER001");
                given(userRepository.findByUserCode("USER001")).willReturn(Optional.of(testUser));

                // when
                ReceiveStatusResponseDto response = letterService.getReceiveStatus();

                // then
                assertThat(response).isNotNull();
                assertThat(response.isOpen()).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("공통 예외 테스트")
    class CommonExceptionTest {

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 예외 발생")
        void getLoginUser_UserNotFound_ThrowsException() {
            // given
            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getUserCode).thenReturn("UNKNOWN");
                given(userRepository.findByUserCode("UNKNOWN")).willReturn(Optional.empty());

                // when & then
                assertThatThrownBy(() -> letterService.getReceiveStatus())
                        .isInstanceOf(BusinessException.class)
                        .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());
            }
        }
    }
}