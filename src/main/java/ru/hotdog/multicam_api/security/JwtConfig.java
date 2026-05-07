package ru.hotdog.multicam_api.security;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.hotdog.multicam_api.service.impl.UserDetailsImpl;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtConfig {
    @Value("${app.secret}")
    private String secret;
    @Value("${app.lifetime}")
    private int lifetime;

    public String generateToken(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return Jwts.builder()
                .subject(userDetails.getUsername()) // Вместо setSubject
                .issuedAt(new Date())               // Вместо setIssuedAt
                .expiration(new Date(System.currentTimeMillis() + lifetime)) // Вместо setExpiration
                .signWith(getSigninKey())           // Алгоритм определится автоматически по ключу
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigninKey())         // Вместо setSigningKey
                .build()
                .parseSignedClaims(token)           // Вместо parseClaimsJws
                .getPayload()                       // Вместо getBody
                .getSubject();
    }

    private javax.crypto.SecretKey getSigninKey() {
        // В 0.12.x Keys.hmacShaKeyFor возвращает SecretKey
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
