package com.hotelbooking.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Точка входа Booking Service. */
@SpringBootApplication
public class BookingApplication {
  /** Запускает Booking Service. */
  public static void main(String[] args) {
    SpringApplication.run(BookingApplication.class, args);
  }
}
