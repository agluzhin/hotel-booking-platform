package com.hotelbooking.booking.api;

import com.hotelbooking.booking.api.ApiModels.LoginRequest;
import com.hotelbooking.booking.api.ApiModels.RegisterRequest;
import com.hotelbooking.booking.api.ApiModels.TokenResponse;
import com.hotelbooking.booking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-контроллер регистрации и аутентификации пользователя. */
@RestController
@RequestMapping("/api/user")
public class AuthController {
  private final AuthService authService;

  /** Создаёт контроллер с сервисом аутентификации. */
  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /** Регистрирует пользователя и выдаёт JWT. */
  @PostMapping("/register")
  public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  /** Проверяет имя и пароль и выдаёт JWT. */
  @PostMapping("/auth")
  public TokenResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
