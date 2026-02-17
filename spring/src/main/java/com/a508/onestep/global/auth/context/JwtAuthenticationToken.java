package com.a508.onestep.global.auth.context;

import lombok.Getter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

/*
JWT 정보 기반 Authentication 객체
*/
@Getter
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String userCode;
    private final String role;

    public JwtAuthenticationToken(String userCode, String role) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        this.userCode = userCode;
        this.role = role;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userCode;
    }
}
