package com.hotelbooking.booking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Настройка безопасного хранения паролей. */
@Configuration
public class PasswordConfig {
  /** Создаёт BCrypt-кодировщик паролей. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
