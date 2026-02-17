package com.a508.onestep.domain.challenge.listener;

import com.a508.onestep.domain.challenge.event.ChallengeCompletedEvent;
import com.a508.onestep.domain.pet.entity.PetOwnership;
import com.a508.onestep.domain.pet.repository.PetOwnershipRepository;
import com.a508.onestep.domain.pet.util.PetLevelCalculator;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChallengeCompletedEventListener {

    private final PetOwnershipRepository petOwnershipRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleChallengeCompleted(ChallengeCompletedEvent event) {
        LogUtils.info("Updating pet exp for userId: {}", event.getUser().getId());

        User user = event.getUser();
        Integer exp = event.getEarnedExp();
        user.updateTotalExp(exp);

        PetOwnership petOwnership = petOwnershipRepository.findByUserId(user.getId())
                .orElseThrow(() -> BusinessException.of(ErrorCode.PET_NOT_FOUND));

        LogUtils.info("회원 원래 포인트: {}", user.getTotalExp());
        LogUtils.info("회원에게 추가된 포인트: {}", event.getEarnedExp());

        int point = user.getTotalExp();
        int newLevel = PetLevelCalculator.getPetLevel(point);
        int currentExp = PetLevelCalculator.getCurrentExpInLevel(point, newLevel);
        int maxExp = PetLevelCalculator.getMaxExpForLevel(newLevel);

        petOwnership.updateExpInfo(newLevel, currentExp, maxExp);
    }
}
