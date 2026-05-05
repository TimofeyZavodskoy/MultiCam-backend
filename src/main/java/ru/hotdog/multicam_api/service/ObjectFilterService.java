package ru.hotdog.multicam_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// FIX #10: был ru.hotdog.backForApi.dto — правильный пакет ru.hotdog.multicam_api.dto
import ru.hotdog.multicam_api.dto.DetectedObj;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Постфильтрация результатов детекции объектов.
 */
@Service
@Slf4j
public class ObjectFilterService {

    private static final Set<String> NOISE_LABELS = Set.of(
            "table", "desk", "chair", "sofa", "couch", "bench", "shelf", "counter",
            "cabinet", "drawer", "wardrobe", "bookshelf", "nightstand", "stool",
            "floor", "wall", "ceiling", "background", "surface", "ground",
            "carpet", "rug", "tile", "wood", "marble", "concrete", "pavement",
            "cloth", "fabric", "tablecloth", "napkin", "curtain", "blanket",
            "shadow", "reflection", "light", "window", "door", "frame",
            "grass", "sky", "tree", "building", "road", "sidewalk",
            "plate", "bowl", "tray"
    );

    public List<DetectedObj> filter(List<DetectedObj> objects) {
        if (objects == null || objects.isEmpty()) {
            return Collections.emptyList();
        }

        List<DetectedObj> filtered = objects.stream()
                .filter(obj -> !isNoise(obj.getLabel()))
                .collect(Collectors.toList());

        int removed = objects.size() - filtered.size();
        if (removed > 0) {
            log.info("ObjectFilter: удалено {} шумовых объектов из {}", removed, objects.size());
        }
        return filtered;
    }

    private boolean isNoise(String label) {
        if (label == null || label.isBlank()) return true;
        String lower = label.toLowerCase().trim();
        return NOISE_LABELS.stream().anyMatch(lower::contains);
    }
}