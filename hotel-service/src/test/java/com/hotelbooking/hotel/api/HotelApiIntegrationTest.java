package com.hotelbooking.hotel.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.hotelbooking.hotel.domain.Room;
import com.hotelbooking.hotel.repo.RoomRepository;
import com.hotelbooking.hotel.service.HotelService.AvailabilityRequest;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** MockMvc-проверки административных операций и конкурентной занятости номеров. */
@SpringBootTest(
    properties = {
      "eureka.client.enabled=false",
      "spring.cloud.discovery.enabled=false"
    })
@AutoConfigureMockMvc
class HotelApiIntegrationTest {
  private static final String JWT_SECRET = "local-dev-secret-key-change-me-32-bytes-minimum!";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private RoomRepository rooms;

  /** Проверяет доступность статистики только администратору и обновление номера. */
  @Test
  void statisticsAreAdminOnlyAndRoomCanBeUpdated() throws Exception {
    mockMvc
        .perform(get("/api/rooms/statistics").header("Authorization", "Bearer " + token("demo", "ROLE_USER")))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(get("/api/rooms/statistics").header("Authorization", "Bearer " + token("admin", "ROLE_ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].timesBooked").exists())
        .andExpect(jsonPath("$[0].hotelName").exists());

    Room room = rooms.findById(1L).orElseThrow();
    String originalNumber = room.getNumber();
    String changedNumber = "MVC-" + UUID.randomUUID();
    mockMvc
        .perform(
            put("/api/rooms/1")
                .header("Authorization", "Bearer " + token("admin", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.createObjectNode().put("number", changedNumber).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(changedNumber));

    mockMvc
        .perform(
            put("/api/rooms/1")
                .header("Authorization", "Bearer " + token("admin", "ROLE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.createObjectNode().put("number", originalNumber).toString()))
        .andExpect(status().isOk());
  }

  /** Проверяет, что повторы confirm/release не меняют слот и счётчик повторно. */
  @Test
  void repeatedConfirmAndReleaseAreIdempotent() throws Exception {
    Room room = rooms.findById(1L).orElseThrow();
    long initialCount = room.getTimesBooked();
    LocalDate startDate = LocalDate.now().plusDays(120);
    LocalDate endDate = startDate.plusDays(2);
    String requestId = "idempotent-" + UUID.randomUUID();
    AvailabilityRequest request = new AvailabilityRequest(startDate, endDate, requestId);
    String body = objectMapper.writeValueAsString(request);

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(
              post("/api/rooms/1/confirm-availability")
                  .header("X-Internal-Key", "local-internal-key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk());
    }
    assertEquals(initialCount + 1, rooms.findById(1L).orElseThrow().getTimesBooked());

    for (int attempt = 0; attempt < 2; attempt++) {
      mockMvc
          .perform(
              post("/api/rooms/1/release")
                  .header("X-Internal-Key", "local-internal-key")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isOk());
    }
    assertEquals(initialCount, rooms.findById(1L).orElseThrow().getTimesBooked());
  }

  /** Проверяет, что из двух одновременных подтверждений пересекающегося слота проходит одно. */
  @Test
  void concurrentOverlappingConfirmationsAllowOnlyOneRequest() throws Exception {
    LocalDate startDate = LocalDate.now().plusDays(100);
    LocalDate endDate = startDate.plusDays(2);
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<Integer> first = executor.submit(() -> confirmAtOnce(ready, start, startDate, endDate));
      Future<Integer> second = executor.submit(() -> confirmAtOnce(ready, start, startDate, endDate));
      assertTrue(ready.await(5, TimeUnit.SECONDS));
      start.countDown();
      int firstStatus = first.get(10, TimeUnit.SECONDS);
      int secondStatus = second.get(10, TimeUnit.SECONDS);
      assertEquals(1, (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0));
      assertTrue(firstStatus == 409 || secondStatus == 409);
    } finally {
      start.countDown();
      executor.shutdownNow();
    }
  }

  /** Выполняет внутреннее подтверждение после общего сигнала старта. */
  private int confirmAtOnce(
      CountDownLatch ready, CountDownLatch start, LocalDate startDate, LocalDate endDate)
      throws Exception {
    ready.countDown();
    assertTrue(start.await(5, TimeUnit.SECONDS));
    AvailabilityRequest request =
        new AvailabilityRequest(startDate, endDate, "parallel-" + UUID.randomUUID());
    MvcResult result =
        mockMvc
            .perform(
                post("/api/rooms/1/confirm-availability")
                    .header("X-Internal-Key", "local-internal-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andReturn();
    return result.getResponse().getStatus();
  }

  /** Подписывает тестовый JWT с указанной ролью для проверки Resource Server. */
  private String token(String username, String role) throws Exception {
    Instant now = Instant.now();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .issuer("hotel-booking")
            .subject(username)
            .issueTime(Date.from(now))
            .expirationTime(Date.from(now.plusSeconds(3600)))
            .claim("roles", List.of(role))
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    jwt.sign(new MACSigner(JWT_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    return jwt.serialize();
  }
}
