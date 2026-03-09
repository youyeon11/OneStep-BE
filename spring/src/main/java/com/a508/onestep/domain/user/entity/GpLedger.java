package com.a508.onestep.domain.user.entity;

import com.a508.onestep.domain.common.BaseTimeEntity;
import com.a508.onestep.domain.common.GpReason;
import com.a508.onestep.domain.common.Origin;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "gp_ledgers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GpLedger extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GpReason reason;

    @Column(name = "ref_type")
    @Enumerated(EnumType.STRING)
    private Origin refType; // 챌린지 할당의 출처

    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "user_code")
    private String userCode;

    /*
    GpLedger 생성자
     */
    public static GpLedger challengeCompleteReward(String userCode, Long assignmentId, Origin refType) {
        GpLedger ledger = new GpLedger();
        ledger.reason = GpReason.CHALLENGE_COMPLETE_REWARD;
        ledger.refType = refType;
        ledger.refId = assignmentId;
        ledger.userCode = userCode;
        return ledger;
    }
}
