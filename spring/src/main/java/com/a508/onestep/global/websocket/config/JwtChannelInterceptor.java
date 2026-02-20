package com.a508.onestep.global.websocket.config;

import com.a508.onestep.global.auth.context.UserContext;
import com.a508.onestep.global.auth.context.UserContextHolder;
import com.a508.onestep.global.auth.utils.JwtUtils;
import com.a508.onestep.global.logging.utils.LogUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ExecutorChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/*
WebSocket 인터셉터에 적용할 JWT Interceptor
 */
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ExecutorChannelInterceptor {

    private final JwtUtils jwtUtils;

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String USER_CONTEXT_ATTR = "userContext";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // CONNECT 검증 시작 -> 세션 저장
        if (StompCommand.CONNECT.equals(command)) {
            String token = extractToken(accessor);

            if (token != null) {
                try {
                    if (jwtUtils.validateToken(token)) {
                        String userCode = jwtUtils.getUserCode(token);
                        String role = jwtUtils.getRole(token);

                        UserContext userContext = new UserContext(userCode, role);
                        // 세션 저장
                        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
                        if (sessionAttributes != null) {
                            sessionAttributes.put(USER_CONTEXT_ATTR, userContext);
                        }
                        accessor.setUser(() -> userCode);

                        LogUtils.info("[WebSocket] CONNECT - JWT validated for user: {}", userCode);
                    }
                } catch (Exception e) {
                    LogUtils.warn("[WebSocket] CONNECT - JWT validation failed: {}", e.getMessage());
                }
            } else {
                LogUtils.warn("[WebSocket] CONNECT - No JWT token found in STOMP headers");
            }
        }
        return message;
    }

    /**
     * ThreadLocal UserContext 설정함으로써 Controller와 동일 스레드 사용
     */
    @Override
    public Message<?> beforeHandle(Message<?> message, MessageChannel channel, org.springframework.messaging.MessageHandler handler) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null) {
                UserContext userContext = (UserContext) sessionAttributes.get(USER_CONTEXT_ATTR);
                if (userContext != null) {
                    UserContextHolder.set(userContext);
                    LogUtils.debug("[WebSocket] beforeHandle - Set UserContext for user: {}", userContext.getUserCode());
                }
            }
        }

        return message;
    }

    /**
     * ThreadLocal 정리
     */
    @Override
    public void afterMessageHandled(Message<?> message, MessageChannel channel, org.springframework.messaging.MessageHandler handler, Exception ex) {
        UserContextHolder.clear();
        LogUtils.debug("[WebSocket] afterMessageHandled - Cleared UserContext");
    }


    /*
    헤더에서 토큰 추출
     */
    private String extractToken(StompHeaderAccessor accessor) {
        String bearerToken = accessor.getFirstNativeHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }

        return null;
    }
}
