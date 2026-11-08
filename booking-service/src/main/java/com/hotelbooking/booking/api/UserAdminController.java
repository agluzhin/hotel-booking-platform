package com.hotelbooking.booking.api;

import com.hotelbooking.booking.api.ApiModels.UserRequest;
import com.hotelbooking.booking.domain.AppUser;
import com.hotelbooking.booking.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-контроллер административного управления пользователями. */
@RestController
@RequestMapping("/api/user")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
  private final UserAdminService service;

  /** Безопасное представление пользователя без хэша пароля. */
  public record UserView(Long id, String username, String role) {}

  /** Создаёт контроллер управления пользователями. */
  public UserAdminController(UserAdminService service) {
    this.service = service;
  }

  /** Создаёт пользователя с указанной ролью. */
  @PostMapping
  public UserView create(@Valid @RequestBody UserRequest request) {
    return view(service.create(request));
  }

  /** Обновляет данные пользователя. */
  @PatchMapping("/{id}")
  public UserView update(@PathVariable Long id, @Valid @RequestBody UserRequest request) {
    return view(service.update(id, request));
  }

  /** Удаляет учётную запись пользователя. */
  @DeleteMapping("/{id}")
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  /** Преобразует сущность в безопасное представление API. */
  private UserView view(AppUser user) {
    return new UserView(user.getId(), user.getUsername(), user.getRole().name());
  }
}
