package com.hotelbooking.booking.service;

import com.hotelbooking.booking.api.ApiModels.UserRequest;
import com.hotelbooking.booking.domain.AppUser;
import com.hotelbooking.booking.domain.Role;
import com.hotelbooking.booking.repo.UserRepository;
import java.util.Locale;
import java.util.NoSuchElementException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Выполняет административное управление учётными записями. */
@Service
public class UserAdminService {
  private final UserRepository users;
  private final PasswordEncoder passwords;

  /** Создаёт сервис управления пользователями. */
  public UserAdminService(UserRepository users, PasswordEncoder passwords) {
    this.users = users;
    this.passwords = passwords;
  }

  /** Создаёт пользователя с хэшированным паролем. */
  @Transactional
  public AppUser create(UserRequest request) {
    if (users.existsByUsername(request.username())) {
      throw new IllegalArgumentException("Username already exists");
    }
    Role role = parseRole(request.role());
    return users.save(new AppUser(request.username(), passwords.encode(request.password()), role));
  }

  /** Обновляет имя, пароль и при наличии значение роли пользователя. */
  @Transactional
  public AppUser update(Long id, UserRequest request) {
    AppUser user =
        users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found"));
    user.setUsername(request.username());
    if (request.password() != null && !request.password().isBlank()) {
      user.setPassword(passwords.encode(request.password()));
    }
    if (request.role() != null) user.setRole(parseRole(request.role()));
    return user;
  }

  /** Удаляет пользователя по идентификатору. */
  @Transactional
  public void delete(Long id) {
    if (!users.existsById(id)) throw new NoSuchElementException("User not found");
    users.deleteById(id);
  }

  /** Преобразует текст роли в enum USER или ADMIN. */
  private Role parseRole(String value) {
    try {
      return value == null ? Role.USER : Role.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Role must be USER or ADMIN", exception);
    }
  }
}
