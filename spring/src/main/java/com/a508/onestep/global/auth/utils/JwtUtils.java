package com.a508.onestep.global.auth.utils;

import com.a508.onestep.global.exception.BusinessException;
import com.a508.onestep.global.response.ErrorCode;
import com.a508.onestep.global.auth.context.UserContext;
import com.a508.onestep.global.logging.utils.LogUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    @Value("${spring.jwt.access.expiration}")
    private long accessTokenExpirationTime;

    @Value("${spring.jwt.refresh.expiration}")
    private long refreshTokenExpirationTime;

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";
    private static final String ROLE_CLAIM = "role";

    private Key key;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 토큰 생성
     * @param userCode : UUID 기반의 UserCode
     * @param role : 회원의 role
     * @param expirationTime : 토큰 만료 기한(Access, Refresh)
     * @param isRefreshToken : Refresh Token 여부
     * @return
     */
    public String generateToken(String userCode, String role, long expirationTime, boolean isRefreshToken) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime * 1000);

        return Jwts.builder()
                .setHeaderParam("type", isRefreshToken ? "refresh" : "access")
                .setHeaderParam("algorithm", "HS256")
                .setSubject(userCode)
                .claim(ROLE_CLAIM, role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /*
    AccessToken 생성
     */
    public String generateAccessToken(String userCode, String role) {
        return generateToken(userCode, role, accessTokenExpirationTime, false);
    }

    /*
    RefreshToken 생성
     */
    public String generateRefreshToken(String userCode, String role) {
        return generateToken(userCode, role, refreshTokenExpirationTime, true);
    }

    /*
    Authorization 헤더에서 토큰 추출(Bearer 시작 시 토큰만 추출)
     */
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        return null;
    }

    /*
     * 토큰의 유효성 검증
     */
    public boolean validateToken(String token) throws Exception {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            LogUtils.warn("유효하지 않은 JWT token : {}", e.getMessage());
            throw BusinessException.of(ErrorCode.TOKEN_INVALID);
        } catch (SignatureException e) {
            LogUtils.warn("유효하지 않은 서명의 JWT token: {}", e.getMessage());
            throw BusinessException.of(ErrorCode.TOKEN_SIGNATURE_INVALID);
        } catch (ExpiredJwtException e) {
            LogUtils.warn("만료된 JWT token : {}", e.getMessage());
            throw BusinessException.of(ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            LogUtils.warn("지원하지 않는 JWT token: {}", e.getMessage());
            throw BusinessException.of(ErrorCode.TOKEN_UNSUPPORTED);
        } catch (IllegalArgumentException e) {
            LogUtils.warn("비어있는 JWT token : {}", e.getMessage());
            throw BusinessException.of(ErrorCode.TOKEN_NOT_FOUND);
        }
    }

    /**
     * 토큰에서 Claims 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 토큰에서 UserCode 추출
     */
    public String getUserCode(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰에서 Role 추출
     */
    public String getRole(String token) {
        return getClaims(token).get(ROLE_CLAIM, String.class);
    }

    /**
     * Request에서 UserContext 추출
     */
    public UserContext extractUserContext(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null) {
            return null;
        }

        try {
            Claims claims = getClaims(token);
            String userCode = claims.getSubject();
            String role = claims.get(ROLE_CLAIM, String.class);

            return new UserContext(userCode, role);
        } catch (Exception e) {
            LogUtils.warn("UserContext 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 토큰 만료 시간 추출
     */
    public Date getExpirationDate(String token) {
        return getClaims(token).getExpiration();
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = getExpirationDate(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}