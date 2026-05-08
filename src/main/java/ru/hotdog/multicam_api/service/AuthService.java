package ru.hotdog.multicam_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.Signin;
import ru.hotdog.multicam_api.dto.Signup;
import ru.hotdog.multicam_api.entity.UserEntity;
import ru.hotdog.multicam_api.repository.UserRepo;
import ru.hotdog.multicam_api.security.JwtConfig;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public Mono<String> registerUser(Signup signupRequest) {
        return userRepo.existsByEmail(signupRequest.getEmail())
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new RuntimeException("Почта уже занята"));
                    }
                    UserEntity user = new UserEntity();
                    user.setName(signupRequest.getUsername());
                    user.setEmail(signupRequest.getEmail());
                    user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
                    return userRepo.save(user);
                })
                .thenReturn("Signup successful");
    }

    public Mono<String> authUser(Signin signinRequest) {
        return userRepo.findByEmail(signinRequest.getEmail())
                .switchIfEmpty(Mono.error(new RuntimeException("Неверный логин или пароль")))
                .flatMap(user -> {
                    if (!passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())) {
                        return Mono.error(new RuntimeException("Неверный логин или пароль"));
                    }
                    return Mono.just(jwtConfig.generateTokenForUser(user));
                });
    }
}