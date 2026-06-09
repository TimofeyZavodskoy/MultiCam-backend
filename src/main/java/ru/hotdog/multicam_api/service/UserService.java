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
// Сервис для пользователей и сохраненных результатов.
public class UserService implements ReactiveUserDetailsService {

    // Репозиторий пользователей.
    private final UserRepo userRepo;
    // Репозиторий сохраненных результатов.
    private final SaveResultRepo saveResultRepo;
    // Переводит объект результата в JSON.
    private final ObjectMapper objectMapper;
    // Кодирует пароль при апгрейде гостя.
    private final PasswordEncoder passwordEncoder;

    @Override
    // Загружает пользователя для Spring Security.
    public Mono<UserDetails> findByUsername(String email) {
        return userRepo.findByEmail(email)
                .map(UserDetailsImpl::build)
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(() -> new RuntimeException(
                        String.format("User with email '%s' not found", email))));
    }

    // Сохраняет результат анализа для пользователя.
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

    // Удаляет лайк, если он принадлежит пользователю.
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

    // Возвращает сохраненные результаты пользователя.
    public Flux<SaveResultEntity> getLikes(String email) {
        return userRepo.findByEmail(email)
                .flatMapMany(user -> saveResultRepo.findAllByUserId(user.getId()));
    }

    // Меняет гостевой аккаунт на обычный.
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
