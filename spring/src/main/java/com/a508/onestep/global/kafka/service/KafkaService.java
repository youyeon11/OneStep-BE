package com.a508.onestep.global.kafka.service;

import com.a508.onestep.global.kafka.dto.UserFeatureSet;

public interface KafkaService {

    void sendUserFeatureSet(UserFeatureSet featureSet);

}