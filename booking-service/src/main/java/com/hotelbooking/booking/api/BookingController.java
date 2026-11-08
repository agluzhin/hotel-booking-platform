package com.hotelbooking.booking.api;

import com.hotelbooking.booking.api.ApiModels.BookingRequest;
import com.hotelbooking.booking.api.ApiModels.BookingResponse;
import com.hotelbooking.booking.service.BookingService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API создания, чтения и отмены бронирований. */
@RestController
@RequestMapping("/api")
public class BookingController {
  private final BookingService service;

  /** Создаёт контроллер бронирований. */
  public BookingController(BookingService service) {
    this.service = service;
  }

  /** Создаёт бронирование от имени текущего пользователя. */
  @PostMapping("/booking")
  public BookingResponse create(@Valid @RequestBody BookingRequest request, Authentication auth) {
    return service.create(auth.getName(), request);
  }

  /** Возвращает бронирование его владельцу или администратору. */
  @GetMapping("/booking/{id}")
  public BookingResponse get(@PathVariable Long id, Authentication auth) {
    return service.get(id, auth.getName(), isAdmin(auth));
  }

  /** Отменяет бронирование его владельца или администратора. */
  @DeleteMapping("/booking/{id}")
  public BookingResponse cancel(@PathVariable Long id, Authentication auth) {
    return service.cancel(id, auth.getName(), isAdmin(auth));
  }

  /** Возвращает историю бронирований текущего пользователя с пагинацией. */
  @GetMapping("/bookings")
  public Page<BookingResponse> history(
      Authentication auth,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable =
        PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending());
    return service.history(auth.getName(), pageable);
  }

  /** Проверяет, содержит ли текущий principal роль администратора. */
  private boolean isAdmin(Authentication auth) {
    return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
  }
}
