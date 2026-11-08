package com.hotelbooking.hotel.security;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/** Проверяет JWT в Hotel Service и ограничивает доступ к внутренним командам общим ключом. */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  /** Создаёт декодер JWT с ключом, совпадающим с Booking Service. */
  @Bean
  JwtDecoder jwtDecoder(@Value("${security.jwt.secret}") String secret) {
    SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /** Преобразует claim roles в authorities Spring Security. */
  @Bean
  Converter<Jwt, ? extends AbstractAuthenticationToken> converter() {
    return jwt ->
        new JwtAuthenticationToken(
            jwt,
            jwt.getClaimAsStringList("roles").stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList()),
            jwt.getSubject());
  }

  /** Разрешает внутренние POST-маршруты, которые сами проверяют X-Internal-Key. */
  @Bean
  SecurityFilterChain chain(
      HttpSecurity http, Converter<Jwt, ? extends AbstractAuthenticationToken> converter)
      throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        HttpMethod.POST,
                        "/api/rooms/*/confirm-availability",
                        "/api/rooms/*/release")
                    .permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(converter)))
        .build();
  }
}
