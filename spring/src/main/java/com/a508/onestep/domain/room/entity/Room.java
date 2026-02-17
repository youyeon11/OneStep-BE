package com.a508.onestep.domain.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "room")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Room {

    @Column(name = "id")
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trouble_id")
    private Long troubleId;

    @Column(name = "duration_time")
    private Integer durationTime;

    @Column(name = "topic")
    private String topic;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
    
    /*
    종료 시 업데이트
     */
    public void updateClosedAt() {
        this.closedAt = LocalDateTime.now();
    }
}
