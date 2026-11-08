package com.hotelbooking.booking.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotelbooking.booking.api.ApiModels.BookingRequest;
import com.hotelbooking.booking.client.HotelClient;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.client.ResourceAccessException;

/** MockMvc-интеграционные проверки API бронирований и саги. */
@SpringBootTest(
    properties = {
      "eureka.client.enabled=false",
      "spring.cloud.discovery.enabled=false"
    })
@AutoConfigureMockMvc
class BookingApiIntegrationTest {
  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private HotelClient hotelClient;

  /** Сбрасывает имитацию Hotel Service перед каждым тестом. */
  @BeforeEach
  void resetHotelClient() {
    reset(hotelClient);
  }

  /** Проверяет доступ к API, идемпотентность и личную историю через HTTP-слой. */
  @Test
  void bookingApiRequiresAuthenticationAndDeduplicatesRequest() throws Exception {
    mockMvc.perform(get("/api/bookings")).andExpect(status().isUnauthorized());

    String userToken = token("demo", "demo12345");
    LocalDate startDate = LocalDate.now().plusDays(40);
    LocalDate endDate = startDate.plusDays(2);
    BookingRequest request =
        new BookingRequest(1L, false, startDate, endDate, "mvc-" + UUID.randomUUID());
    String requestBody = objectMapper.writeValueAsString(request);

    MvcResult first =
        mockMvc
            .perform(
                post("/api/booking")
                    .header("Authorization", "Bearer " + userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andReturn();
    String bookingId =
        objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

    mockMvc
        .perform(
            post("/api/booking")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookingId));
    verify(hotelClient, times(1))
        .confirm(1L, startDate, endDate, request.requestId());

    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(bookingId));
  }

  /** Проверяет переход в CANCELLED и компенсацию после тайм-аута Hotel Service. */
  @Test
  void timeoutCancelsBookingAndReturnsConflict() throws Exception {
    String userToken = token("demo", "demo12345");
    LocalDate startDate = LocalDate.now().plusDays(50);
    LocalDate endDate = startDate.plusDays(2);
    String requestId = "mvc-timeout-" + UUID.randomUUID();
    doThrow(new ResourceAccessException("Read timed out"))
        .when(hotelClient)
        .confirm(anyLong(), any(), any(), anyString());

    BookingRequest request = new BookingRequest(1L, false, startDate, endDate, requestId);
    mockMvc
        .perform(
            post("/api/booking")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());

    verify(hotelClient).release(1L, startDate, endDate, requestId);
    mockMvc
        .perform(get("/api/bookings").header("Authorization", "Bearer " + userToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].requestId").value(requestId))
        .andExpect(jsonPath("$.content[0].status").value("CANCELLED"));
  }

  /** Проверяет, что пользователь не может обращаться к административному API. */
  @Test
  void userCannotCreateAdministrativeAccount() throws Exception {
    String userToken = token("demo", "demo12345");
    mockMvc
        .perform(
            post("/api/user")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"blocked-user\",\"password\":\"password123\",\"role\":\"USER\"}"))
        .andExpect(status().isForbidden());
  }

  /** Получает JWT тестового пользователя через публичный endpoint входа. */
  private String token(String username, String password) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/user/auth")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        objectMapper
                            .createObjectNode()
                            .put("username", username)
                            .put("password", password)
                            .toString()))
            .andExpect(status().isOk())
            .andReturn();
    JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
    return response.get("accessToken").asText();
  }
}
