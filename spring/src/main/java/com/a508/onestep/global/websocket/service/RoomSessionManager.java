package com.a508.onestep.global.websocket.service;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.websocket.dto.RoomParticipant;
import com.a508.onestep.global.websocket.dto.RoomSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/*
RoomSessionManager 관리
Redis의 RoomSession
 */
@Component
public class RoomSessionManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private static final String ROOM_SESSION_KEY = "room:session:";
    private static final String ROOM_MESSAGES_KEY = "room:messages:";
    private static final int SESSION_TTL = 60;
    private final ObjectMapper objectMapper;

    public RoomSessionManager(@Qualifier("redisObjectTemplate") RedisTemplate<String, Object> redisTemplate,
                              RedisTemplate<String, String> stringRedisTemplate,
                              ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 방 세션 조회
     */
    public Optional<RoomSession> getRoomSession(Long roomId) {
        String key = ROOM_SESSION_KEY + roomId;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(objectMapper.convertValue(value, RoomSession.class));
    }

    /**
     * 방 세션 저장
     */
    public void saveRoomSession(RoomSession roomSession) {
        String key = ROOM_SESSION_KEY + roomSession.getRoomId();
        redisTemplate.opsForValue().set(key, roomSession, SESSION_TTL, TimeUnit.MINUTES);
    }

    /**
     * 방 세션 삭제
     */
    public void deleteRoomSession(Long roomId) {
        String key = ROOM_SESSION_KEY + roomId;
        redisTemplate.delete(key);
    }

    /**
     * 참여자 추가
     */
    public RoomSession addParticipant(Long roomId, RoomParticipant participant) {
        RoomSession roomSession = getRoomSession(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND));

        roomSession.getParticipants().add(participant);
        saveRoomSession(roomSession);
        return roomSession;
    }

    /**
     * 참여자 제거
     */
    public RoomSession removeParticipant(Long roomId, String userCode) {
        RoomSession roomSession = getRoomSession(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROUTE_SESSION_NOT_FOUND));
        roomSession.getParticipants().removeIf(p -> p.getUserCode().equals(userCode));

        saveRoomSession(roomSession);
        return roomSession;
    }

    /**
     * 특정 사용자가 방에 참여 중인지 확인
     */
    public boolean isParticipant(Long roomId, String userCode) {
        return getRoomSession(roomId)
                .map(session -> session.getParticipants().stream()
                        .anyMatch(p -> p.getUserCode().equals(userCode)))
                .orElse(false);
    }

    /**
     * 채팅 메시지를 Redis List에 추가
     */
    public void addMessage(Long roomId, String formattedMessage) {
        String key = ROOM_MESSAGES_KEY + roomId;
        stringRedisTemplate.opsForList().rightPush(key, formattedMessage);
        stringRedisTemplate.expire(key, SESSION_TTL, TimeUnit.MINUTES);
    }

    /**
     * Redis List에서 모든 채팅 메시지 조회
     */
    public List<String> getMessages(Long roomId) {
        String key = ROOM_MESSAGES_KEY + roomId;
        List<String> messages = stringRedisTemplate.opsForList().range(key, 0, -1);
        return messages != null ? messages : Collections.emptyList();
    }

    /**
     * Redis List에서 채팅 메시지 삭제
     */
    public void deleteMessages(Long roomId) {
        String key = ROOM_MESSAGES_KEY + roomId;
        stringRedisTemplate.delete(key);
    }
}
