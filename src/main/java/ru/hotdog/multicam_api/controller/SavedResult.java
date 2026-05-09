package ru.hotdog.multicam_api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.SaveRequest;
import ru.hotdog.multicam_api.entity.SaveResultEntity;
import ru.hotdog.multicam_api.service.UserService;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/save")
@Slf4j
public class SavedResult {

    private final UserService userService;

    @PostMapping("/like")
    public Mono<ResponseEntity<String>> saveResult(@RequestBody SaveRequest request, Principal principal) {
        return userService.saveResult(request, principal.getName())
                .map(saved -> ResponseEntity.ok("Успешно сохранено!"))
                .onErrorResume(e -> {
                    log.error("Save error", e);
                    return Mono.just(ResponseEntity.status(500).body("Ошибка: " + e.getMessage()));
                });
    }

    @GetMapping("/likes/all")
     public Flux<SaveResultEntity> getAllLikes(Principal principal) {
        return userService.getLikes(principal.getName());
    }
}
