package com.hotelbooking.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/** Точка входа сервера регистрации и обнаружения микросервисов. */
@SpringBootApplication
@EnableEurekaServer
public class EurekaApplication {
  /** Запускает Eureka Server. */
  public static void main(String[] args) {
    SpringApplication.run(EurekaApplication.class, args);
  }
}
