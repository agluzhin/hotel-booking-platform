# Booking Service

Отвечает за пользователей, JWT-аутентификацию, историю бронирований и оркестрацию саги. Порт `8081`, имя Eureka `booking-service`, локальная база `jdbc:h2:mem:bookingdb`.

Общие инструкции по установке, сборке и запуску четырёх процессов — в [корневом README](../README.md). Swagger UI после запуска: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html).

## Доступ и токены

- `POST /api/user/register` и `POST /api/user/auth` доступны без JWT.
- После аутентификации передавайте токен как `Authorization: Bearer <JWT>`.
- Токен живёт 3600 секунд. Claim `roles` содержит `ROLE_USER` или `ROLE_ADMIN`.
- Бронирования пользователя доступны только владельцу; роль `ADMIN` разрешает чтение и отмену любого бронирования.
- Административные пользовательские маршруты требуют `ROLE_ADMIN`.

При запуске создаются демонстрационные пользователи `demo / demo12345` и `admin / admin12345`. Пароли предназначены только для локальной учебной среды.

## Endpoint’ы

### Регистрация и вход

| Метод и путь | Авторизация | Тело | Ответ |
|---|---|---|---|
| `POST /api/user/register` | публичный | `{"username":"newuser","password":"password123"}` | `accessToken`, `tokenType`, `expiresIn` |
| `POST /api/user/auth` | публичный | `{"username":"demo","password":"demo12345"}` | `accessToken`, `tokenType`, `expiresIn` |

Пароль при регистрации должен содержать не менее 8 символов. Повтор имени пользователя отклоняется.

### Администрирование пользователей

| Метод и путь | Роль | Назначение |
|---|---|---|
| `POST /api/user` | ADMIN | Создать пользователя; поля `username`, `password`, необязательная `role` (`USER`/`ADMIN`) |
| `PATCH /api/user/{id}` | ADMIN | Изменить имя, пароль и/или роль пользователя |
| `DELETE /api/user/{id}` | ADMIN | Удалить пользователя |

Пароль в ответе API не возвращается. При PATCH поле `password` можно не передавать, тогда пароль не меняется.

### Бронирования

| Метод и путь | Роль | Назначение |
|---|---|---|
| `POST /api/booking` | USER | Создать бронирование с `roomId` или автоматическим подбором |
| `GET /api/bookings?page=0&size=20` | USER | Получить страницу своей истории, сортировка по времени создания от новых к старым |
| `GET /api/booking/{id}` | владелец или ADMIN | Получить детали бронирования |
| `DELETE /api/booking/{id}` | владелец или ADMIN | Отменить бронирование и освободить занятый слот |

Тело POST:

```json
{
  "roomId": 1,
  "autoSelect": false,
  "startDate": "2027-01-10",
  "endDate": "2027-01-12",
  "requestId": "booking-request-001"
}
```

При `autoSelect: true` поле `roomId` игнорируется. В противном случае `roomId` обязателен. `requestId` — уникальный ключ идемпотентности: при повторной доставке того же запроса сервис возвращает существующую запись, не создавая дубликат. Заезд должен быть в будущем, выезд — позже заезда. Интервал дат полуоткрытый `[startDate, endDate)`.

Успешный ответ содержит `id`, `roomId`, даты и состояние `CONFIRMED`. При невозможности подтверждения слот освобождается компенсацией, а состояние становится `CANCELLED`; клиент получает HTTP `409`.

### Формат ошибок

Ошибки возвращаются как JSON с `code`, `message` и `timestamp`.

| HTTP | Ситуация |
|---:|---|
| `400` | Ошибка валидации, неправильные даты или некорректный запрос |
| `401` | Отсутствующий/недействительный JWT или неверные учётные данные |
| `403` | Пользователь пытается читать/отменить чужое бронирование либо выполнить ADMIN-операцию |
| `404` | Бронирование или пользователь не найден |
| `409` | Бронирование не удалось подтвердить или конфликтует с существующим состоянием |

## Сквозной сценарий проверки через Gateway

Запустите все модули по [общей инструкции](../README.md), затем выполните команды из терминала:

```bash
# Получить токен демо-пользователя
curl -s -X POST http://localhost:8080/api/user/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo12345"}'
```

Скопируйте `accessToken` из ответа в переменную `TOKEN`, затем проверьте список гостиниц, подбор номера и бронирование:

```bash
export TOKEN='<accessToken>'
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/hotels

curl -G -H "Authorization: Bearer $TOKEN" \
  --data-urlencode 'startDate=2027-01-10' \
  --data-urlencode 'endDate=2027-01-12' \
  http://localhost:8080/api/rooms/recommend

curl -i -X POST http://localhost:8080/api/booking \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"autoSelect":true,"startDate":"2027-01-10","endDate":"2027-01-12","requestId":"manual-check-001"}'
```

Повторите последний запрос с тем же `requestId` и убедитесь, что новый объект не создаётся. Проверьте результат через `GET /api/bookings`. Для административных запросов выполните вход с `admin / admin12345` и используйте его токен.

## Автоматические тесты

Из корня проекта:

```bash
mvn -pl booking-service test
```

`BookingServiceTest` проверяет бизнес-логику подтверждения, отказ Hotel Service, тайм-аут удалённого вызова с компенсацией и повторную доставку `requestId`. `BookingApiIntegrationTest` дополнительно проверяет через MockMvc JWT-доступ, HTTP-сценарий саги, идемпотентность и личную историю. Полный набор всех модулей запускается командой `mvn clean verify`.

## Сага и настройки

Booking Service сначала фиксирует `PENDING` в собственной транзакции, потом вызывает Hotel Service с timeout 1500 мс и тремя попытками с экспоненциальной задержкой. Для `confirm` и `release` передаётся `requestId`; ответ `409` не повторяется как временная ошибка, сетевые ошибки и ответы `5xx` повторяются. При успехе запись становится `CONFIRMED`, при неуспехе сервис отправляет компенсационный `release` и переводит запись в `CANCELLED`.

Конфигурация: `booking-service/src/main/resources/application.yml`. Для совместной работы с Hotel Service должны совпадать `JWT_SECRET` и `INTERNAL_SERVICE_KEY`. Логи содержат `traceId`, `bookingId` и `requestId`.
