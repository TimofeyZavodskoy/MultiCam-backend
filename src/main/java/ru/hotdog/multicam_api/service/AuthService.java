package ru.hotdog.multicam_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.RefreshRequest;
import ru.hotdog.multicam_api.dto.Signin;
import ru.hotdog.multicam_api.dto.Signup;
import ru.hotdog.multicam_api.dto.TokenPair;
import ru.hotdog.multicam_api.entity.RefreshTokenEntity;
import ru.hotdog.multicam_api.entity.UserEntity;
import ru.hotdog.multicam_api.repository.RefreshTokenRepo;
import ru.hotdog.multicam_api.repository.UserRepo;
import ru.hotdog.multicam_api.security.JwtConfig;
import ru.hotdog.multicam_api.service.impl.UserDetailsImpl;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    // ── Регистрация ───────────────────────────────────────────────────────────

    public Mono<String> registerUser(Signup signupRequest) {
        return userRepo.existsByEmail(signupRequest.getEmail())
                .flatMap(exists -> {
                    if (exists) return Mono.error(new RuntimeException("Почта уже занята"));
                    UserEntity user = new UserEntity();
                    user.setName(signupRequest.getName());
                    user.setEmail(signupRequest.getEmail());
                    user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
                    return userRepo.save(user);
                })
                .thenReturn("Signup successful");
    }

    // ── Вход ──────────────────────────────────────────────────────────────────

    public Mono<TokenPair> authUser(Signin signinRequest) {
        return userRepo.findByEmail(signinRequest.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Неверный логин или пароль")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new RuntimeException("Неверный логин или пароль"));
                    }
                    return issueTokenPair(user);
                });
    }

    // ── Гостевой вход ─────────────────────────────────────────────────────────

    public Mono<TokenPair> registerGuest(String uuid) {
        String guestEmail = uuid + "@guest.local";

        return userRepo.findByEmail(guestEmail)
                .switchIfEmpty(Mono.defer(() -> {
                    UserEntity newGuest = new UserEntity();
                    newGuest.setName(uuid);
                    newGuest.setEmail(guestEmail);
                    newGuest.setGuest(true);
                    newGuest.setPassword(passwordEncoder.encode(uuid));
                    return userRepo.save(newGuest);
                }))
                .flatMap(this::issueTokenPair);
    }

    // ── Refresh ───────────────────────────────────────────────────────────────

    public Mono<TokenPair> refresh(RefreshRequest request) {
        return refreshTokenRepo.findByToken(request.getRefreshToken())
                .switchIfEmpty(Mono.error(new RuntimeException("Refresh token не найден или отозван")))
                .flatMap(tokenEntity -> {
                    if (tokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return refreshTokenRepo.delete(tokenEntity)
                                .then(Mono.error(new RuntimeException("Refresh token истёк")));
                    }
                    return userRepo.findById(tokenEntity.getUserId())
                            .switchIfEmpty(Mono.error(new RuntimeException("Пользователь не найден")))
                            .flatMap(user ->
                                    // Инвалидируем старый refresh token и выдаём новую пару
                                    refreshTokenRepo.delete(tokenEntity)
                                            .then(issueTokenPair(user))
                            );
                });
    }

    // ── После апгрейда гостя ──────────────────────────────────────────────────

    public Mono<TokenPair> loginAfterUpgrade(UserEntity user) {
        return issueTokenPair(user);
    }

    // ── Внутренний метод: выдать пару токенов ─────────────────────────────────

    private Mono<TokenPair> issueTokenPair(UserEntity user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()
        );
        String accessToken = jwtConfig.generateToken(auth);

        // Refresh token — случайный UUID, живёт 30 дней
        String rawRefresh = UUID.randomUUID().toString();
        RefreshTokenEntity refreshEntity = new RefreshTokenEntity();
        refreshEntity.setUserId(user.getId());
        refreshEntity.setToken(rawRefresh);
        refreshEntity.setExpiresAt(LocalDateTime.now().plusDays(30));

        return refreshTokenRepo.save(refreshEntity)
                .map(saved -> new TokenPair(accessToken, rawRefresh));
    }
}