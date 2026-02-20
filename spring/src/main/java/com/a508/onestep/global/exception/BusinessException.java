package com.a508.onestep.global.exception;

import com.a508.onestep.global.response.BaseCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BaseCode baseCode;

    public BusinessException(BaseCode baseCode) {
        super(baseCode.getMessage());
        this.baseCode = baseCode;
    }

    public static BusinessException of(BaseCode baseCode) {
        return new BusinessException(baseCode);
    }
}