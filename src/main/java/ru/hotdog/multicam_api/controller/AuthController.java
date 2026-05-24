package ru.hotdog.multicam_api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.*;
import ru.hotdog.multicam_api.service.AuthService;
import ru.hotdog.multicam_api.service.UserService;

import java.security.Principal;

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
    public Mono<ResponseEntity<TokenPair>> registerGuest(@RequestBody GuestRequest request) {
        return authService.registerGuest(request.getUuid())
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).build()));
    }

    @PostMapping("/signin")
    public Mono<ResponseEntity<TokenPair>> signin(@Valid @RequestBody Signin signinRequest) {
        return authService.authUser(signinRequest)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.badRequest().build()));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<TokenPair>> refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request)
                .map(ResponseEntity::ok)
                .onErrorResume(ex -> Mono.just(ResponseEntity.status(401).build()));
    }

    @PostMapping("/upgrade")
    public Mono<ResponseEntity<TokenPair>> upgradeAccount(
            @RequestBody Signup request,
            Principal principal
    ) {
        return userService.upgradeGuest(
                        principal.getName(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getName()
                )
                .flatMap(authService::loginAfterUpgrade)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(400).build()));
    }
}