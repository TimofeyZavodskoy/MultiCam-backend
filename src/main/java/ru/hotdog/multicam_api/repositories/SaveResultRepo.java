package ru.hotdog.multicam_api.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.hotdog.multicam_api.entities.SaveResult;

@Repository
public interface SaveResultRepo extends JpaRepository<SaveResult, Long> {
}
