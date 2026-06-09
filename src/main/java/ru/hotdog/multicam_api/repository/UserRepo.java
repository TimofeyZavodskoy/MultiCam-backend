package ru.hotdog.multicam_api.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.entity.UserEntity;

@Repository
// Репозиторий для работы с пользователями.
public interface UserRepo extends R2dbcRepository<UserEntity, Long> {
    // Ищет пользователя по почте.
    Mono<UserEntity> findByEmail(String email);
    // Проверяет, занята ли почта.
    Mono<Boolean> existsByEmail(String email);
}
