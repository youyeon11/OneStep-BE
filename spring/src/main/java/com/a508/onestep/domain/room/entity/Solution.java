package com.a508.onestep.domain.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "solution")
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Solution {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "trouble_id")
    private Long troubleId;

    @Column(name = "summary")
    private String summary;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 읽기 처리
    public Solution updateReadAt() {
        this.readAt = LocalDateTime.now();
        return this;
    }
}
