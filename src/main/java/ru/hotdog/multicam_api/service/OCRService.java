package ru.hotdog.multicam_api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;
import ru.hotdog.multicam_api.dto.DetectedObj;
import ru.hotdog.multicam_api.dto.OCRResponse;
import ru.hotdog.multicam_api.dto.SearchResult;
import ru.hotdog.multicam_api.prompt.OcrPrompt;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OCRService {

    private static final Set<String> KNOWN_CATEGORIES = Set.of(
            "physics", "chemistry", "math", "mixed", "text", "food", "objects", "image", "noise"
    );

    @Value("${deepseek.api.key:}")
    private String deepSeekApiKey;

    @Value("${deepseek.api.model:deepseek-chat}")
    private String deepSeekModel;

    @Value("${llm.api.model}")
    private String localModel;

    @Value("${llm.api.temperature}")
    private double localTemperature;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient localWebClient;
    private final WebClient deepSeekWebClient;
    private final ObjectFilterService objectFilterService;
    private final ProductSearchService productSearchService;

    public OCRService(WebClient.Builder webClientBuilder,
                      ObjectFilterService objectFilterService,
                      ProductSearchService productSearchService,
                      @Value("${llm.api.base-url}") String modelBaseUrl,
                      @Value("${deepseek.api.base-url}") String deepSeekBaseUrl) {

        this.objectFilterService = objectFilterService;
        this.productSearchService = productSearchService;

        log.info("[INIT] Инициализация OCRService с URL: {}", modelBaseUrl);

        HttpClient localHttpClient = HttpClient.create()
                .noProxy()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100000)
                .responseTimeout(Duration.ofSeconds(180))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(180, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS)));

        this.localWebClient = webClientBuilder.clone()
                .baseUrl(modelBaseUrl)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(localHttpClient))
                .build();

        HttpClient deepSeekHttpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(180));

        this.deepSeekWebClient = webClientBuilder.clone()
                .baseUrl(deepSeekBaseUrl)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(deepSeekHttpClient))
                .build();
    }

    // ── Public entry point ────────────────────────────────────────────────────

    public Mono<OCRResponse> processRequest(byte[] imageBytes) {
        log.info("[PIPELINE-START] Получен запрос на обработку. Размер изображения: {} байт", imageBytes.length);

        return sendToVllm(imageBytes, OcrPrompt.CLASSIFIER, 32)
                .map(OCRService::normalizeCategory)
                .flatMap(category -> categoryRouter(imageBytes, category))
                .onErrorResume(ex -> {
                    log.error("[PIPELINE-ERROR] Критическая ошибка на верхнем уровне пайплайна: {}", ex.getMessage(), ex);
                    OCRResponse err = new OCRResponse();
                    err.setResult("Ошибка обработки: " + ex.getMessage());
                    return Mono.just(err);
                });
    }

    // ── Router ────────────────────────────────────────────────────────────────

    private Mono<OCRResponse> categoryRouter(byte[] imageBytes, String category) {
        log.info("[ROUTER] Направление потока в обработчик категории: {}", category);
        return switch (category) {
            case "math", "mixed" -> handleMath(imageBytes);
            case "physics"       -> handlePhysics(imageBytes);
            case "chemistry"     -> handleChemistry(imageBytes);
            case "text"          -> handleText(imageBytes);
            case "food"          -> handleFood(imageBytes);
            case "objects"       -> handleObjs(imageBytes);
            case "image"         -> handleImage(imageBytes);
            case "noise" -> {
                log.info("[ROUTER] Категория 'noise'. Прерываем пайплайн, возвращаем заглушку.");
                OCRResponse response = new OCRResponse();
                response.setTag("noise");
                response.setResult("На изображении не обнаружен четкий объект для анализа. Попробуйте сфотографировать объект на однородном фоне");
                yield Mono.just(response);
            }
            default -> {
                log.warn("[ROUTER] Неизвестная категория '{}'. Фоллбэк на 'handleImage'.", category);
                yield handleImage(imageBytes);
            }
        };
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private Mono<OCRResponse> handleMath(byte[] imageBytes) {
        log.info("[HANDLER-MATH] Старт обработки. Шаг 1: Извлекаем текст из изображения.");
        return handleMathOCR(imageBytes)
                .flatMap(textResponse -> {
                    String extractedText = textResponse.getResult();
                    log.info("[HANDLER-MATH] Шаг 2: Текст успешно извлечен. Используем deepseek-v3.1...");
                    log.debug("[HANDLER-MATH] Извлеченный текст:\n{}", extractedText);
                    return mathSolver(extractedText);
                })
                .map(solvedResult -> {
                    OCRResponse response = new OCRResponse();
                    response.setTag("math");
                    response.setResult(solvedResult);
                    log.info("[HANDLER-MATH] Обработка математики успешно завершена.");
                    return response;
                });
    }

    private Mono<OCRResponse> handleChemistry(byte[] imageBytes) {
        log.info("[HANDLER-CHEMISTRY] Старт обработки запроса");
        return scienceSolver(imageBytes, OcrPrompt.CHEMISTRY, 8192)
                .map(res -> {
                    log.debug("[HANDLER-CHEMISTRY] Распознанная задача:\n{}", res);
                    OCRResponse response = new OCRResponse();
                    response.setTag("chemistry");
                    response.setResult(res);
                    log.info("[HANDLER-CHEMISTRY] Обработка успешно завершена");
                    return response;
                });
    }

    private Mono<OCRResponse> handlePhysics(byte[] imageBytes) {
        log.info("[HANDLER-PHYSICS] Старт обработки запроса");
        return scienceSolver(imageBytes, OcrPrompt.PHYSICS, 8192)
                .map(res -> {
                    log.debug("[HANDLER-PHYSICS] Распознанная задача:\n{}", res);
                    OCRResponse response = new OCRResponse();
                    response.setTag("physics");
                    response.setResult(res);
                    log.info("[HANDLER-PHYSICS] Обработка успешно завершена");
                    return response;
                });
    }

    private Mono<OCRResponse> handleText(byte[] imageBytes) {
        log.info("[HANDLER-TEXT] Старт обработки текста.");
        return sendToVllm(imageBytes, OcrPrompt.OCR, 1024)
                .map(res -> {
                    log.debug("[HANDLER-TEXT] Распознанный текст:\n{}", res);
                    OCRResponse response = new OCRResponse();
                    response.setTag("text");
                    response.setResult(res);
                    log.info("[HANDLER-TEXT] Обработка успешно завершена.");
                    return response;
                });
    }

    private Mono<OCRResponse> handleMathOCR(byte[] imageBytes) {
        log.info("[MATH-OCR] Старт обработки математического текста");
        return sendToVllm(imageBytes, OcrPrompt.EXTRACT, 2048)
                .map(res -> {
                    log.debug("[MATH-OCR] Распознанная формула:\n{}", res);
                    OCRResponse response = new OCRResponse();
                    response.setTag("null");
                    response.setResult(res);
                    log.info("[MATH-OCR] Обработка успешно завершена.");
                    return response;
                });
    }

    private Mono<OCRResponse> handleFood(byte[] imageBytes) {
        log.info("[HANDLER-FOOD] Старт анализа КБЖУ.");
        return sendToVllm(imageBytes, OcrPrompt.FOOD, 512)
                .map(jsonStr -> {
                    log.info("[HANDLER-FOOD] Получен сырой ответ от модели.");
                    log.debug("[HANDLER-FOOD] Содержимое ответа:\n{}", jsonStr);
                    try {
                        String cleanJson = jsonStr.replaceAll("```json\\s*", " ").replaceAll("```", " ").trim();
                        log.debug("[HANDLER-FOOD] Ответ после очистки регулярками:\n{}", cleanJson);

                        OCRResponse response = objectMapper.readValue(cleanJson, OCRResponse.class);
                        response.setTag("food");
                        log.info("[HANDLER-FOOD] JSON успешно распаршен в объект. Калории: {}", response.getCalories());
                        return response;
                    } catch (Exception e) {
                        log.error("[HANDLER-FOOD] Ошибка парсинга JSON еды. Сырая строка: {}", jsonStr, e);
                        OCRResponse err = new OCRResponse();
                        err.setTag("food");
                        err.setResult("Ошибка разбора данных о еде");
                        return err;
                    }
                });
    }

    private Mono<OCRResponse> handleObjs(byte[] imageBytes) {
        log.info("[HANDLER-OBJS] Старт детекции объектов.");
        return sendToVllm(imageBytes, OcrPrompt.DETECT, 1024)
                .map(jsonStr -> {
                    log.info("[HANDLER-OBJS] Получен сырой ответ от модели.");
                    log.debug("[HANDLER-OBJS] Содержимое ответа:\n{}", jsonStr);
                    try {
                        String clear = stripJsonFences(jsonStr);
                        log.debug("[HANDLER-OBJS] Строка после stripJsonFences:\n{}", clear);

                        List<DetectedObj> raw = objectMapper.readValue(clear, new TypeReference<List<DetectedObj>>() {});
                        log.info("[HANDLER-OBJS] Распаршено {} объектов до фильтрации.", raw.size());

                        List<DetectedObj> filtered = objectFilterService.filter(raw);
                        log.info("[HANDLER-OBJS] После фильтрации осталось {} объектов.", filtered.size());

                        OCRResponse response = new OCRResponse();
                        response.setTag("objects");
                        response.setDetectedObjs(filtered);

                        if (!filtered.isEmpty()) {
                            log.info("[HANDLER-OBJS] Запуск генерации ссылок на маркетплейсы для объекта: {}", filtered.get(0).getLabel());
                            List<SearchResult> links = productSearchService.generateLinksForPrimaryObject(filtered);
                            response.setMarketplaceLinks(links);
                            log.info("[HANDLER-OBJS] Найдено {} ссылок.", links != null ? links.size() : 0);
                        } else {
                            log.info("[HANDLER-OBJS] Список отфильтрованных объектов пуст, поиск ссылок пропущен.");
                        }

                        return response;
                    } catch (Exception e) {
                        log.error("[HANDLER-OBJS] Ошибка парсинга массива объектов. Сырая строка: {}", jsonStr, e);
                        OCRResponse err = new OCRResponse();
                        err.setTag("objects");
                        err.setResult("Не удалось разобрать список объектов");
                        return err;
                    }
                });
    }

    private Mono<OCRResponse> handleImage(byte[] imageBytes) {
        log.info("[HANDLER-IMAGE] Старт генерации описания изображения.");
        return sendToVllm(imageBytes, OcrPrompt.DESCRIPTION, 1024)
                .map(res -> {
                    log.debug("[HANDLER-IMAGE] Сгенерированное описание:\n{}", res);
                    OCRResponse response = new OCRResponse();
                    response.setTag("image");
                    response.setDescription(res);
                    response.setResult(res);
                    log.info("[HANDLER-IMAGE] Обработка успешно завершена.");
                    return response;
                });
    }

    // ── LLM clients ───────────────────────────────────────────────────────────

    private Mono<String> sendToVllm(byte[] imageBytes, OcrPrompt prompt, int maxTokens) {
        log.info("[gpt-5.4-nano] Подготовка запроса к модели. Модель: {}, prompt: {}, maxTokens: {}",
                localModel, prompt.name(), maxTokens);

        String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "model", localModel.trim(),
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", prompt.getText()),
                                Map.of("type", "image_url", "image_url", Map.of("url", base64Image))
                        ))
                ),
                "temperature", localTemperature,
                "max_completion_tokens", maxTokens
        );

        log.info("[gpt-5.4-nano] Отправка POST /v1/chat/completions");
        long startTime = System.currentTimeMillis();

        return localWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + deepSeekApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    log.info("[gpt-5.4-nano] Ответ получен за {} мс", System.currentTimeMillis() - startTime);
                    log.debug("[gpt-5.4-nano] Сырой ответ (Map): {}", response);
                    return extractContentFromResponse(response);
                })
                .doOnError(err -> log.error("[gpt-5.4-nano] Ошибка: {}", err.getMessage(), err))
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Детальная ошибка от ProxyAPI: Код {}, Тело: {}", ex.getStatusCode(), ex.getResponseBodyAsString()))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2))
                        .filter(err -> !(err instanceof WebClientResponseException ex) || ex.getStatusCode().is5xxServerError())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private Mono<String> scienceSolver(byte[] imageBytes, OcrPrompt prompt, int maxTokens) {
        log.info("[gemini-3.1-flash-lite] Подготовка запроса. Модель: {}, prompt: {}, maxTokens: {}",
                deepSeekModel, prompt.name(), maxTokens);

        String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekModel.trim(),
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", prompt.getText()),
                                Map.of("type", "image_url", "image_url", Map.of("url", base64Image))
                        ))
                ),
                "max_completion_tokens", maxTokens
        );

        log.info("[gemini-3.1-flash-lite] Отправка POST /v1/chat/completions");
        long startTime = System.currentTimeMillis();

        return deepSeekWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + deepSeekApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    log.info("[gemini-3.1-flash-lite] Ответ получен за {} мс", System.currentTimeMillis() - startTime);
                    log.debug("[gemini-3.1-flash-lite] Сырой ответ (Map): {}", response);
                    return extractContentFromResponse(response);
                })
                .doOnError(err -> log.error("[gemini-3.1-flash-lite] Ошибка: {}", err.getMessage(), err))
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Детальная ошибка от ProxyAPI: Код {}, Тело: {}", ex.getStatusCode(), ex.getResponseBodyAsString()))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2))
                        .filter(err -> !(err instanceof WebClientResponseException ex) || ex.getStatusCode().is5xxServerError())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private Mono<String> mathSolver(String problemText) {
        log.info("[DEEPSEEK-CLIENT] Подготовка запроса к DeepSeek. Модель: {}", deepSeekModel);
        log.debug("[DEEPSEEK-CLIENT] Задача для решения:\n{}", problemText);

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekModel,
                "messages", List.of(
                        Map.of("role", "user", "content", OcrPrompt.MATH.getText() + "\n\n" + problemText)
                ),
                "max_completion_tokens", 8192
        );

        long startTime = System.currentTimeMillis();

        return deepSeekWebClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + deepSeekApiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    log.info("[DEEPSEEK-CLIENT] Ответ получен за {} мс", System.currentTimeMillis() - startTime);
                    return extractContentFromResponse(response);
                })
                .doOnError(err -> log.error("[DEEPSEEK-CLIENT] Ошибка: {}", err.getMessage()))
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("[DEEPSEEK-CLIENT] Тело ошибки: {}", ex.getResponseBodyAsString()))
                .retryWhen(Retry.fixedDelay(1, Duration.ofSeconds(5))
                        .filter(err -> !(err instanceof WebClientResponseException ex) || ex.getStatusCode().is5xxServerError())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    // ── Utils ─────────────────────────────────────────────────────────────────

    private String stripJsonFences(String raw) {
        log.debug("[UTILS] Вызов stripJsonFences. Исходная строка: {}", raw);
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>\\s*", " ")
                .replaceAll("(?s)```json\\s*", " ")
                .replaceAll("(?s)```\\s*", " ")
                .trim();
        log.debug("[UTILS] stripJsonFences результат: {}", cleaned);
        return cleaned;
    }

    static String normalizeCategory(String raw) {
        log.debug("[UTILS] Вызов normalizeCategory. Исходная строка: '{}'", raw);
        if (raw == null || raw.isBlank()) return "";

        String clean = raw.toLowerCase().trim().replaceAll("[^a-z]", " ");
        List<String> words = List.of(clean.trim().split("\\s+"));
        if (words.size() == 1 && KNOWN_CATEGORIES.contains(words.get(0))) return words.get(0);

        if (words.contains("physics"))   return "physics";
        if (words.contains("chemistry")) return "chemistry";
        if (words.contains("mixed"))     return "mixed";
        if (words.contains("math"))      return "math";
        if (clean.contains("food")  || clean.contains("meal"))       return "food";
        if (clean.contains("noise") || clean.contains("empty") || clean.contains("background")) return "noise";
        if (clean.contains("object") || clean.contains("product"))   return "objects";
        if (clean.contains("text"))      return "text";
        if (clean.contains("image") || clean.contains("scene") || clean.contains("photo")) return "image";

        log.debug("[UTILS] normalizeCategory не нашел четких совпадений, возвращаем: '{}'", clean);
        return clean;
    }

    @SuppressWarnings("unchecked")
    private String extractContentFromResponse(Map<String, Object> response) {
        try {
            log.debug("[UTILS] Извлечение content из ответа...");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            String noThink = content.replaceAll("(?s)<think>.*?</think>", " ").trim();
            log.debug("[UTILS] Извлеченный текст длиной {} символов", noThink.length());
            return noThink;
        } catch (Exception e) {
            log.error("[UTILS] Ошибка парсинга Map ответа. Тело Map: {}", response, e);
            return "Ошибка при чтении ответа модели.";
        }
    }
}