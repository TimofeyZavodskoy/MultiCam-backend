package ru.hotdog.multicam_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.Signin;
import ru.hotdog.multicam_api.dto.Signup;
import ru.hotdog.multicam_api.service.AuthService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup/save")
    public Mono<ResponseEntity<String>> signup(@Valid @RequestBody Signup signupRequest) {
        return authService.registerUser(signupRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body(ex.getMessage())));
    }

    @PostMapping("/signin")
    public Mono<ResponseEntity<String>> signin(@Valid @RequestBody Signin signinRequest) {
        return authService.authUser(signinRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body(ex.getMessage())));
    }
}