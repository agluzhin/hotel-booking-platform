package com.hotelbooking.hotel.api;

import java.time.Instant;
import java.util.NoSuchElementException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Преобразует ошибки Hotel Service в единый формат HTTP-ответа. */
@RestControllerAdvice
public class HotelExceptionHandler {
  /** Единая DTO ошибки Hotel Service. */
  public record Error(String code, String message, Instant timestamp) {}

  /** Преобразует некорректные данные в HTTP 400. */
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Error> badRequest(IllegalArgumentException exception) {
    return response(HttpStatus.BAD_REQUEST, "BAD_REQUEST", exception.getMessage());
  }

  /** Возвращает HTTP 403, если внутренний запрос не прошёл проверку ключа. */
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Error> forbidden(AccessDeniedException exception) {
    return response(HttpStatus.FORBIDDEN, "FORBIDDEN", exception.getMessage());
  }

  /** Преобразует конфликт занятости в HTTP 409. */
  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<Error> conflict(IllegalStateException exception) {
    return response(HttpStatus.CONFLICT, "CONFLICT", exception.getMessage());
  }

  /** Преобразует нарушение уникальности номера комнаты в HTTP 409. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Error> duplicate(DataIntegrityViolationException exception) {
    return response(HttpStatus.CONFLICT, "CONFLICT", "Room number already exists in this hotel");
  }

  /** Преобразует отсутствие сущности в HTTP 404. */
  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<Error> notFound(NoSuchElementException exception) {
    return response(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
  }

  /** Формирует общий объект ответа с кодом, описанием и временем. */
  private ResponseEntity<Error> response(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new Error(code, message, Instant.now()));
  }
}
