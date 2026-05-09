package ru.hotdog.multicam_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.GuestRequest;
import ru.hotdog.multicam_api.dto.Signin;
import ru.hotdog.multicam_api.dto.Signup;
import ru.hotdog.multicam_api.service.AuthService;
import ru.hotdog.multicam_api.service.UserService;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/signup/save")
    public Mono<ResponseEntity<String>> signup(@Valid @RequestBody Signup signupRequest) {
        return authService.registerUser(signupRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body(ex.getMessage())));
    }

    @PostMapping("/signup/guest")
    public Mono<ResponseEntity<String>> registerGuest(@RequestBody GuestRequest request) {
        return authService.registerGuest(request.getUuid())
                .map(tokenResponse -> ResponseEntity.ok(tokenResponse))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body("Ошибка: " + e.getMessage())));
    }

    @PostMapping("/upgrade")
    public Mono<ResponseEntity<String>> upgradeAccount(
            @RequestBody Signup request,
            Principal principal
    ) {
        String guestEmail = principal.getName();

        return userService.upgradeGuest(guestEmail, request.getEmail(), request.getPassword(), request.getName())
                .flatMap(authService::loginAfterUpgrade)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(400).body(Map.of("error", e.getMessage()).toString())));
    }

    @PostMapping("/signin")
    public Mono<ResponseEntity<String>> signin(@Valid @RequestBody Signin signinRequest) {
        return authService.authUser(signinRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().body(ex.getMessage())));
    }


}