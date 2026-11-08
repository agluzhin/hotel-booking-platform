package com.hotelbooking.booking.service;

import com.hotelbooking.booking.domain.AppUser;
import com.hotelbooking.booking.domain.Booking;
import com.hotelbooking.booking.domain.BookingStatus;
import com.hotelbooking.booking.repo.BookingRepository;
import com.hotelbooking.booking.repo.UserRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Инкапсулирует короткие локальные транзакции Booking Service. */
@Service
public class BookingStore {
  private final BookingRepository bookings;
  private final UserRepository users;

  /** Создаёт хранилище с репозиториями локальной базы. */
  public BookingStore(BookingRepository bookings, UserRepository users) {
    this.bookings = bookings;
    this.users = users;
  }

  /** Фиксирует PENDING-бронирование до вызова Hotel Service. */
  @Transactional
  public Booking createPending(
      String username, Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
    return bookings
        .findByRequestId(requestId)
        .orElseGet(
            () -> {
              AppUser user =
                  users
                      .findByUsername(username)
                      .orElseThrow(() -> new IllegalArgumentException("User not found"));
              return bookings.save(new Booking(user, roomId, startDate, endDate, requestId));
            });
  }

  /** Ищет бронирование по requestId для повторной доставки. */
  @Transactional(readOnly = true)
  public Booking findRequest(String requestId) {
    return bookings.findByRequestId(requestId).orElse(null);
  }

  /** Переводит бронирование из PENDING в указанное конечное состояние. */
  @Transactional
  public Booking updateStatus(Long id, BookingStatus status) {
    Booking booking =
        bookings.findById(id).orElseThrow(() -> new IllegalArgumentException("Booking not found"));
    if (booking.getStatus() == BookingStatus.PENDING) booking.setStatus(status);
    return booking;
  }

  /** Возвращает бронирование по первичному ключу. */
  @Transactional(readOnly = true)
  public Booking findById(Long id) {
    return bookings
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Booking not found"));
  }
}
