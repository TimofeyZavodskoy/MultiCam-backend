package ru.hotdog.multicam_api.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.Signin;
import ru.hotdog.multicam_api.dto.Signup;
import ru.hotdog.multicam_api.entity.UserEntity;
import ru.hotdog.multicam_api.repository.UserRepo;
import ru.hotdog.multicam_api.security.JwtConfig;
import ru.hotdog.multicam_api.service.impl.UserDetailsImpl;

import java.util.ArrayList;
import java.util.UUID;

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
                    user.setName(signupRequest.getName());
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

public Mono<String> registerGuest(String uuid) {
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
            .map(user -> {
                UserDetailsImpl principal = UserDetailsImpl.build(user);
                Authentication auth = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

                return jwtConfig.generateToken(auth);
            });
}
    public Mono<String> loginAfterUpgrade(UserEntity user) {
        return Mono.just(jwtConfig.generateTokenForUser(user));
    }
}