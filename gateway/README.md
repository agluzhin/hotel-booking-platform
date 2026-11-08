# API Gateway

Spring Cloud Gateway — единая внешняя точка входа на `http://localhost:8080`. Маршруты используют `lb://...`, поэтому адреса экземпляров берутся из Eureka. Gateway передаёт `Authorization` backend-сервису; JWT и роли проверяют сами backend-сервисы.

## Маршруты

| Внешний путь | HTTP-методы | Сервис | Назначение |
|---|---|---|---|
| `/api/booking/**`, `/api/bookings/**`, `/api/user/**` | все | `booking-service` | регистрация, пользователи и бронирования |
| `/api/hotels/**` | все | `hotel-service` | гостиницы |
| `/api/rooms/**` | `GET` | `hotel-service` | просмотр и рекомендации свободных номеров |
| `/api/rooms`, `/api/rooms/{id}`, `/api/rooms/{id}/availability` | `POST`, `PATCH`, `DELETE` | `hotel-service` | административное управление номерами |

Пути `/api/rooms/{id}/confirm-availability` и `/api/rooms/{id}/release` намеренно не входят в маршруты Gateway. Это внутренние команды Booking Service → Hotel Service, защищённые `X-Internal-Key`.

## Сквозная трассировка

Глобальный фильтр читает `X-Trace-Id`, сохраняет переданное значение или генерирует UUID, передаёт его backend-сервису и добавляет в ответ. Для бизнес-корреляции сага дополнительно пишет `bookingId` и `requestId` в логи.

## Запуск и ручная проверка

Сначала должны быть запущены Eureka и оба backend-сервиса. Общая инструкция находится в [корневом README](../README.md).

```bash
mvn -pl gateway spring-boot:run
curl -i http://localhost:8080/api/hotels
```

Запрос без JWT должен получить `401`, когда Hotel Service уже зарегистрирован. После входа и передачи `Authorization: Bearer <JWT>` запрос должен пройти к Hotel Service:

```bash
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/hotels
```

Проверка внутреннего маршрута: через Gateway запрос `POST /api/rooms/1/release` не должен маршрутизироваться в Hotel Service. Внутренние команды выполняет Booking Service напрямую через Eureka.

Gateway не содержит отдельного набора модульных тестов. Проверяйте маршруты ручными запросами и запускайте весь Maven-набор через `mvn clean verify` из корня проекта.
