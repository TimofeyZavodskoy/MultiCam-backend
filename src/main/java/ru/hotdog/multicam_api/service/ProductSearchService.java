package ru.hotdog.multicam_api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
// Берем DTO из нашего пакета приложения.
import ru.hotdog.multicam_api.dto.DetectedObj;
import ru.hotdog.multicam_api.dto.SearchResult;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
// Сервис создает ссылки на поиск товаров.
public class ProductSearchService {

    // Базовая ссылка поиска Wildberries.
    private static final String WB_URL   = "https://www.wildberries.ru/catalog/0/search.aspx?search=";
    // Базовая ссылка поиска Ozon.
    private static final String OZON_URL = "https://www.ozon.ru/search/?text=";
    // Базовая ссылка поиска AliExpress.
    private static final String ALI_URL  = "https://aliexpress.ru/wholesale?SearchText=";

    // Создает ссылки на маркетплейсы по названию объекта.
    public List<SearchResult> generateLinks(String objectLabel) {
        if (objectLabel == null || objectLabel.isBlank()) {
            log.warn("ProductSearchService: пустая метка объекта, ссылки не сгенерированы");
            return Collections.emptyList();
        }

        String encoded = URLEncoder.encode(objectLabel.trim(), StandardCharsets.UTF_8);
        log.info("ProductSearchService: генерируем ссылки для «{}»", objectLabel);

        return List.of(
                new SearchResult("Wildberries", WB_URL   + encoded, "🟣"),
                new SearchResult("Ozon",        OZON_URL + encoded, "🔵"),
                new SearchResult("AliExpress",  ALI_URL  + encoded, "🟠")
        );
    }

    // Выбирает главный объект и создает ссылки для него.
    public List<SearchResult> generateLinksForPrimaryObject(List<DetectedObj> detectedObjs) {
        if (detectedObjs == null || detectedObjs.isEmpty()) {
            return Collections.emptyList();
        }

        DetectedObj primary = detectedObjs.stream()
                .max((a, b) -> {
                    double areaA = a.getBbox().getWidth() * a.getBbox().getHeight();
                    double areaB = b.getBbox().getWidth() * b.getBbox().getHeight();
                    return Double.compare(areaA, areaB);
                })
                .orElse(detectedObjs.get(0));

        log.info("ProductSearchService: первичный объект — «{}»", primary.getLabel());
        return generateLinks(primary.getLabel());
    }
}
