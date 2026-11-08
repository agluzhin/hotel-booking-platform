package com.hotelbooking.booking.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.hotelbooking.booking.api.ApiModels.BookingRequest;
import com.hotelbooking.booking.client.HotelClient;
import com.hotelbooking.booking.domain.AppUser;
import com.hotelbooking.booking.domain.Booking;
import com.hotelbooking.booking.domain.BookingStatus;
import com.hotelbooking.booking.domain.Role;
import com.hotelbooking.booking.repo.BookingRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

/** Unit-тесты жизненного цикла саги бронирования. */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
  @Mock private BookingStore store;
  @Mock private BookingRepository repository;
  @Mock private HotelClient hotel;
  private BookingService service;
  private Booking booking;
  private BookingRequest request;

  /** Подготавливает тестовые зависимости и бронирование. */
  @BeforeEach
  void setUp() {
    service = new BookingService(store, repository, hotel);
    AppUser user = new AppUser("demo", "hash", Role.USER);
    request =
        new BookingRequest(
            7L, false, LocalDate.now().plusDays(2), LocalDate.now().plusDays(4), "req-1");
    booking = new Booking(user, 7L, request.startDate(), request.endDate(), request.requestId());
    lenient().when(store.findRequest("req-1")).thenReturn(null);
    lenient()
        .when(store.createPending("demo", 7L, request.startDate(), request.endDate(), "req-1"))
        .thenReturn(booking);
    lenient()
        .when(store.updateStatus(nullable(Long.class), any()))
        .thenAnswer(
            invocation -> {
              booking.setStatus(invocation.getArgument(1));
              return booking;
            });
  }

  /** Проверяет, что успешное подтверждение переводит бронирование в CONFIRMED. */
  @Test
  void confirmsBookingAfterHotelAcceptsSlot() {
    assertEquals("CONFIRMED", service.create("demo", request).status());
    verify(hotel).confirm(7L, request.startDate(), request.endDate(), "req-1");
    verify(hotel, never()).release(any(), any(), any(), anyString());
  }

  /** Проверяет, что повторная доставка requestId не создаёт новое бронирование. */
  @Test
  void repeatedRequestReturnsPreviouslyCreatedBooking() {
    booking.setStatus(BookingStatus.CONFIRMED);
    when(store.findRequest("req-1")).thenReturn(booking);
    assertEquals("CONFIRMED", service.create("demo", request).status());
    verify(store, never()).createPending(anyString(), any(), any(), any(), anyString());
    verifyNoInteractions(hotel);
  }

  /** Проверяет компенсацию при сетевом тайм-ауте Hotel Service. */
  @Test
  void compensatesAfterRemoteTimeout() {
    doThrow(new ResourceAccessException("Read timed out"))
        .when(hotel)
        .confirm(7L, request.startDate(), request.endDate(), "req-1");
    assertThrows(IllegalStateException.class, () -> service.create("demo", request));
    verify(hotel).release(7L, request.startDate(), request.endDate(), "req-1");
    assertEquals(BookingStatus.CANCELLED, booking.getStatus());
  }

  /** Проверяет компенсацию, если Hotel Service отклоняет слот. */
  @Test
  void cancelsAndReleasesWhenHotelFails() {
    doThrow(new IllegalStateException("occupied"))
        .when(hotel)
        .confirm(7L, request.startDate(), request.endDate(), "req-1");
    assertThrows(IllegalStateException.class, () -> service.create("demo", request));
    verify(hotel).release(7L, request.startDate(), request.endDate(), "req-1");
    assertEquals(BookingStatus.CANCELLED, booking.getStatus());
  }
}
