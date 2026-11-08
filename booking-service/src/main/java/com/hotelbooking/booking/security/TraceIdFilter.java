package com.hotelbooking.booking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Добавляет traceId в журналы Booking Service и ответ клиенту. */
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {
  /** Обрабатывает HTTP-запрос и очищает MDC после завершения обработки. */
  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String traceId = request.getHeader("X-Trace-Id");
    if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();
    MDC.put("traceId", traceId);
    response.setHeader("X-Trace-Id", traceId);
    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove("traceId");
    }
  }
}
