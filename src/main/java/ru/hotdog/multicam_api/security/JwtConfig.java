package ru.hotdog.multicam_api.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import ru.hotdog.multicam_api.entity.UserEntity;
import ru.hotdog.multicam_api.service.impl.UserDetailsImpl;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
// Класс для создания и чтения JWT токенов.
public class JwtConfig {
    @Value("${app.secret}")
    // Секретный ключ для подписи токена.
    private String secret;

    @Value("${app.lifetime}")
    // Время жизни access token.
    private int lifetime;

    // Создает токен из данных авторизации.
    public String generateToken(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenForUser(userDetails.getEmail(), userDetails.getId());
    }

    // Создает токен для почты и id пользователя.
    public String generateTokenForUser(String email, Long userId) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + lifetime))
                .signWith(getSigningKey())
                .compact();
    }

    // Создает токен для объекта пользователя.
    public String generateTokenForUser(UserEntity user) {
        return generateTokenForUser(user.getEmail(), user.getId());
    }

    // Достает почту пользователя из токена.
    public String getUsernameFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Достает email из токена.
    public String getEmailFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Создает ключ для подписи JWT.
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
