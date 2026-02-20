package com.a508.onestep.domain.room.service;

import com.a508.onestep.domain.room.dto.response.RoomKeyResponseDto;
import com.a508.onestep.domain.room.entity.Room;

import java.util.List;

public interface RoomService {

    // room_key 반환
    RoomKeyResponseDto returnRoomKey();

    Room createRoom(Long troubleId, String topic, Integer durationTime);


    void closeRoom(Long roomId);

    List<Room> getActiveRooms();
}
