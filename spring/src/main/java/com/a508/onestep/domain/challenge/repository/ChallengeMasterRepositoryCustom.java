package com.a508.onestep.domain.challenge.repository;

import com.a508.onestep.domain.challenge.entity.ChallengeMaster;
import com.a508.onestep.domain.common.TagCategory;
import com.a508.onestep.domain.user.entity.User;

import java.util.List;

public interface ChallengeMasterRepositoryCustom {

    List<ChallengeMaster> findRecommendedChallenges(
            Integer difficultyLevel,
            List<TagCategory> categories,
            User user
    );
}
