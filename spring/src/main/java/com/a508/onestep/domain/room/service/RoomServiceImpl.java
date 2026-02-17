package com.a508.onestep.domain.room.service;

import com.a508.onestep.domain.room.dto.response.RoomKeyResponseDto;
import com.a508.onestep.domain.room.entity.Room;
import com.a508.onestep.domain.room.entity.Trouble;
import com.a508.onestep.domain.room.repository.RoomRepository;
import com.a508.onestep.domain.room.repository.TroubleRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.websocket.service.RoomSessionManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final TroubleRepository troubleRepository;
    private final RoomSessionManager roomSessionManager;

    @Transactional
    public RoomKeyResponseDto returnRoomKey() {
        String userCode = UserContextHolder.getUserCode();

        List<Room> roomList = roomRepository.findByTroubleIdIsNotEqualUserCodeANDCLOSEDATISNULL(userCode);

        // 가득 찬 방 제외
        List<Room> availableRooms = roomList.stream()
                .filter(room -> roomSessionManager.getRoomSession(room.getId())
                        .map(session -> !session.isFull())
                        .orElse(true))
                .toList();

        Random random = new Random();
        int roomListSize = availableRooms.size();

        Room choicedRoom = null;

        if (roomListSize > 0) {
            // 입장 가능한 방이 있다면
            int randomIndex = random.nextInt(roomListSize);

            choicedRoom = availableRooms.get(randomIndex);
        } else {
            // 룸이 없다면
            Integer durationTime = 1; // 1분으로 설정
            Trouble trouble = troubleRepository.findRandomAndNotEqualUserCode(userCode)
                    .orElseThrow( () -> BusinessException.of(ErrorCode.TROUBLE_NOT_FOUND));

            choicedRoom = createRoom(trouble.getId(), trouble.getContent(), durationTime);
        }

        RoomKeyResponseDto roomKeyResponseDto = RoomKeyResponseDto.builder()
                .roomId(choicedRoom.getId())
                .troubleId(choicedRoom.getTroubleId())
                .build();

        return roomKeyResponseDto;
    }

    /**
     * 방 생성
     */
    @Transactional
    public Room createRoom(Long troubleId, String topic, Integer durationTime) {
        Room room = Room.builder()
                .troubleId(troubleId)
                .topic(topic)
                .durationTime(durationTime != null ? durationTime : 30)
                .closedAt(null)
                .build();

        return roomRepository.save(room);
    }

    /**
     * 방 종료
     */
    @Transactional
    public void closeRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ROOM_NOT_FOUND));
        room.updateClosedAt();
        roomRepository.save(room);
    }

    /**
     * 활성화된 방 목록 조회
     */
    public List<Room> getActiveRooms() {
        return roomRepository.findActiveRooms(LocalDateTime.now());
    }
}
