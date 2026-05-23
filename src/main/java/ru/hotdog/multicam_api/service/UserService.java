package ru.hotdog.multicam_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.dto.SaveRequest;
import ru.hotdog.multicam_api.entity.SaveResultEntity;
import ru.hotdog.multicam_api.entity.UserEntity;
import ru.hotdog.multicam_api.repository.SaveResultRepo;
import ru.hotdog.multicam_api.repository.UserRepo;
import ru.hotdog.multicam_api.service.impl.UserDetailsImpl;

@Service
@RequiredArgsConstructor
public class UserService implements ReactiveUserDetailsService {

    private final UserRepo userRepo;
    private final SaveResultRepo saveResultRepo;
    private final ObjectMapper objectMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return userRepo.findByEmail(email)
                .map(UserDetailsImpl::build)
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(() -> new RuntimeException(
                        String.format("User with email '%s' not found", email))));
    }

    public Mono<SaveResultEntity> saveResult(SaveRequest request, String email) {
        return userRepo.findByEmail(email)
                .flatMap(user -> {
                    SaveResultEntity entity = new SaveResultEntity();
                    entity.setCategory(request.getCategory());
                    entity.setImageUrl(request.getImageUrl());
                    entity.setUserId(user.getId());
                    try {
                        String jsonString = objectMapper.writeValueAsString(request.getClientJson());
                        entity.setJsonData(jsonString);
                    } catch (Exception e) {
                        return Mono.error(new RuntimeException("Ошибка конвертации JSON", e));
                    }
                    return saveResultRepo.save(entity);
                })
                .switchIfEmpty(Mono.error(() -> new RuntimeException("Пользователь не найден")));
    }

    /**
     * Удаляет лайк. Проверяет, что запись принадлежит пользователю.
     */
    public Mono<Void> deleteLike(Long likeId, String email) {
        return userRepo.findByEmail(email)
                .switchIfEmpty(Mono.error(new RuntimeException("Пользователь не найден")))
                .flatMap(user -> saveResultRepo.findById(likeId)
                        .switchIfEmpty(Mono.error(new RuntimeException("Запись не найдена")))
                        .flatMap(entity -> {
                            if (!entity.getUserId().equals(user.getId())) {
                                return Mono.error(new RuntimeException("Доступ запрещён"));
                            }
                            return saveResultRepo.delete(entity);
                        })
                );
    }

    public Flux<SaveResultEntity> getLikes(String email) {
        return userRepo.findByEmail(email)
                .flatMapMany(user -> saveResultRepo.findAllByUserId(user.getId()));
    }

    public Mono<UserEntity> upgradeGuest(String guestEmail, String newEmail, String newPassword, String username) {
        return userRepo.findByEmail(guestEmail)
                .flatMap(user -> {
                    user.setEmail(newEmail);
                    user.setPassword(passwordEncoder.encode(newPassword));
                    user.setName(username);
                    user.setGuest(false);
                    return userRepo.save(user);
                });
    }
}