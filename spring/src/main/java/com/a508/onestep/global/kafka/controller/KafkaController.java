package com.a508.onestep.global.kafka.controller;

import com.a508.onestep.global.kafka.dto.UserFeatureSet;
import com.a508.onestep.global.kafka.service.KafkaService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/kafka")
@RequiredArgsConstructor
@Tag(name = "Kafka Controller", description = "kafka 테스트용 컨트롤러")
public class KafkaController {

    private final KafkaService kafkaService;

    @PostMapping("/send")
    void sendTest(@RequestBody UserFeatureSet dto) {
        kafkaService.sendUserFeatureSet(dto);
    }

}
