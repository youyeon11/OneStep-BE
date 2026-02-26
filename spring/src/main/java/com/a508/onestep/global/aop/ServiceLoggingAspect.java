package com.a508.onestep.global.aop;

import com.a508.onestep.global.logging.utils.LogUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ServiceLoggingAspect {

    private final ObservationRegistry observationRegistry;

    public ServiceLoggingAspect(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @Around("within(@org.springframework.stereotype.Service *)")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String method = joinPoint.getSignature().toShortString();
        Observation observation = Observation.createNotStarted(method, observationRegistry).start();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            LogUtils.info("method={} duration={}ms status=OK", method, System.currentTimeMillis() - start);
            return result;
        } catch (Throwable e) {
            LogUtils.error("method={} duration={}ms status=FAIL error={}",
                    method, System.currentTimeMillis() - start, e.getMessage());
            observation.error(e);
            throw e;
        } finally {
            observation.stop();
        }
    }
}
