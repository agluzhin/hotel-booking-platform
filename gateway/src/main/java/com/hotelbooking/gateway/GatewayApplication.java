package com.hotelbooking.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа API Gateway. */
@SpringBootApplication
public class GatewayApplication {
  /** Запускает API Gateway. */
  public static void main(String[] args) {
    SpringApplication.run(GatewayApplication.class, args);
  }
}
