package com.a508.onestep.global.logging.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class LogUtils {

    /*
    Exception Handler
     */
    public static String exception(Throwable e) {
        return e == null ? "" : e.getMessage();
    }

    /*
    Basic Logging
     */
    // DEBUG LOG
    public static void debug(String msg, Object... args) {
        if (log.isDebugEnabled()) {
            log.debug(">>> [DEBUG] " + msg, args);
        }
    }

    // INFO LOG
    public static void info(String msg, Object... args) {
        if (log.isInfoEnabled()) {
            log.info(">>> [INFO] " + msg, args);
        }
    }

    // WARN LOG
    public static void warn(String msg, Object... args) {
        if (log.isWarnEnabled()) {
            log.warn(">>> [WARN] " + msg, args);
        }
    }

    // ERROR LOG
    public static void error(String msg, Object... args) {
        if (log.isErrorEnabled()) {
            log.error(">>> [ERROR] " + msg, args);
        }
    }

    // Request ERROR LOG
    public static void error(Throwable e, HttpServletRequest request) {
        if (request == null) {
            log.error(">>> Exception : {}", exception(e), e);
            return;
        }
        log.error(">>> Request URI : [{}] {}", request.getMethod(), request.getRequestURI());
        log.error(">>> Exception : {}", exception(e), e);
    }
}
