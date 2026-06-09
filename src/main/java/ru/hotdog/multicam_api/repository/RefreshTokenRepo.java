package ru.hotdog.multicam_api.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.entity.RefreshTokenEntity;

@Repository
// Репозиторий для работы с refresh token.
public interface RefreshTokenRepo extends R2dbcRepository<RefreshTokenEntity, Long> {
    // Ищет refresh token по строке токена.
    Mono<RefreshTokenEntity> findByToken(String token);
    // Удаляет все refresh token пользователя.
    Mono<Void> deleteByUserId(Long userId);
}
