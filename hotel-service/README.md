# Hotel Service

Владеет гостиницами, номерами и временными слотами занятости. Порт `8082`, имя Eureka `hotel-service`, локальная база `jdbc:h2:mem:hoteldb`.

Общие требования, Maven-зависимости и запуск приведены в [корневом README](../README.md). OpenAPI UI: [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html).

## Безопасность

Пользовательские запросы требуют JWT от Booking Service. GET-маршруты разрешены `USER` и `ADMIN`; изменение гостиниц и номеров ограничено ролью `ADMIN`. Внутренние команды `confirm-availability` и `release` не требуют пользовательского JWT, но обязательно проверяют `X-Internal-Key`. Gateway эти два маршрута не публикует.

При старте создаются гостиницы `Demo Central` и `Demo Riverside` и по пять номеров в каждой. Данные хранятся в H2 только до остановки процесса.

## Публичные endpoint’ы

### Гостиницы

| Метод и путь | Роль | Тело / параметры | Назначение |
|---|---|---|---|
| `GET /api/hotels` | USER/ADMIN | — | Список гостиниц |
| `POST /api/hotels` | ADMIN | `{"name":"City Hotel","address":"Main street 5"}` | Создать гостиницу |
| `PATCH /api/hotels/{id}` | ADMIN | Те же `name`, `address` | Изменить гостиницу |
| `DELETE /api/hotels/{id}` | ADMIN | — | Удалить гостиницу, если в ней нет номеров |

### Номера

| Метод и путь | Роль | Тело / параметры | Назначение |
|---|---|---|---|
| `POST /api/rooms` | ADMIN | `{"hotelId":1,"number":"101"}` | Добавить номер |
| `PUT /api/rooms/{id}` | ADMIN | `{"number":"101A"}` | Изменить номер комнаты в пределах текущей гостиницы |
| `GET /api/rooms?startDate=2027-01-10&endDate=2027-01-12` | USER/ADMIN | Период дат | Все свободные эксплуатируемые номера |
| `GET /api/rooms/recommend?startDate=...&endDate=...` | USER/ADMIN | Период дат | Свободные номера по возрастанию `timesBooked`, затем ID |
| `GET /api/rooms/statistics` | ADMIN | — | Загрузка всех номеров, включая выведенные из эксплуатации; сортировка по убыванию `timesBooked` |
| `PATCH /api/rooms/{id}/availability?available=false` | ADMIN | Query `available` | Вывести номер из эксплуатации или вернуть в неё |
| `DELETE /api/rooms/{id}` | ADMIN | — | Удалить номер без сохранённой истории слотов |

`available` описывает техническую доступность номера и не заменяет календарь занятости. Занятость определяется активными записями `room_slots`. Даты трактуются как `[startDate, endDate)`: соседние периоды, например выезд 12-го и заезд 12-го, не пересекаются.

## Внутренние endpoint’ы Booking Service

Они доступны только при прямом обращении к Hotel Service и не настроены в Gateway.

| Метод и путь | Заголовок | Тело | Поведение |
|---|---|---|---|
| `POST /api/rooms/{id}/confirm-availability` | `X-Internal-Key` | `{"startDate":"2027-01-10","endDate":"2027-01-12","requestId":"booking-request-001"}` | Блокирует строку номера, проверяет пересечение и фиксирует слот |
| `POST /api/rooms/{id}/release` | `X-Internal-Key` | То же тело и исходный `requestId` | Идемпотентно освобождает активный слот |

На один номер применяется `PESSIMISTIC_WRITE`, поэтому параллельные подтверждения на пересекающийся период сериализуются. Повтор `confirm` с теми же номером, датами и `requestId` не создаёт второй слот и не увеличивает счётчик повторно. Повтор `release` не уменьшает счётчик дважды. Использование одного `requestId` с другими датами/номером отклоняется.

## Ручная проверка

Запустите Eureka, Hotel Service, Booking Service и Gateway по общей инструкции. Получите USER-токен по инструкции входа в [Booking Service](../booking-service/README.md#регистрация-и-вход), затем используйте его для публичных запросов:

```bash
curl -s -X POST http://localhost:8080/api/user/auth \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo12345"}'
export TOKEN='<accessToken>'

curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/hotels
curl -G -i -H "Authorization: Bearer $TOKEN" \
  --data-urlencode 'startDate=2027-01-10' \
  --data-urlencode 'endDate=2027-01-12' \
  http://localhost:8080/api/rooms/recommend
```

Для проверки административного endpoint’а получите ADMIN-токен по инструкции входа в [Booking Service](../booking-service/README.md#регистрация-и-вход), затем создайте гостиницу:

```bash
curl -i -X POST http://localhost:8080/api/hotels \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Test Hotel","address":"Test Street 1"}'
```

Внутренние endpoint’ы можно проверить прямым запросом к `localhost:8082`. Значение ключа должно совпасть с `INTERNAL_SERVICE_KEY` в обоих сервисах. Не выполняйте этот сценарий на реальном занятом демо-номере:

```bash
curl -i -X POST http://localhost:8082/api/rooms/1/confirm-availability \
  -H 'X-Internal-Key: local-internal-key' -H 'Content-Type: application/json' \
  -d '{"startDate":"2027-01-10","endDate":"2027-01-12","requestId":"hotel-manual-001"}'

curl -i -X POST http://localhost:8082/api/rooms/1/release \
  -H 'X-Internal-Key: local-internal-key' -H 'Content-Type: application/json' \
  -d '{"startDate":"2027-01-10","endDate":"2027-01-12","requestId":"hotel-manual-001"}'
```

## Ответы и ошибки

- `200` — успешные GET/PATCH/POST, включая повтор идемпотентной команды.
- `200` — успешные GET/PATCH и создание через POST в текущей реализации.
- `400` — некорректные даты или параметры.
- `401` — отсутствующий/недействительный JWT на публичном маршруте.
- `403` — нет роли ADMIN либо неверен внутренний ключ.
- `404` — гостиница/номер отсутствует.
- `409` — номер занят на даты, повторный ключ использован для другого запроса или сущность нельзя безопасно удалить.

Ошибки имеют поля `code`, `message`, `timestamp`. MockMvc-тесты модуля проверяют административный доступ и обновление номера, идемпотентность внутренних операций и отказ одной из параллельных пересекающихся броней. Запуск тестов из корня проекта: `mvn -pl hotel-service test`.
