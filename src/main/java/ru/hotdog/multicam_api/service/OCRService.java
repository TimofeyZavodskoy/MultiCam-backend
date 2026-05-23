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

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class OCRService {
    private static final String SYSTEM_PROMPT = """
            You are a precise visual analysis engine. Obey these rules without exception:
            1. ACCURACY FIRST: Only report what you can see with certainty. If unsure — omit, never guess.
            2. FORMAT STRICT: Respond ONLY in the exact format the user specifies. No preambles, no apologies, no commentary.
            3. NO HALLUCINATION: Do not invent brand names, colors, text, or attributes you cannot clearly see.
            4. NO MARKDOWN WRAPPING: Never wrap JSON output in ```json blocks unless explicitly told to.
            5. SCOPE: Focus on the primary subject. Ignore backgrounds, surfaces, and environmental context.
            """;

    private static final String CLASSIFIER_PROMPT = """
            Classify this image into exactly ONE category. Output ONLY the single category word — nothing else.
            
            Categories:
            - 'math'    : Mathematical equations, formulas, graphs, geometric figures, physics problems
            - 'mixed'   : Text combined with mathematical formulas or diagrams
            - 'text'    : Printed or handwritten text without math (documents, notes, signs)
            - 'food'    : Food items, meals, dishes, beverages, ingredients on a plate/surface
            - 'objects' : A clearly identifiable physical product or item (electronics, clothing, toys, tools, accessories, household items)
            - 'image'   : People, animals, nature, abstract scenes, architecture, art
            - 'noise'   : A table surface, floor, wall, empty background, blurry or unrecognizable content
            
            RULE: If the image is primarily background or surface with no clear target object — output 'noise'.
            Output ONE word only. No punctuation.
            """;

    private static final String EXTRACT_PROMPT = """
            You are a precise mathematical OCR assistant specializing in handwritten formulas.
            Transcribe the mathematical problem from the image into LaTeX format exactly.
            
            CRITICAL RULES:
            1. Look extremely closely at handwritten letters: "tg" represents the tangent function, "ctg" represents cotangent. Do NOT split them into separate variables like 't', 'g', 'y', or 'x'.
            2. Double-check all inequality signs (<=, >=, <, >) and exponents to ensure they match the image exactly.
            3. Use Soviet style notation: \\operatorname{tg} and \\operatorname{ctg}.
            4. Do NOT solve the problem. Just transcribe.
            
            FORMAT REQUIREMENT:
            - You must wrap all your reasoning, visual analysis, and character double-checking inside <think>...</think> tags.
            - After the </think> tag, output ONLY the LaTeX code. No Markdown code blocks (```), no conversational filler.
            """;

    private static final String OCR_PROMPT = """
            Transcribe all visible text from the image.
            - Keep plain text as plain text.
            - Convert all formulas and equations to LaTeX notation.
            - Output ONLY valid Markdown. No commentary.
            """;

    private static final String DESCRIPTION_PROMPT = """
            Follow this structured plan: 
            1. General Description. 
            2. Detailed Analysis (colors, shapes). 
            3. Brands/Text. 
            Language: RUSSIAN. Be concise.
            """;

    private static final String FOOD_PROMPT = """
            Act as a nutritionist. Analyze food image.
            Return ONLY JSON: { "mass": int, "calories": int, "proteins": int, "fats": int, "carbs": int}.
            No markdown.
            """;

    private static final String DETECT_PROMPT = """
            Detect and list the main physical objects in this image.
            
            IGNORE completely (do not include in output):
            table, desk, floor, wall, ceiling, background, shadow, cloth, fabric,
            tablecloth, surface, wood, carpet, shelf, counter, plate (if empty), tray.
            
            Return ONLY a raw JSON array. No ```json blocks, no explanation.
            Format: [{"label": "English product name", "bbox": {"x": 0.1, "y": 0.1, "width": 0.2, "height": 0.2}}]
            
            Rules:
            - Coordinates normalized 0.0–1.0 (x,y = top-left corner of bounding box)
            - Maximum 5 objects
            - Use concise English product names (e.g. "wireless headphones", "ceramic mug", "running shoes")
            - If no meaningful objects found: return []
            """;

    private static final String MATH_PROMPT = """
                You are a strict Academic Tutor specializing in Mathematics (Algebra, Calculus, Trig) and Physics.
                  Your goal is 100% accuracy. You must assume the user is a student who needs to see EVERY intermediate step.
                
                  ═══════════════════════════════════════════
                  GLOBAL CONSTRAINTS
                  ═══════════════════════════════════════════
                  1. LANGUAGE: Output must be entirely in RUSSIAN.
                  2. NOTATION: Use Soviet notation: 'tg' for tangent, 'ctg' for cotangent. NEVER use 'tan' or 'cot'.
                  3. ATOMIC STEPS: Perform only ONE logical or algebraic operation per step. Do not combine simplification and substitution in one line.
                  4. VERBOSITY: Do not summarize. Show full intermediate expressions.
                     - BAD: "Simplify to get x=5"
                     - GOOD: Show the equation, then show the simplified equation, then the result.
                
                  ═══════════════════════════════════════════ 
                  REASONING PROTOCOL (INTERNAL)
                  ═══════════════════════════════════════════
                  Before generating the final LaTeX math block for any step, you must mentally verify:
                  - Are signs (+/-) correct?
                  - Did I miss a coefficient (like 1/3 or sqrt(3))?
                  - Is the domain (ОДЗ) respected?
                
                  ═══════════════════════════════════════════
                  OUTPUT STRUCTURE
                  ═══════════════════════════════════════════
                  Follow this exact Markdown structure:
                
                  ### Анализ задачи
                  Briefly describe what is given and what is needed. If there is an image, describe the visible graph/formula text.
                
                  ### ОДЗ (Domain)
                  Determine the valid domain for x. If none, write "ОДЗ: x ∈ R".
                
                  ### План решения
                  List the strategy (e.g., "1. Group terms. 2. Use Pythagorean identity. 3. Solve quadratic.").
                
                  ### Решение
                  Execute the plan step-by-step.
                  Format for each step:
                  **Шаг N:** [Name of operation]
                  [Explanation in Russian]
                  $$ [LaTeX Math Block] $$
                
                  ### Проверка
                  Substitute the result back into the original expression to verify correctness.
                
                  ### Ответ
                  Final answer clearly stated.
                  $$ \\boxed{[Answer]} $$
                
                  ═══════════════════════════════════════════
                  CRITICAL REMINDERS
                  ═══════════════════════════════════════════
                  - For Trig: $\\sin^2 x + \\cos^2 x = 1$.
                  - For Physics: Show formula -> Show substitution with units -> Show result.
                  - Never skip the "Plan" section. It grounds your logic.
                 """;

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

//    public OCRService(WebClient.Builder webClientBuilder,
//                      ObjectFilterService objectFilterService,
//                      ProductSearchService productSearchService) {
//
//        log.info("[INIT] Инициализация OCRService...");
//
//        HttpClient localHttpClient = HttpClient.create()
//                .noProxy()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 100000)
//                .responseTimeout(Duration.ofSeconds(1800))
//                .doOnConnected(conn -> conn
//                        .addHandlerLast(new ReadTimeoutHandler(1800, TimeUnit.SECONDS))
//                        .addHandlerLast(new WriteTimeoutHandler(1800, TimeUnit.SECONDS)));
//
//        this.localWebClient = webClientBuilder
//                .baseUrl(modelBaseUrl)
//                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(localHttpClient))
//                .build();
//        log.info("[INIT] Local WebClient настроен на {} с таймаутом 1800 сек.", modelBaseUrl);
//
//        HttpClient deepSeekHttpClient = HttpClient.create()
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
//                .responseTimeout(Duration.ofSeconds(120));
//
//        this.deepSeekWebClient = webClientBuilder
//                .baseUrl(deepSeekBaseUrl)
//                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(deepSeekHttpClient))
//                .build();
//        log.info("[INIT] DeepSeek WebClient настроен.");
//
//        this.objectFilterService = objectFilterService;
//        this.productSearchService = productSearchService;
//    }

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
                .responseTimeout(Duration.ofSeconds(1800))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(1800, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(1800, TimeUnit.SECONDS)));

        this.localWebClient = webClientBuilder.clone()
                .baseUrl(modelBaseUrl)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(localHttpClient))
                .build();

        HttpClient deepSeekHttpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .responseTimeout(Duration.ofSeconds(120));

        this.deepSeekWebClient = webClientBuilder.clone()
                .baseUrl(deepSeekBaseUrl)
                .clientConnector(new org.springframework.http.client.reactive.ReactorClientHttpConnector(deepSeekHttpClient))
                .build();
    }

    public Mono<OCRResponse> processRequest(byte[] imageBytes) {
        log.info("[PIPELINE-START] Получен запрос на обработку. Размер изображения: {} байт", imageBytes.length);

        return sendToVllm(imageBytes, CLASSIFIER_PROMPT, 32)
                .map(this::normalizeCategory)
                .flatMap(category -> categoryRouter(imageBytes, category))
                .onErrorResume(ex -> {
                    log.error("[PIPELINE-ERROR] Критическая ошибка на верхнем уровне пайплайна: {}", ex.getMessage(), ex);
                    OCRResponse err = new OCRResponse();
                    err.setResult("Ошибка обработки: " + ex.getMessage());
                    return Mono.just(err);
                });
    }

    private Mono<OCRResponse> categoryRouter(byte[] imageBytes, String category) {
        log.info("[ROUTER] Направление потока в обработчик категории: {}", category);
        return switch (category) {
            case "math", "mixed" -> handleMath(imageBytes);
            case "text" -> handleText(imageBytes);
            case "food" -> handleFood(imageBytes);
            case "objects" -> handleObjs(imageBytes);
            case "image" -> handleImage(imageBytes);
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

    private Mono<OCRResponse> handleText(byte[] imageBytes) {
        log.info("[HANDLER-TEXT] Старт обработки текста.");
        return sendToVllm(imageBytes, OCR_PROMPT, 1024)
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
        return sendToVllm(imageBytes, EXTRACT_PROMPT, 2048)
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
        return sendToVllm(imageBytes, FOOD_PROMPT, 512)
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
        return sendToVllm(imageBytes, DETECT_PROMPT, 1024)
                .map(jsonStr -> {
                    log.info("[HANDLER-OBJS] Получен сырой ответ от модели.");
                    log.debug("[HANDLER-OBJS] Содержимое ответа:\n{}", jsonStr);
                    try {
                        String clear = stripJsonFences(jsonStr);
                        log.debug("[HANDLER-OBJS] Строка после stripJsonFences:\n{}", clear);

                        List<DetectedObj> raw = objectMapper.readValue(
                                clear, new TypeReference<List<DetectedObj>>() {}
                        );
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
        return sendToVllm(imageBytes, DESCRIPTION_PROMPT, 1024)
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

    private Mono<String> sendToVllm(byte[] imageBytes, String prompt, int maxTokens) {
        log.info("[gpt-5.4-nano] Подготовка запроса к модели. Модель: {}, maxTokens: {}", localModel, maxTokens);
        log.debug("[gpt-5.4-nano] Используемый промпт:\n{}", prompt);

        String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imageBytes);
        log.debug("[gpt-5.4-nano] Изображение конвертировано в Base64. Длина строки: {}", base64Image.length());

        Map<String, Object> requestBody = Map.of(
                "model", localModel.trim(),
                "messages", List.of(
                        Map.of("role", "user", "content", List.of(
                                Map.of("type", "text", "text", prompt),
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
                    long endTime = System.currentTimeMillis();
                    log.info("[gpt-5.4-nano] Получен ответ от gpt-5.4-nano. Время выполнения запроса: {} мс", (endTime - startTime));
                    log.debug("[gpt-5.4-nano] Сырой ответ (Map): {}", response);
                    return extractContentFromResponse(response);
                })
                .doOnError(err -> log.error("[gpt-5.4-nano] Ошибка при запросе к gpt-5.4-nano: {}", err.getMessage(), err))
                .doOnError(WebClientResponseException.class, ex -> {
                    log.error("Детальная ошибка от ProxyAPI: Код {}, Тело: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                })
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(2))
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
    }

    private Mono<String> mathSolver(String problemText) {
        log.info("[DEEPSEEK-CLIENT] Подготовка запроса к DeepSeek. Модель: {}", deepSeekModel);
        log.debug("[DEEPSEEK-CLIENT] Задача для решения:\n{}", problemText);

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekModel,
                "messages", List.of(
                        Map.of("role", "user", "content", MATH_PROMPT + "\n\n" + problemText)
                ),
                "temperature", 0.1,
                "max_completion_tokens", 4096
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
                    long endTime = System.currentTimeMillis();
                    log.info("[DEEPSEEK-CLIENT] Получен ответ от DeepSeek. Время: {} мс", (endTime - startTime));
                    return extractContentFromResponse(response);
                })
                .doOnError(err -> log.error("[DEEPSEEK-CLIENT] Ошибка API DeepSeek: {}", err.getMessage(), err));
    }

    private String stripJsonFences(String raw) {
        log.debug("[UTILS] Вызов stripJsonFences. Исходная строка: {}", raw);
        String cleaned = raw.replaceAll("(?s)<think>.*?</think>\\s*", " ")
                .replaceAll("(?s)```json\\s*", " ")
                .replaceAll("(?s)```\\s*", " ")
                .trim();
        log.debug("[UTILS] stripJsonFences результат: {}", cleaned);
        return cleaned;
    }

    private String normalizeCategory(String raw) {
        log.debug("[UTILS] Вызов normalizeCategory. Исходная строка: '{}'", raw);
        String clean = raw.toLowerCase().trim().replaceAll("[^a-z]", " ");
        if (clean.contains("math") || clean.contains("mixed")) return clean.contains("mixed") ? "mixed" : "math";
        if (clean.contains("food") || clean.contains("meal")) return "food";
        if (clean.contains("noise") || clean.contains("empty") || clean.contains("background")) return "noise";
        if (clean.contains("object") || clean.contains("product")) return "objects";
        if (clean.contains("text")) return "text";
        if (clean.contains("image") || clean.contains("scene") || clean.contains("photo")) return "image";

        log.debug("[UTILS] normalizeCategory не нашел четких совпадений, возвращаем: '{}'", clean);
        return clean;
    }

    private String extractContentFromResponse(Map<String, Object> response) {
        try {
            log.debug("[UTILS] Извлечение content из ответа...");
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            String noThinkContent = content.replaceAll("(?s)<think>.*?</think>", " ").trim();
            log.debug("[UTILS] Извлеченный и очищенный от <think> текст длиной {} символов", noThinkContent.length());
            return noThinkContent;
        } catch (Exception e) {
            log.error("[UTILS] Ошибка парсинга Map ответа в extractContentFromResponse. Тело Map: {}", response, e);
            return "Ошибка при чтении ответа модели.";
        }
    }
}