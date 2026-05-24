package ru.hotdog.multicam_api.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.hotdog.multicam_api.entity.RefreshTokenEntity;

@Repository
public interface RefreshTokenRepo extends R2dbcRepository<RefreshTokenEntity, Long> {
    Mono<RefreshTokenEntity> findByToken(String token);
    Mono<Void> deleteByUserId(Long userId);
}
