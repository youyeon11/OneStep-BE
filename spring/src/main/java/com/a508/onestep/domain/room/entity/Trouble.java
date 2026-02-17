package com.a508.onestep.domain.room.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trouble")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Trouble {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "content")
    private String content;

    @Column(name = "user_code")
    private String userCode;
}
