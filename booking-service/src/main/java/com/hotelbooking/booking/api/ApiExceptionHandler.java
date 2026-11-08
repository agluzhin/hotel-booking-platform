package com.hotelbooking.booking.api;

import com.hotelbooking.booking.api.ApiModels.ErrorResponse;
import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Преобразует исключения Booking Service в единый формат ответа API. */
@RestControllerAdvice
public class ApiExceptionHandler {
  /** Возвращает HTTP 400 при некорректных входных данных. */
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException exception) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
  }

  /** Возвращает HTTP 401 при неверных данных аутентификации. */
  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<ErrorResponse> unauthenticated(AuthenticationException exception) {
    return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", exception.getMessage());
  }

  /** Возвращает HTTP 403 при попытке доступа к чужому бронированию. */
  @ExceptionHandler(SecurityException.class)
  ResponseEntity<ErrorResponse> forbidden(SecurityException exception) {
    return error(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
  }

  /** Возвращает HTTP 404, если запрошенная сущность отсутствует. */
  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<ErrorResponse> missing(NoSuchElementException exception) {
    return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
  }

  /** Возвращает HTTP 409 при конфликте бизнес-операции. */
  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ErrorResponse> conflict(IllegalStateException exception) {
    return error(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
  }

  /** Возвращает HTTP 400 при нарушении Bean Validation. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException exception) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
  }

  /** Создаёт DTO ошибки с временем возникновения. */
  private ResponseEntity<ErrorResponse> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ErrorResponse(code, message, Instant.now()));
  }
}
