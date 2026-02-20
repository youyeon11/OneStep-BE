package com.a508.onestep.domain.room.service;

import com.a508.onestep.domain.room.dto.request.TroubleCreateRequestDto;
import com.a508.onestep.domain.room.dto.response.SolutionResponseDto;
import com.a508.onestep.domain.room.entity.Solution;
import com.a508.onestep.domain.room.entity.Trouble;
import com.a508.onestep.domain.room.repository.SolutionRepository;
import com.a508.onestep.domain.room.repository.TroubleRepository;
import com.a508.onestep.domain.user.repository.UserRepository;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TroubleServiceImpl implements TroubleService {

    private final UserRepository userRepository;
    private final TroubleRepository troubleRepository;
    private final SolutionRepository solutionRepository;

    @Override
    @Transactional
    public Long createTrouble(TroubleCreateRequestDto requestDto) {
        String userCode = UserContextHolder.getUserCode();
        validateUserExists(userCode);

        Trouble trouble = Trouble.builder()
                .content(requestDto.getContent())
                .userCode(userCode)
                .build();

        return troubleRepository.save(trouble).getId();
    }

    /*
    Trouble에 달린 Solution 전체 반환하기
     */
    @Override
    public List<SolutionResponseDto> getAllSolution() {
        String userCode = UserContextHolder.getUserCode();
        validateUserExists(userCode);

        List<Solution> solutionList = solutionRepository.findSolutionsByUserCode(userCode);
        return solutionList.stream()
                .map(SolutionResponseDto::from)
                .toList();
    }

    /*
    상세 답변 조회하기
     */
    @Override
    @Transactional
    public SolutionResponseDto getSolution(Long solutionId) {
        String userCode = UserContextHolder.getUserCode();
        validateUserExists(userCode);

        Solution solution = solutionRepository.findById(solutionId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.SOLUTION_NOT_FOUND));
        solution.updateReadAt();
        return SolutionResponseDto.from(solution);
    }

    private void validateUserExists(String userCode) {
        if (!userRepository.existsByUserCode(userCode)) {
            throw BusinessException.of(ErrorCode.USER_NOT_FOUND);
        }
    }
}