# ⚙️ MultiCam API — Backend

![Java](https://img.shields.io/badge/Language-Java%2017-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Framework-Spring%20Boot%203.2-green?logo=springboot)
![WebFlux](https://img.shields.io/badge/Stack-WebFlux%20%2B%20R2DBC-blue)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL-blue?logo=postgresql)

Реактивный REST API для обработки изображений с помощью LLM. Принимает фото, классифицирует содержимое и возвращает структурированный ответ (решение задачи, КБЖУ, список объектов и т.д.).

---

## ✨ Возможности

- **Классификация изображений** — автоматически определяет тип (математика, физика, химия, еда, объект, текст, изображение)
- **Мультимодельный пайплайн** — vision-модель для распознавания, текстовая модель для решения
- **Решение задач** — пошаговые решения по математике, физике, химии на русском языке с LaTeX
- **Анализ питания** — КБЖУ для блюд
- **Детекция объектов** — bbox-координаты + ссылки на маркетплейсы
- **JWT-аутентификация** — access + refresh токены с ротацией
- **Гостевой режим** — вход по UUID устройства без регистрации
- **Апгрейд аккаунта** — конвертация гостя с сохранением всех данных
- **Избранное** — сохранение и синхронизация результатов

---

## 🛠 Технологический стек

| Слой | Технология |
|------|-----------|
| Framework | Spring Boot 3.2 |
| Web | Spring WebFlux (реактивный) |
| База данных | PostgreSQL + Spring Data R2DBC |
| Безопасность | Spring Security + JWT (jjwt 0.12) |
| HTTP-клиент | WebClient (Reactor Netty) |
| Сборка | Maven |
| Хостинг | Railway |

---

## 🚀 Запуск

### Требования

- JDK 17+
- Maven 3.9+
- PostgreSQL 14+

### Локальный запуск

```bash
git clone https://github.com/your-username/multicam-api.git
cd multicam-api
```

Создать файл `src/main/resources/application.yml`:

```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/multicam
    username: your_user
    password: your_password
  sql:
    init:
      mode: always

app:
  secret: your-jwt-secret-at-least-32-chars-long
  lifetime: 900000   # 15 минут в миллисекундах

llm:
  api:
    base-url: https://api.your-llm-provider.com/v1
    model: gpt-4o-mini
    temperature: 0.1

deepseek:
  api:
    key: your-api-key
    base-url: https://api.your-deepseek-provider.com/v1
    model: deepseek-chat
```

```bash
./mvnw spring-boot:run
```

API будет доступен на `http://localhost:8080`.

### Docker (опционально)

```bash
./mvnw clean package -DskipTests
docker build -t multicam-api .
docker run -p 8080:8080 --env-file .env multicam-api
```

---

## 📁 Структура проекта

```
src/main/java/ru/hotdog/multicam_api/
├── config/
│   ├── AppConfig.java          # BCryptPasswordEncoder bean
│   ├── SecurityConfig.java     # CORS, JWT-фильтр, правила доступа
│   └── WebClientConfig.java    # WebClient.Builder bean
├── controller/
│   ├── AuthController.java     # /auth/** — регистрация, вход, refresh, upgrade
│   ├── OCRController.java      # /api/ocr/process — обработка изображений
│   └── SavedResult.java        # /api/save/** — управление избранным
├── dto/
│   ├── OCRResponse.java        # Главный DTO ответа (все типы результатов)
│   ├── TokenPair.java          # Access + Refresh токены
│   ├── Signup/Signin.java      # Данные для входа/регистрации
│   └── ...
├── entity/
│   ├── UserEntity.java         # Таблица users
│   ├── RefreshTokenEntity.java # Таблица refresh_tokens
│   └── SaveResultEntity.java   # Таблица saved_result
├── repository/
│   ├── UserRepo.java
│   ├── RefreshTokenRepo.java
│   └── SaveResultRepo.java
├── security/
│   ├── JwtConfig.java          # Генерация и валидация JWT
│   └── JwtWebFilter.java       # WebFilter: извлекает JWT из заголовка
└── service/
    ├── AuthService.java         # Логика аутентификации
    ├── UserService.java         # Работа с пользователями и лайками
    ├── OCRService.java          # Главный пайплайн обработки изображений
    ├── ObjectFilterService.java # Фильтрация шумовых объектов
    └── ProductSearchService.java # Генерация ссылок на маркетплейсы
```

---

## 🔌 API Endpoints

### Аутентификация (`/auth/**` — публичные)

| Метод | URL | Тело | Описание |
|-------|-----|------|----------|
| POST | `/auth/signup/save` | `{name, email, password}` | Регистрация |
| POST | `/auth/signin` | `{email, password}` | Вход |
| POST | `/auth/signup/guest` | `{uuid}` | Гостевой вход |
| POST | `/auth/refresh` | `{refreshToken}` | Обновление токена |
| POST | `/auth/upgrade` | `{name, email, password}` | Апгрейд гостя (требует Bearer) |

### OCR (требует Bearer-токен)

| Метод | URL | Тело | Описание |
|-------|-----|------|----------|
| POST | `/api/ocr/process` | `multipart: image` | Обработка изображения |

### Избранное (требует Bearer-токен)

| Метод | URL | Тело | Описание |
|-------|-----|------|----------|
| POST | `/api/save/like` | `{imageUrl?, clientJson, category}` | Сохранить результат |
| DELETE | `/api/save/like/{id}` | — | Удалить из избранного |
| GET | `/api/save/likes/all` | — | Получить все лайки |

### Пример ответа `/api/ocr/process`

```json
{
  "tag": "food",
  "calories": 450,
  "proteins": 32,
  "fats": 18,
  "carbs": 41
}
```

```json
{
  "tag": "math",
  "result": "### Анализ задачи\n...\n### Ответ\n$$\\boxed{x = 3}$$"
}
```

```json
{
  "tag": "objects",
  "detectedObjs": [
    { "label": "wireless headphones", "bbox": {"x": 0.2, "y": 0.1, "width": 0.4, "height": 0.5} }
  ],
  "searchResults": [
    { "marketplace": "Wildberries", "url": "https://...", "icon": "🟣" },
    { "marketplace": "Ozon", "url": "https://...", "icon": "🔵" }
  ]
}
```

---

## 🧠 Пайплайн обработки изображений

```
Входящее изображение (JPEG bytes)
         │
         ▼
  [1] Классификатор (vision LLM)
      → math / physics / chemistry /
        text / food / objects / image / noise
         │
         ▼
  [2] Роутер по категориям
         │
    ┌────┴────┬──────────┬──────────┬────────┐
    ▼         ▼          ▼          ▼        ▼
  math     physics   chemistry   food    objects
    │         │          │          │        │
  OCR→    Решение    Решение    JSON    Детекция
  Solve   (vision)   (vision)   КБЖУ   + Ссылки
    │
  Решение
  (текст)
```

**Двухшаговый пайплайн для математики:**
1. Vision-модель видит изображение и транскрибирует формулы в LaTeX (без решения)
2. Текстовая модель решает задачу пошагово на русском языке

---

## 🗄 Схема базы данных

```sql
users
  id            BIGSERIAL PRIMARY KEY
  username      VARCHAR(255)
  hashed_password VARCHAR(255)
  email         VARCHAR(255) UNIQUE
  is_guest      BOOL DEFAULT FALSE
  created_at    TIMESTAMP

saved_result
  id            BIGSERIAL PRIMARY KEY
  image_url     VARCHAR(1024)
  json_data     TEXT              -- сериализованный OCRResponse
  category      VARCHAR(100)
  user_id       BIGINT → users(id) ON DELETE CASCADE
  created_at    TIMESTAMP

refresh_tokens
  id            BIGSERIAL PRIMARY KEY
  user_id       BIGINT → users(id) ON DELETE CASCADE
  token         VARCHAR(512) UNIQUE
  expires_at    TIMESTAMP         -- +30 дней от создания
  created_at    TIMESTAMP
```

---

## 🔐 Безопасность

- Пароли хэшируются через **BCrypt** (соль встроена в хэш)
- **JWT access-токены** подписаны HMAC-SHA256
- **Refresh-токены** ротируются при каждом использовании (использованный токен немедленно удаляется)
- Все эндпоинты кроме `/auth/**` требуют валидный Bearer-токен
- Удаление лайков проверяет `userId` — нельзя удалить чужую запись
- CORS открыт для мобильного клиента (`allowedOrigins("*")`)

---

## ⚙️ Переменные окружения

| Переменная | Описание | Пример |
|------------|----------|--------|
| `SPRING_R2DBC_URL` | URL базы данных | `r2dbc:postgresql://host:5432/db` |
| `SPRING_R2DBC_USERNAME` | Пользователь БД | `postgres` |
| `SPRING_R2DBC_PASSWORD` | Пароль БД | `secret` |
| `APP_SECRET` | JWT секрет (мин. 32 символа) | `my-super-secret-key-32chars` |
| `APP_LIFETIME` | Время жизни access-токена (мс) | `900000` |
| `LLM_API_BASE_URL` | URL vision-модели | `https://api.provider.com/v1` |
| `LLM_API_MODEL` | Имя vision-модели | `gpt-4o-mini` |
| `LLM_API_TEMPERATURE` | Температура модели | `0.1` |
| `DEEPSEEK_API_KEY` | API-ключ | `sk-...` |
| `DEEPSEEK_API_BASE_URL` | URL текстовой модели | `https://api.deepseek.com/v1` |
| `DEEPSEEK_API_MODEL` | Имя текстовой модели | `deepseek-chat` |
| `JAVA_OPTS` | JVM-флаги для Railway | см. ниже |

### Рекомендуемые JAVA_OPTS для Railway Free Tier

```
-Xmx300m -Xms100m -XX:+UseSerialGC -XX:MaxMetaspaceSize=80m -Dio.netty.maxDirectMemory=209715200
```

---

## 📄 Лицензия

MIT License — см. файл [LICENSE](LICENSE)
