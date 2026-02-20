package com.a508.onestep.domain.letter.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "letter_deliveries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LetterDelivery extends BaseTimeEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Column(name = "delivered_at")
        private LocalDateTime deliveredAt;

        @Enumerated(EnumType.STRING)
        @Column(name = "storage_status", nullable = false)
        private StorageStatus storageStatus;

        @Column(name = "is_read")
        private Boolean isRead;

        @Column(name = "read_at")
        private LocalDateTime readAt;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "receiver_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private User receiver;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "letter_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
        private Letter letter;

        public void changeStorageStatus(StorageStatus storageStatus) {

                this.storageStatus = storageStatus;
        }
}
