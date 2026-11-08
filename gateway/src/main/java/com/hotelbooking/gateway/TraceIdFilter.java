package com.hotelbooking.gateway;

import java.util.UUID;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/** Глобальный фильтр для сквозной передачи идентификатора запроса. */
@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
  /** Возвращает порядок выполнения фильтра. */
  @Override
  public int getOrder() {
    return HIGHEST_PRECEDENCE;
  }

  /** Добавляет X-Trace-Id к входящему запросу и исходящему ответу. */
  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String traceId = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
    if (traceId == null || traceId.isBlank()) traceId = UUID.randomUUID().toString();
    ServerWebExchange updated =
        exchange
            .mutate()
            .request(exchange.getRequest().mutate().header("X-Trace-Id", traceId).build())
            .build();
    updated.getResponse().getHeaders().set("X-Trace-Id", traceId);
    return chain.filter(updated);
  }
}
