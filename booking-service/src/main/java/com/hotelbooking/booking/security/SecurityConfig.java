package com.hotelbooking.booking.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/** Настраивает аутентификацию JWT и права доступа Booking Service. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  /** Создаёт HMAC-ключ из настройки security.jwt.secret. */
  @Bean
  SecretKeySpec jwtKey(@Value("${security.jwt.secret}") String secret) {
    return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
  }

  /** Создаёт декодер, проверяющий подпись входящего JWT. */
  @Bean
  JwtDecoder jwtDecoder(SecretKeySpec key) {
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /** Создаёт кодировщик JWT для выпуска токенов при входе. */
  @Bean
  JwtEncoder jwtEncoder(SecretKeySpec key) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(key));
  }

  /** Преобразует роли из claims JWT в authorities Spring Security. */
  @Bean
  Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthConverter() {
    return jwt ->
        new JwtAuthenticationToken(
            jwt,
            jwt.getClaimAsStringList("roles").stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()),
            jwt.getSubject());
  }

  /** Настраивает публичные маршруты и проверку JWT на остальных маршрутах. */
  @Bean
  SecurityFilterChain security(
      HttpSecurity http, Converter<Jwt, ? extends AbstractAuthenticationToken> converter)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/user/register",
                        "/api/user/auth",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/actuator/health")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
        .build();
  }
}
