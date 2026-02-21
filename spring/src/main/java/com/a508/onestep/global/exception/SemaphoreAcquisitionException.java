package com.a508.onestep.global.exception;

import com.a508.onestep.global.response.BaseCode;

public class SemaphoreAcquisitionException extends RuntimeException {

    public SemaphoreAcquisitionException(BaseCode baseCode) {
        super(baseCode.getMessage());
    }

    public static SemaphoreAcquisitionException of(BaseCode baseCode) {
        return new SemaphoreAcquisitionException(baseCode);
    }

    public static SemaphoreAcquisitionException of(String message, Throwable cause) {
        return new SemaphoreAcquisitionException(message, cause);
    }

    private SemaphoreAcquisitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
