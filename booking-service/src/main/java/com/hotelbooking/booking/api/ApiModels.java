package com.hotelbooking.booking.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** DTO запросов и ответов Booking Service. */
public final class ApiModels {
  /** Запрещает создание экземпляров утилитного контейнера DTO. */
  private ApiModels() {}

  /** Данные регистрации пользователя. */
  public record RegisterRequest(
      @NotBlank @Size(max = 80) String username,
      @NotBlank @Size(min = 8, max = 100) String password) {}

  /** Учётные данные для входа. */
  public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

  /** JWT и его свойства для клиента. */
  public record TokenResponse(String accessToken, String tokenType, long expiresIn) {}

  /** Данные для создания бронирования. */
  public record BookingRequest(
      Long roomId,
      boolean autoSelect,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      @NotBlank @Size(max = 80) String requestId) {}

  /** Представление бронирования в API. */
  public record BookingResponse(
      Long id,
      String username,
      Long roomId,
      LocalDate startDate,
      LocalDate endDate,
      String status,
      Instant createdAt,
      String requestId) {}

  /** Данные для административного создания или изменения пользователя. */
  public record UserRequest(
      @NotBlank @Size(max = 80) String username,
      @Size(min = 8, max = 100) String password,
      String role) {}

  /** Единый ответ API об ошибке. */
  public record ErrorResponse(String code, String message, Instant timestamp) {}
}
