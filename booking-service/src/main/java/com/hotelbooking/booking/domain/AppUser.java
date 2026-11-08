package com.hotelbooking.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** JPA-сущность пользователя и его роли. */
@Entity
@Table(
    name = "users",
    indexes = @Index(name = "uk_users_username", columnList = "username", unique = true))
public class AppUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 80)
  private String username;

  @Column(nullable = false)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private Role role = Role.USER;

  /** Конструктор JPA для загрузки сущности. */
  protected AppUser() {}

  /** Создаёт пользователя с заданной ролью. */
  public AppUser(String username, String password, Role role) {
    this.username = username;
    this.password = password;
    this.role = role;
  }

  /** Возвращает идентификатор пользователя. */
  public Long getId() {
    return id;
  }

  /** Возвращает имя учётной записи. */
  public String getUsername() {
    return username;
  }

  /** Возвращает хэш пароля пользователя. */
  public String getPassword() {
    return password;
  }

  /** Возвращает роль пользователя. */
  public Role getRole() {
    return role;
  }

  /** Меняет имя учётной записи. */
  public void setUsername(String username) {
    this.username = username;
  }

  /** Сохраняет новый хэш пароля. */
  public void setPassword(String password) {
    this.password = password;
  }

  /** Меняет роль пользователя. */
  public void setRole(Role role) {
    this.role = role;
  }
}
