package com.a508.onestep.global.websocket.service;

import com.a508.onestep.domain.common.MessageRoleType;
import com.a508.onestep.domain.room.entity.Room;
import com.a508.onestep.domain.room.repository.RoomRepository;
import com.a508.onestep.domain.room.service.RoomService;
import com.a508.onestep.global.client.genai.GenAiClient;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.logging.utils.LogUtils;
import com.a508.onestep.global.message.MessageTemplate;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.websocket.dto.RoomParticipant;
import com.a508.onestep.global.websocket.dto.RoomSession;
import com.a508.onestep.global.websocket.dto.request.ChatMessageRequestDto;
import com.a508.onestep.global.websocket.dto.response.ChatMessageResponseDto;
import com.a508.onestep.global.websocket.dto.response.SummaryResponseDto;
import com.a508.onestep.global.websocket.event.RoomEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import com.a508.onestep.global.client.genai.event.GenAiSummaryEvent;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@RequiredArgsConstructor
@Service
public class WebSocketServiceImpl implements WebSocketService {

    private final MessageTemplate messageTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    private final RoomRepository roomRepository;
    private final RoomSessionManager roomSessionManager;
    private final RoomService roomService;

    private final GenAiClient genAiClient;

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledFutureMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> roomDurationMap = new ConcurrentHashMap<>();

    /**
     * Socket 연결
     */
    public void connect() {
        String userCode = UserContextHolder.getUserCode();
        LogUtils.info("User {} connected", userCode);
    }

    /**
     * 방 입장
     */
    @Transactional
    public void enterRoom(Long roomId) {
        String userCode = UserContextHolder.getUserCode();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROOM_NOT_FOUND));

        // Redis로부터 방세션 초기화 및 조회
        RoomSession roomSession = roomSessionManager.getRoomSession(roomId)
                .orElseGet(() -> initializeRoomSession(room));
        if (roomSession.isExpired()) {
            roomSessionManager.deleteRoomSession(roomId);
            throw BusinessException.of(ErrorCode.ROOM_EXPIRED);
        }

        // 세션 만료 확인
        if (roomSession.isFull()) {
            throw BusinessException.of(ErrorCode.ROOM_FULL);
        }

        // 중복 입장 유저 확인
        if (roomSessionManager.isParticipant(roomId, userCode)) {
            throw BusinessException.of(ErrorCode.ALREADY_ENTERED);
        }

        RoomParticipant newParticipant = RoomParticipant.builder()
                .userCode(userCode)
                .enteredAt(LocalDateTime.now())
                .build();
        roomSession = roomSessionManager.addParticipant(roomId, newParticipant);

        // 입장 이벤트 발행
        applicationEventPublisher.publishEvent(RoomEvent.enter(userCode, roomId));

        // 사용자 입장 응답 저장 및 전송
        LocalDateTime now = LocalDateTime.now();
        long remainingMillis = ChronoUnit.MILLIS.between(now, roomSession.getSessionExpiresAt());

        String enterMessage = newParticipant.getUserCode() + "님이 입장하셨습니다.";

        ChatMessageResponseDto enterResponse = ChatMessageResponseDto.builder()
                .roomId(roomId)
                .topic(room.getTopic())
                .senderCode(null)
                .content(enterMessage)
                .timestamp(now)
                .messageRoleType(MessageRoleType.ENTER)
                .durationTime(room.getDurationTime())
                .participants(new ArrayList<>(roomSession.getParticipants()))
                .expiresAt(roomSession.getSessionExpiresAt())
                .remainingTime(Math.max(0L, remainingMillis))
                .currentCount(roomSession.getCurrentCount())
                .build();
        messageTemplate.send("/user/" + userCode + "/queue/room/enter", enterResponse);

        // 방의 모든 참여자에게 알림
        messageTemplate.send("/topic/room/" + roomId + "/messages", enterResponse);

        LogUtils.info("User {} entered room {} ({}/4)", userCode, roomId, roomSession.getCurrentCount());
    }

    /**
     * 방 퇴장
     */
    @Transactional
    public void exitRoom(Long roomId) {
        String userCode = UserContextHolder.getUserCode();
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROOM_NOT_FOUND));

        // 참여자 확인
        if (!roomSessionManager.isParticipant(roomId, userCode)) {
            throw BusinessException.of(ErrorCode.NOT_PARTICIPANT);
        }

        // Redis에서 참여자 제거
        RoomSession updatedSession = roomSessionManager.removeParticipant(roomId, userCode);

        // 퇴장 이벤트 발행
        applicationEventPublisher.publishEvent(RoomEvent.exit(userCode, roomId));

        // 방의 모든 참여자에게 알림
        if (updatedSession != null) {
            LocalDateTime now = LocalDateTime.now();
            long remainingMillis = ChronoUnit.MILLIS.between(now, updatedSession.getSessionExpiresAt());

            String exitMessage = userCode + "님이 퇴장하셨습니다.";

            ChatMessageResponseDto exitResponse = ChatMessageResponseDto.builder()
                    .roomId(roomId)
                    .topic(room.getTopic())
                    .senderCode(null)
                    .content(exitMessage)
                    .timestamp(now)
                    .messageRoleType(MessageRoleType.EXIT)
                    .participants(new ArrayList<>(updatedSession.getParticipants()))
                    .expiresAt(updatedSession.getSessionExpiresAt())
                    .remainingTime(Math.max(0L, remainingMillis))
                    .currentCount(updatedSession.getCurrentCount())
                    .build();

            messageTemplate.send("/topic/room/" + roomId + "/messages", exitResponse);
        } else {
            LogUtils.info("Last participant left room {}", roomId);
        }
        LogUtils.info("User {} exited room {} (remaining: {})",
                userCode, roomId, updatedSession != null ? updatedSession.getCurrentCount() : 0);
    }

    /**
     * 메시지 전송
     */
    @Transactional
    public void sendMessage(Long roomId, ChatMessageRequestDto request) {
        String userCode = UserContextHolder.getUserCode();
        if (!roomSessionManager.isParticipant(roomId, userCode)) {
            throw BusinessException.of(ErrorCode.NOT_PARTICIPANT);
        }

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROOM_NOT_FOUND));
        RoomSession roomSession = roomSessionManager.getRoomSession(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROOM_NOT_FOUND));

        String formatted = userCode + ": " + request.getContent();
        roomSessionManager.addMessage(roomId, formatted);

        LocalDateTime now = LocalDateTime.now();
        long remainingMillis = ChronoUnit.MILLIS.between(now, roomSession.getSessionExpiresAt());

        ChatMessageResponseDto response = ChatMessageResponseDto.builder()
                .roomId(roomId)
                .topic(room.getTopic())
                .senderCode(userCode)
                .content(request.getContent())
                .messageRoleType(MessageRoleType.TEXT)
                .timestamp(now)
                .durationTime(room.getDurationTime())
                .participants(new ArrayList<>(roomSession.getParticipants()))
                .expiresAt(roomSession.getSessionExpiresAt())
                .remainingTime(Math.max(0L, remainingMillis))
                .currentCount(roomSession.getCurrentCount())
                .build();

        messageTemplate.send("/topic/room/" + roomId + "/messages", response);

        LogUtils.info("User {} sent message to room {}", userCode, roomId);
    }

    /**
     * 방 세션 초기화
     */
    private RoomSession initializeRoomSession(Room room) {
        LocalDateTime now = LocalDateTime.now();
        RoomSession roomSession = RoomSession.builder()
                .roomId(room.getId())
                .participants(new HashSet<>())
                .sessionStartedAt(now)
                .sessionExpiresAt(now.plusMinutes(room.getDurationTime()))
                .build();

        roomSessionManager.saveRoomSession(roomSession);
        startRoomTimer(room.getId(), room.getDurationTime());
        LogUtils.info("Initialized room session for room {}", room.getId());
        return roomSession;
    }

    /**
     * 타이머 시작
     */
    private void startRoomTimer(Long roomId, Integer durationMinutes) {
        if (scheduledFutureMap.containsKey(roomId)) return;

        roomDurationMap.put(roomId, durationMinutes);

        // 종료 30초 전 알림 스케줄링
        long delayToNotify = durationMinutes * 60L * 1000L - 30L * 1000L;
        if (delayToNotify > 0) {
            ScheduledFuture<?> notifyTask = taskScheduler.schedule(
                    () -> sendTimerEvent(roomId, "CLOSING_SOON"),
                    new Date(System.currentTimeMillis() + delayToNotify));
            scheduledFutureMap.put(roomId, notifyTask);
            LogUtils.info("방 {}의 종료가 30초 남음", roomId);
        }

        // 종료
        long delayToClose = durationMinutes * 60L * 1000L;
        taskScheduler.schedule(
                () -> sendTimerEvent(roomId, "CLOSED"),
                new Date(System.currentTimeMillis() + delayToClose));
        LogUtils.info("방 {}가 {}분이 되어 종료됩니다.", roomId, durationMinutes);
    }

    /**
     * 타이머 이벤트 전송 및 DB 저장
     * @param eventType 타이머에 대한 이벤트 타입 (CLOSING_SOON, CLOSED)
     */
    private void sendTimerEvent(Long roomId, String eventType) {
        Room room = roomRepository.findById(roomId).orElse(null);
        RoomSession roomSession = roomSessionManager.getRoomSession(roomId).orElse(null);

        if (room == null || roomSession == null) {
            LogUtils.warn("Room or session not found for timer event: roomId={}", roomId);
            return;
        }

        boolean isClosingSoon = "CLOSING_SOON".equals(eventType);
        String message = isClosingSoon
                ? "대화 종료 30초 전입니다. 최종 결론을 도출해 주세요."
                : "대화 시간이 종료되었습니다. 잠시 후 요약이 시작됩니다.";

        LocalDateTime now = LocalDateTime.now();
        long remainingMillis = ChronoUnit.MILLIS.between(now, roomSession.getSessionExpiresAt());

        roomSessionManager.addMessage(roomId, "SYSTEM: " + message);

        ChatMessageResponseDto response = ChatMessageResponseDto.builder()
                .roomId(roomId)
                .topic(room.getTopic())
                .senderCode(null)
                .content(message)
                .timestamp(now)
                .messageRoleType(MessageRoleType.TIMER)
                .durationTime(room.getDurationTime())
                .participants(new ArrayList<>(roomSession.getParticipants()))
                .expiresAt(roomSession.getSessionExpiresAt())
                .remainingTime(Math.max(0L, remainingMillis))
                .currentCount(roomSession.getCurrentCount())
                .build();

        messageTemplate.send("/topic/room/" + roomId + "/messages", response);
        LogUtils.info("Sent timer event {} for room {}", eventType, roomId);

        // 방 종료 시 DB 업데이트 및 요약 전송
        if (!isClosingSoon) {
            roomService.closeRoom(roomId);
            sendSummary(roomId, room);
        }
    }

    /**
     * 요약 정보 전송 및 Solution 저장 이벤트 발행
     */
    private void sendSummary(Long roomId, Room room) {
        List<String> messages = roomSessionManager.getMessages(roomId);

        if (messages.isEmpty()) {
            LogUtils.warn("No messages found for room {}", roomId);
            return;
        }

        genAiClient.summarizeFormattedMessages(messages, room.getTopic())
                .thenAccept(summary -> {
                    SummaryResponseDto summaryResponse = SummaryResponseDto.builder()
                            .roomId(roomId)
                            .troubleId(room.getTroubleId())
                            .summary(summary)
                            .timestamp(LocalDateTime.now())
                            .build();

                    messageTemplate.send("/topic/room/" + roomId + "/summary", summaryResponse);
                    LogUtils.info("Sent summary for room {}", roomId);

                    roomSessionManager.deleteMessages(roomId);

                    GenAiSummaryEvent event = GenAiSummaryEvent.builder()
                            .roomId(roomId)
                            .troubleId(room.getTroubleId())
                            .summary(summary)
                            .topic(room.getTopic())
                            .build();
                    applicationEventPublisher.publishEvent(event);
                })
                .exceptionally(ex -> {
                    LogUtils.error("Failed to summarize messages for room {}: {}", roomId, ex.getMessage());
                    return null;
                });
    }
}
