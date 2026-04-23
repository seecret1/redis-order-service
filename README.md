# redis-order-service

Проект из 2 микросервисов на Spring Boot с доставкой событий заказа через Redis Streams:
- `order-service` создает заказ и публикует событие в stream `orders`.
- `notification-service` читает stream в consumer group, подтверждает успешные сообщения через `XACK`, ретраит неуспешные и отправляет окончательно неуспешные в DLQ (`orders-dlq`).

## Требования
- JDK 17
- Docker + Docker Compose
- (Опционально) `curl` для ручной проверки API

## Быстрый старт
1. Поднять инфраструктуру:
   ```bash
   docker compose up -d redis postgres
   ```
  Опционально, если нужен RedisJSON (Redis Stack):
  ```bash
  docker compose --profile redisjson up -d redis-stack postgres
  ```
2. Запустить сервисы в отдельных терминалах:
   ```bash
   ./gradlew :order-service:bootRun
   ./gradlew :notification-service:bootRun
   ```
3. Отправить тестовый заказ:
   ```bash
   curl -X POST http://localhost:8081/api/orders \
     -H "Content-Type: application/json" \
     -d '{
       "userId": "user-42",
       "productCode": "SKU-100",
       "quantity": 2,
       "totalPrice": 1499.90
     }'
   ```

## Примеры запросов и ответов

### 1) Успешное создание заказа
Запрос:
```http
POST /api/orders HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "userId": "user-42",
  "productCode": "SKU-100",
  "quantity": 2,
  "totalPrice": 1499.90
}
```

Ответ (пример):
```json
{
  "orderId": "c4f1fc2b-4010-4d7f-ae31-5d887823ea88",
  "userId": "user-42",
  "productCode": "SKU-100",
  "quantity": 2,
  "totalPrice": 1499.90,
  "timestamp": "2026-04-23T09:11:22.482287900Z"
}
```

### 2) Ошибка валидации
Запрос:
```http
POST /api/orders HTTP/1.1
Host: localhost:8081
Content-Type: application/json

{
  "userId": "",
  "productCode": "",
  "quantity": 0,
  "totalPrice": -10
}
```

Ответ (пример):
```json
{
  "status": 400,
  "message": "Validation failed",
  "errors": {
    "userId": "must not be blank",
    "productCode": "must not be blank",
    "quantity": "must be greater than or equal to 1",
    "totalPrice": "must be greater than 0"
  },
  "timestamp": "2026-04-23T09:20:01.545799Z"
}
```

## Надежная доставка
- `notification-service` читает stream `orders` в consumer group.
- Успешная обработка: `XACK`.
- Ошибка обработки: сообщение остается в `PENDING`.
- Планировщик `RetryScheduler` сканирует pending, повторно вызывает обработчик, применяя backoff.
- После исчерпания лимита `countRetry` сообщение переносится в DLQ `orders-dlq` и подтверждается (`XACK`).

## Интеграционный тест (Testcontainers)
В `notification-service` добавлен тест `RedisStreamIntegrationTest`, который:
- поднимает Redis в контейнере,
- публикует заведомо невалидное событие (quantity > limit),
- проверяет, что сообщение после ретраев попадает в `orders-dlq`.

Запуск:
```bash
./gradlew :notification-service:test --tests "*RedisStreamIntegrationTest"
```

## Теоретические вопросы

### 1) В чем разница между Pub/Sub и Streams в Redis? Когда что использовать?
- Pub/Sub: fire-and-forget, нет хранения сообщений, нет подтверждений, если подписчик был офлайн - сообщение потеряно.
- Streams: сообщения сохраняются, есть ID, чтение с позиции, consumer groups, `PENDING`, подтверждения (`XACK`).
- Использовать Pub/Sub для эфемерных уведомлений в реальном времени без требований надежной доставки.
- Использовать Streams для очередей задач и событий, где нужна надежность, повторная обработка и аудит.

### 2) Что такое consumer group в Redis Streams? Как она связана с LAST_ID и >
- Consumer group - это механизм конкурентного чтения одного stream группой потребителей без дублирования обработки.
- Для группы Redis хранит `LAST_ID` (последний выданный ID группе).
- `>` в `XREADGROUP` означает: выдать только новые сообщения, которые еще не доставлялись ни одному consumer этой группы.
- Чтение конкретного ID (или `0`) позволяет поднимать историю/pending.

### 3) Как работает механизм PENDING и XACK? Зачем они нужны для надежной доставки?
- После выдачи сообщения через `XREADGROUP` оно попадает в список `PENDING` группы до подтверждения.
- `XACK` удаляет сообщение из `PENDING`, фиксируя успешную обработку.
- Если consumer упал до `XACK`, сообщение остается в pending и может быть найдено/перераспределено (например, через scheduler/claim/retry).
- Это дает at-least-once доставку и предотвращает потерю сообщений.

### 4) Как реализовать идемпотентную обработку при Redis Streams?
Кратко:
- хранить ключ обработанного события (`eventId`) в Redis/БД с TTL или уникальным индексом,
- перед обработкой проверять, не был ли `eventId` уже обработан,
- бизнес-операции делать атомарно (или через транзакцию),
- только после успеха делать `XACK`.

### 5) Способы сериализации объектов в Spring Data Redis. Что выбрано и почему?
Основные варианты:
- String (ручное преобразование полей),
- JDK Serialization,
- JSON (Jackson serializer),
- Hash mapping (`HashMapper`).

В этом проекте выбрана передача полей как `Map<String, String>` в Redis Stream.
Причины:
- простая и прозрачная структура сообщения,
- легко читать/дебажить через Redis CLI,
- нет жесткой зависимости от формата классов и версий Java сериализации,
- достаточно для данного учебного кейса.
