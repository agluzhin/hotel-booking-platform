package com.hotelbooking.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа Hotel Service. */
@SpringBootApplication
public class HotelApplication {
  /** Запускает Hotel Service. */
  public static void main(String[] args) {
    SpringApplication.run(HotelApplication.class, args);
  }
}
