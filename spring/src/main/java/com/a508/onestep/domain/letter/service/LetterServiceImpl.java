package com.a508.onestep.domain.letter.service;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.letter.dto.request.LetterCreateRequestDto;
import com.a508.onestep.domain.letter.dto.response.*;
import com.a508.onestep.domain.letter.entity.*;
import com.a508.onestep.domain.letter.repository.LetterDeliveryRepository;
import com.a508.onestep.domain.letter.repository.LetterRepository;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.response.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LetterServiceImpl implements LetterService {

    // 한국 시간(KST)으로 설정
    private static final java.time.ZoneId KST_ZONE = java.time.ZoneId.of("Asia/Seoul");
    private static final java.time.Clock APP_CLOCK = java.time.Clock.system(KST_ZONE);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepository userRepository;
    private final LetterRepository letterRepository;
    private final LetterDeliveryRepository letterDeliveryRepository;
    private final GoogleGenAiChatModel googleGenAiChatModel;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 편지 작성
     */
    @Override
    @Transactional
    public Long createLetter(LetterCreateRequestDto requestDto) {
        User sender = getLoginUser();

        // 1. 오늘 작성 여부 체크 (UTC 기준으로 통일)
        LocalDate today = LocalDate.now(APP_CLOCK);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        // 작성 했는지 안했는지 확인하는 로직
        if (letterRepository.existsByUserAndCreatedAtBetween(
                sender, startOfDay, startOfNextDay
        )) {
            throw BusinessException.of(ErrorCode.LETTER_ALREADY_WRITTEN_TODAY);
        }

        // 2. AI에게 제목 요약 + 비속어 검사 요청
        String prompt = """
                내용을 분석해서 JSON 형식으로 응답해줘.
                내용: %s
                
                응답 형식:
                {
                  "title": "내용을 한글 10자 이내로 요약",
                  "status": "PASS 또는 FAIL"
                }
                
                필터링 기준 (하나라도 해당하면 FAIL):
                - 비속어, 욕설, 혐오 표현
                - 성적 표현, 폭력적 내용
                - 자해 유도, 부정적 감정 조장
                - 개인정보 요구, 만남 유도
                - 조롱 표현 (예: 메롱)
                
                [예시]
                내용: "너는 가망없어. 뭘 해도 안 될 사람 같아."
                응답: {"title": "부정적 평가", "status": "FAIL"}
                
                내용: "오늘 날씨가 너무 좋아서 산책을 갔다 왔어. 너도 기분 좋은 하루 보냈길 바래!"
                응답: {"title": "행복한 산책", "status": "PASS"}
                
                내용: "야 이 바보야, 너 진짜 짜증 나니까 연락하지 마. 죽어버려."
                응답: {"title": "공격적인 메시지", "status": "FAIL"}
                
                내용: "너 어디 살아? 이름이랑 전화번호 좀 알려줄 수 있어? 지금 바로 갈게."
                응답: {"title": "개인정보 요구", "status": "FAIL"}
                """.formatted(requestDto.getContent());

        JsonNode aiNode = parseAiJson(
                googleGenAiChatModel.call(new Prompt(prompt))
                        .getResult()
                        .getOutput()
                        .getText()
        );

        String title = extractTitle(aiNode);
        FilterStatus filterStatus = extractFilterStatus(aiNode);

        // 3. Letter 저장
        // createdAt/updatedAt은 Auditing이 자동으로 채움
        Letter savedLetter = letterRepository.save(
                Letter.builder()
                        .user(sender)
                        .title(title)
                        .content(requestDto.getContent())
                        .filterStatus(filterStatus)
                        .build()
        );

        // 보상
        ChallengeCompletedEvent event = ChallengeCompletedEvent.fromLetter(savedLetter);
        eventPublisher.publishEvent(event);

        return savedLetter.getId();
    }

    /**
     * 오늘의 편지 수신
     * - 오늘 이미 받았으면 예외 발생
     * - 없으면 새로운 편지 배정 및 생성
     */
    @Transactional
    public LetterReceiveResponseDto receiveTodayLetter() {
        User user = getLoginUser();

        LocalDateTime now = LocalDateTime.now(APP_CLOCK); // 시간 일관성
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(LocalTime.MAX);

        // 오늘 이미 받은 편지가 있는지 확인
        Optional<LetterDelivery> todayDelivery = letterDeliveryRepository
                .findTodayDelivery(user.getId(), startOfDay, endOfDay);

        if (todayDelivery.isPresent()) {
            throw BusinessException.of(ErrorCode.LETTER_ALREADY_RECEIVED_TODAY);
        }

        // 수신 가능한 편지 조회
        List<Long> candidateLetterIds = letterRepository.findCandidateLetterIds(user.getId());

        if (candidateLetterIds.isEmpty()) {
            log.info("수신 가능한 편지 없음: userId={}", user.getId());
            return LetterReceiveResponseDto.builder()
                    .letterId(null)
                    .title(null)
                    .content(null)
                    .deliveredAt(null)
                    .build();
        }

        // 랜덤으로 편지 선택
        Collections.shuffle(candidateLetterIds); // ThreadSafe한 랜덤 선택
        Long selectedLetterId = candidateLetterIds.get(0);

        // Letter 조회
        Letter letter = letterRepository.findById(selectedLetterId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.LETTER_NOT_FOUND));

        // LetterDelivery 생성 및 저장 (첫 배달)
        LetterDelivery letterDelivery = LetterDelivery.builder()
                .receiver(user)
                .letter(letter)
                .deliveredAt(now)
                .storageStatus(StorageStatus.UNSAVED)
                .isRead(false)
                .build();

        letterDeliveryRepository.save(letterDelivery);

        LogUtils.info("편지 배달 완료: userId={}, letterId={}", user.getId(), letter.getId());
        return LetterReceiveResponseDto.builder()
                .letterId(letter.getId())
                .title(letter.getTitle())
                .content(letter.getContent())
                .deliveredAt(now)
                .build();
    }

    /**
     * 편지 보관 (UNSAVED → SAVED)
     */
    @Override
    @Transactional
    public LetterStatusResponseDto saveLetter(Long letterId) {
        User user = getLoginUser();

        LetterDelivery delivery = letterDeliveryRepository
                .findByReceiverIdAndLetterIdAndStorageStatus(user.getId(), letterId, StorageStatus.UNSAVED)
                .orElseThrow(() -> BusinessException.of(ErrorCode.LETTER_NOT_FOUND));

        delivery.changeStorageStatus(StorageStatus.SAVED);

        return LetterStatusResponseDto.builder()
                .letterId(letterId)
                .storageStatus(delivery.getStorageStatus())
                .build();
    }

    /**
     * 편지 삭제 (UNSAVED/SAVED → DELETED)
     */
    @Override
    @Transactional
    public LetterStatusResponseDto deleteLetter(Long letterId) {
        User user = getLoginUser();

        LetterDelivery delivery = letterDeliveryRepository
                .findByReceiverIdAndLetterId(user.getId(), letterId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.LETTER_NOT_FOUND));

        delivery.changeStorageStatus(StorageStatus.DELETED);

        return LetterStatusResponseDto.builder()
                .letterId(letterId)
                .storageStatus(delivery.getStorageStatus())
                .build();
    }

    /**
     * 보관함 편지 목록 조회 (SAVED)
     */
    @Override
    public List<SavedLetterListItemResponseDto> getSavedLetters() {
        User user = getLoginUser();

        return letterDeliveryRepository
                .findByReceiverIdAndStorageStatusWithLetter(
                        user.getId(), StorageStatus.SAVED
                )
                .stream()
                .map(SavedLetterListItemResponseDto::from)
                .toList();
    }

    /**
     * 보관함 편지 상세 조회 (content만)
     */
    @Override
    public SavedLetterDetailResponseDto getSavedLetterDetail(Long letterId) {
        User user = getLoginUser();

        LetterDelivery delivery = letterDeliveryRepository
                .findByReceiverIdAndLetterIdAndStorageStatus(
                        user.getId(), letterId, StorageStatus.SAVED
                )
                .orElseThrow(() -> BusinessException.of(ErrorCode.LETTER_NOT_FOUND));

        return SavedLetterDetailResponseDto.builder()
                .content(delivery.getLetter().getContent())
                .build();
    }

    /**
     * 현재 창문 is_open 상태 조회 (상태 변경 X)
     */
    @Override
    public ReceiveStatusResponseDto getReceiveStatus() {
        User user = getLoginUser(); // 현재 로그인한 13번 유저 정보

        return ReceiveStatusResponseDto.builder()
                .isOpen(user.getIsOpen())
                .build();
    }

    /**
     * 창문 is_open(T/F) 토글
     */
    @Override
    @Transactional
    public ReceiveStatusResponseDto toggleReceiveStatus() {
        User user = getLoginUser();
        user.toggleIsOpen();

        return ReceiveStatusResponseDto.builder()
                .isOpen(user.getIsOpen())
                .build();
    }

    /**
     * 공통: 로그인 유저 조회
     */
    private User getLoginUser() {
        String userCode = UserContextHolder.getUserCode();
        return userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));
    }

    // AI 응답 문자열을 FilterStatus Enum으로 변환 (대소문자 구분 없음)
    private FilterStatus toFilterStatus(String status) {
        if ("PASS".equalsIgnoreCase(status)) {
            return FilterStatus.PASS;
        }
        return FilterStatus.FAIL;
    }

    // AI 응답 파싱 유틸
    private JsonNode parseAiJson(String raw) {
        try {
            String cleaned = raw.trim()
                    .replaceAll("^```json", "")
                    .replaceAll("^```", "")
                    .replaceAll("```$", "")
                    .trim();

            return OBJECT_MAPPER.readTree(cleaned);
        } catch (Exception e) {
            log.warn("AI 응답 JSON 파싱 실패 → FAIL 처리", e);
            return null;
        }
    }

    // 제목 뽑아내기
    private String extractTitle(JsonNode node) {
        if (node == null) return null;

        return node.has("title") && !node.get("title").isNull()
                ? node.get("title").asText()
                : null;
    }

    // 통과/탈락 판정하기
    private FilterStatus extractFilterStatus(JsonNode node) {
        if (node == null) return FilterStatus.FAIL;

        String status = node.has("status") && !node.get("status").isNull()
                ? node.get("status").asText()
                : "FAIL";

        return toFilterStatus(status);
    }
}