package com.a508.onestep.domain.route.event;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageSaveEvent {
    private Long routeSessionId;
    private String imageUrl;
}
