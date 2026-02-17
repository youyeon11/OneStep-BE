package com.a508.onestep.global.auth.context;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;

public class UserContextHolder {

    private static final ThreadLocal<UserContext> CONTEXT = new ThreadLocal<>();

    /*
    UserContext 설정
     */
    public static void set(UserContext context) {
        CONTEXT.set(context);
    }

    /*
    UserContext 조회
     */
    public static UserContext get() {
        return CONTEXT.get();
    }

    /*
    UserContext 현재 사용자 코드 조회
     */
    public static String getUserCode() {
        UserContext context = get();
        if (context == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return context.getUserCode();
    }

    public static String getRole() {
        UserContext context = get();
        if (context == null) {
            throw BusinessException.of(ErrorCode.UNAUTHORIZED);
        }
        return context.getRole();
    }

    /*
    인증 여부 확인
     */
    public static boolean isAuthenticated() {
        return CONTEXT.get() != null;
    }


    public static void clear() {
        CONTEXT.remove();
    }
}
