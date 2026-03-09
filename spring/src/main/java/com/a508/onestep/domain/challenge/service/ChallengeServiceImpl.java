package com.a508.onestep.domain.challenge.service;

import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.challenge.repository.ChallengeAssignmentRepository;
import com.a508.onestep.domain.challenge.repository.ChallengeMasterRepository;
import com.a508.onestep.domain.challenge.dto.request.ChallengeCompleteRequestDto;
import com.a508.onestep.domain.challenge.dto.request.ChallengeRequestDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeCompleteResponseDto;
import com.a508.onestep.domain.challenge.dto.response.ChallengeResponseDto;
import com.a508.onestep.domain.challenge.dto.response.InitialChallengeResponseDto;
import com.a508.onestep.domain.common.Origin;
import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.User;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.exception.SemaphoreAcquisitionException;
import com.a508.onestep.global.response.ErrorCode;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Semaphore;


@Service
@RequiredArgsConstructor
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeTransactionHelper transactionHelper;
    private final ChallengeMasterRepository challengeMasterRepository;
    private final ChallengeAssignmentRepository challengeAssignmentRepository;
    private final UserRepository userRepository;
    private final Semaphore databaseSemaphore;

    /*
     * 챌린지(할 일) 등록하기
     */
    @Override
    public ChallengeResponseDto register(ChallengeRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        boolean acquired = false;
        try {
            databaseSemaphore.acquire();
            acquired = true;
            return transactionHelper.register(userCode, requestDto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SemaphoreAcquisitionException.of("DB 접근 대기 중 인터럽트 발생", e);
        } finally {
            if (acquired) databaseSemaphore.release();
        }
    }

    /*
     * 내가 추가한 챌린지(할 일) 조회하기
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChallengeResponseDto> getAll() {
        String userCode = UserContextHolder.getUserCode();

        LocalDate today = LocalDate.now();
        return challengeAssignmentRepository
                .findByUserCodeAndAssignedDateAndOrigin(userCode, today, Origin.SELF)
                .stream()
                .map(ChallengeResponseDto::from)
                .toList();
    }

    /*
     * 챌린지 완료 체크하기
     */
    @Override
    public ChallengeCompleteResponseDto complete(ChallengeCompleteRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        boolean acquired = false;
        try {
            databaseSemaphore.acquire();
            acquired = true;
            return transactionHelper.complete(userCode, requestDto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SemaphoreAcquisitionException.of("DB 접근 대기 중 인터럽트 발생", e);
        } finally {
            if (acquired) databaseSemaphore.release();
        }
    }

    /*
     * 랜덤으로 20개 선정
     */
    @Override
    public List<InitialChallengeResponseDto> getInitialRecommendations() {
        String userCode = UserContextHolder.getUserCode();
        User user = userRepository.findByUserCode(userCode)
                .orElseThrow(() -> BusinessException.of(ErrorCode.USER_NOT_FOUND));

        Integer recoveryLevel = user.getRecoveryLevel();
        List<TagCategory> categories = List.of(TagCategory.values());
        List<ChallengeMaster> challengeMasterList = challengeMasterRepository
                .findRecommendedChallenges(recoveryLevel, categories, user);

        return challengeMasterList.stream()
                .map(InitialChallengeResponseDto::from)
                .toList();
    }

    /*
     * 여러개를 선택해서 저장
     */
    @Override
    public List<ChallengeResponseDto> selectInitialChallenges(List<ChallengeRequestDto> requestDto) {
        String userCode = UserContextHolder.getUserCode();
        boolean acquired = false;
        try {
            databaseSemaphore.acquire();
            acquired = true;
            return transactionHelper.selectInitialChallenges(userCode, requestDto);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SemaphoreAcquisitionException.of("DB 접근 대기 중 인터럽트 발생", e);
        } finally {
            if (acquired) databaseSemaphore.release();
        }
    }

    @Override
    public List<ChallengeResponseDto> getChallenge() {
        String userCode = UserContextHolder.getUserCode();
        boolean acquired = false;
        try {
            databaseSemaphore.acquire();
            acquired = true;
            return transactionHelper.getChallenge(userCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw SemaphoreAcquisitionException.of("DB 접근 대기 중 인터럽트 발생", e);
        } finally {
            if (acquired) databaseSemaphore.release();
        }
    }
}