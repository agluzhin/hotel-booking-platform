package com.hotelbooking.booking.client;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Выполняет REST-вызовы Booking Service к Hotel Service. */
@Component
public class HotelClient {
  private final RestClient client;
  private final int attempts;
  private final long retryDelayMs;
  private final String internalKey;

  /** Создаёт HTTP-клиент с тайм-аутом чтения и подключением через Eureka. */
  public HotelClient(
      RestClient.Builder builder,
      @Value("${hotel.base-url}") String baseUrl,
      @Value("${hotel.timeout-ms}") long timeoutMs,
      @Value("${hotel.max-attempts}") int attempts,
      @Value("${hotel.retry-delay-ms}") long retryDelayMs,
      @Value("${hotel.internal-key}") String internalKey) {
    JdkClientHttpRequestFactory factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build());
    factory.setReadTimeout(Duration.ofMillis(timeoutMs));
    this.client = builder.requestFactory(factory).baseUrl(baseUrl).build();
    this.attempts = attempts;
    this.retryDelayMs = retryDelayMs;
    this.internalKey = internalKey;
  }

  /** Запрос для внутренних операций с занятостью номера. */
  public record AvailabilityRequest(LocalDate startDate, LocalDate endDate, String requestId) {}

  /** DTO номера, возвращаемое Hotel Service. */
  public record RoomDto(
      Long id, Long hotelId, String number, boolean available, long timesBooked) {}

  /** Подтверждает доступность номера на заданный период. */
  public void confirm(Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
    call(
        "/api/rooms/{id}/confirm-availability",
        roomId,
        new AvailabilityRequest(startDate, endDate, requestId));
  }

  /** Освобождает период номера при компенсации или отмене бронирования. */
  public void release(Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
    call("/api/rooms/{id}/release", roomId, new AvailabilityRequest(startDate, endDate, requestId));
  }

  /** Получает номера, упорядоченные Hotel Service по возрастанию загрузки. */
  public List<RoomDto> recommend(LocalDate startDate, LocalDate endDate) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        var request =
            client
                .get()
                .uri(
                    uri ->
                        uri.path("/api/rooms/recommend")
                            .queryParam("startDate", startDate)
                            .queryParam("endDate", endDate)
                            .build());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwt) {
          request.header("Authorization", "Bearer " + jwt.getToken().getTokenValue());
        }
        addTraceId(request);
        RoomDto[] rooms = request.retrieve().body(RoomDto[].class);
        return rooms == null ? List.of() : Arrays.asList(rooms);
      } catch (RestClientException exception) {
        lastFailure = exception;
        if (attempt >= attempts || !isRetryable(exception)) throw exception;
        pause(attempt);
      }
    }
    throw lastFailure == null
        ? new IllegalStateException("Recommendation request failed")
        : lastFailure;
  }

  /** Добавляет traceId текущего запроса в исходящий заголовок. */
  private void addTraceId(RestClient.RequestHeadersSpec<?> request) {
    String traceId = MDC.get("traceId");
    if (traceId != null) request.header("X-Trace-Id", traceId);
  }

  /** Повторяет сетевые ошибки и ответы 5xx, но не повторы клиентских 4xx. */
  private boolean isRetryable(RestClientException exception) {
    return !(exception instanceof HttpStatusCodeException status)
        || status.getStatusCode().is5xxServerError();
  }

  /** Выдерживает экспоненциальную задержку перед новой попыткой. */
  private void pause(int attempt) {
    try {
      Thread.sleep(retryDelayMs * (1L << (attempt - 1)));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Retry wait was interrupted", exception);
    }
  }

  /** Отправляет внутреннюю POST-команду с ключом сервиса и ограниченными повторами. */
  private void call(String path, Long roomId, AvailabilityRequest body) {
    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= attempts; attempt++) {
      try {
        client
            .post()
            .uri(path, roomId)
            .header("X-Internal-Key", internalKey)
            .headers(
                headers -> {
                  String traceId = MDC.get("traceId");
                  if (traceId != null) headers.set("X-Trace-Id", traceId);
                })
            .body(body)
            .retrieve()
            .toBodilessEntity();
        return;
      } catch (RestClientException exception) {
        lastFailure = exception;
        if (attempt >= attempts || !isRetryable(exception)) throw exception;
        pause(attempt);
      }
    }
    throw lastFailure == null
        ? new IllegalStateException("Hotel Service request failed")
        : lastFailure;
  }
}
