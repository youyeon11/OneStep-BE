package com.a508.onestep.global.logging.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;

@Component
@Slf4j
public class LogUtils {

    /*
    Exception Handler
     */
    public static String exception(Throwable e) {
        return e == null ? "" : e.getMessage();
    }

    public static String exceptionWithCause(Throwable e) {
        if (e == null) return "";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        Throwable cause = e.getCause();
        while (cause != null) {
            pw.println("Caused by:");
            cause.printStackTrace(pw);
            cause = cause.getCause();
        }
        return sw.toString();
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
        log.error(">>> Exception : {}", exceptionWithCause(e), e);
    }
}
