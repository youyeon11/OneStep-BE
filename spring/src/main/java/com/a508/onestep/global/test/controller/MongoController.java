package com.a508.onestep.global.test.controller;

import com.a508.onestep.domain.challenge.entity.RecommendedRoutine;
import com.a508.onestep.domain.challenge.repository.RecommendedRoutineRepository;
import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/mongo")
@RequiredArgsConstructor
@Tag(name = "mongodb Controller", description = "mongodb 테스트용 컨트롤러")
public class MongoController {

    private final RecommendedRoutineRepository recommendedRoutineRepository;

    @GetMapping("")
    public RecommendedRoutine test(){
        return recommendedRoutineRepository.findByUserCode("U0001")
                .orElseThrow(() ->BusinessException.of(ErrorCode.RECOMMENDATION_NOT_FOUND));
    }

}
