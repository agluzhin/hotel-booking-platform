package com.hotelbooking.booking.service;

import com.hotelbooking.booking.domain.AppUser;
import com.hotelbooking.booking.domain.Role;
import com.hotelbooking.booking.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Создаёт демонстрационные пользовательские учётные записи. */
@Component
public class DataSeeder implements CommandLineRunner {
  private final UserRepository users;
  private final PasswordEncoder passwords;

  /** Создаёт загрузчик демоданных. */
  public DataSeeder(UserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  /** Добавляет администратора и демонстрационного пользователя в пустую базу. */
  @Override
  public void run(String... args) {
    if (!users.existsByUsername("admin")) {
      users.save(new AppUser("admin", passwords.encode("admin12345"), Role.ADMIN));
    }
    if (!users.existsByUsername("demo")) {
      users.save(new AppUser("demo", passwords.encode("demo12345"), Role.USER));
    }
  }
}
