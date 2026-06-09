package ru.hotdog.multicam_api.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.hotdog.multicam_api.entity.SaveResultEntity;

@Repository
// Репозиторий для сохраненных результатов анализа.
public interface SaveResultRepo extends R2dbcRepository<SaveResultEntity, Long> {
    // Ищет все сохраненные результаты пользователя.
    Flux<SaveResultEntity> findAllByUserId(Long userId);
}
