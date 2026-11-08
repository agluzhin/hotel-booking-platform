package com.hotelbooking.booking.service;

import com.hotelbooking.booking.api.ApiModels.LoginRequest;
import com.hotelbooking.booking.api.ApiModels.RegisterRequest;
import com.hotelbooking.booking.api.ApiModels.TokenResponse;
import com.hotelbooking.booking.domain.AppUser;
import com.hotelbooking.booking.domain.Role;
import com.hotelbooking.booking.repo.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Реализует регистрацию, вход и выпуск JWT. */
@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtEncoder jwtEncoder;

  /** Создаёт сервис аутентификации. */
  public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder jwtEncoder) {
    this.users = users;
    this.passwords = passwords;
    this.jwtEncoder = jwtEncoder;
  }

  /** Регистрирует обычного пользователя и возвращает токен. */
  @Transactional
  public TokenResponse register(RegisterRequest request) {
    if (users.existsByUsername(request.username())) {
      throw new IllegalArgumentException("Username already exists");
    }
    AppUser user =
        users.save(
            new AppUser(request.username(), passwords.encode(request.password()), Role.USER));
    return createToken(user);
  }

  /** Проверяет учётные данные и возвращает токен. */
  @Transactional(readOnly = true)
  public TokenResponse login(LoginRequest request) {
    AppUser user =
        users
            .findByUsername(request.username())
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
    if (!passwords.matches(request.password(), user.getPassword())) {
      throw new BadCredentialsException("Invalid credentials");
    }
    return createToken(user);
  }

  /** Подписывает JWT с ролью пользователя и сроком действия в один час. */
  private TokenResponse createToken(AppUser user) {
    Instant issuedAt = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer("hotel-booking")
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(3600))
            .subject(user.getUsername())
            .claim("roles", List.of("ROLE_" + user.getRole().name()))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new TokenResponse(token, "Bearer", 3600);
  }
}
