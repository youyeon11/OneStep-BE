package com.a508.onestep.domain.room.controller;

import com.a508.onestep.domain.room.dto.response.RoomKeyResponseDto;
import com.a508.onestep.domain.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Room", description = "채팅방 관련 API")
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Tag(name = "Room API Controller", description = "Room 관련 API Controller")
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "랜덤 방 ID 반환하기",
            description = "접속할 랜덤 방 조회")
    @GetMapping("")
    RoomKeyResponseDto returnRoom(
            @RequestHeader("Authorization") String token
    ){
        return roomService.returnRoomKey();
    }
}
