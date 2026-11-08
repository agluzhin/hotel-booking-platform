package com.hotelbooking.booking.service;

import com.hotelbooking.booking.api.ApiModels.BookingRequest;
import com.hotelbooking.booking.api.ApiModels.BookingResponse;
import com.hotelbooking.booking.client.HotelClient;
import com.hotelbooking.booking.domain.Booking;
import com.hotelbooking.booking.domain.BookingStatus;
import com.hotelbooking.booking.repo.BookingRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Координирует жизненный цикл бронирования и межсервисную сагу. */
@Service
public class BookingService {
  private static final Logger log = LoggerFactory.getLogger(BookingService.class);
  private final BookingStore store;
  private final BookingRepository bookings;
  private final HotelClient hotel;

  /** Создаёт сервис бронирований с локальным хранилищем и Hotel Service client. */
  public BookingService(BookingStore store, BookingRepository bookings, HotelClient hotel) {
    this.store = store;
    this.bookings = bookings;
    this.hotel = hotel;
  }

  /** Создаёт PENDING-бронирование, подтверждает его в Hotel Service или выполняет компенсацию. */
  public BookingResponse create(String username, BookingRequest request) {
    validateDates(request.startDate(), request.endDate());
    Booking existing = store.findRequest(request.requestId());
    if (existing != null) return existingForUser(existing, username);

    Long roomId = request.roomId();
    if (request.autoSelect()) {
      List<HotelClient.RoomDto> recommended =
          hotel.recommend(request.startDate(), request.endDate());
      if (recommended.isEmpty()) throw new IllegalStateException("No available rooms");
      roomId = recommended.get(0).id();
    }
    if (roomId == null)
      throw new IllegalArgumentException("roomId is required when autoSelect is false");

    Booking pending;
    try {
      pending =
          store.createPending(
              username, roomId, request.startDate(), request.endDate(), request.requestId());
    } catch (DataIntegrityViolationException duplicateRequest) {
      Booking raced = store.findRequest(request.requestId());
      if (raced == null) throw duplicateRequest;
      return existingForUser(raced, username);
    }
    if (pending.getStatus() != BookingStatus.PENDING) return response(pending);
    log.info("bookingId={} state=PENDING requestId={}", pending.getId(), pending.getRequestId());

    try {
      hotel.confirm(roomId, request.startDate(), request.endDate(), request.requestId());
      Booking confirmed = store.updateStatus(pending.getId(), BookingStatus.CONFIRMED);
      log.info("bookingId={} state=CONFIRMED", confirmed.getId());
      return response(confirmed);
    } catch (RuntimeException confirmationFailure) {
      compensate(pending, roomId);
      Booking cancelled = store.updateStatus(pending.getId(), BookingStatus.CANCELLED);
      log.warn(
          "bookingId={} state=CANCELLED reason={}",
          pending.getId(),
          confirmationFailure.getClass().getSimpleName());
      throw new IllegalStateException(
          "Booking could not be confirmed and was cancelled", confirmationFailure);
    }
  }

  /** Возвращает бронирование его владельцу или администратору. */
  public BookingResponse get(Long id, String username, boolean admin) {
    Booking booking = store.findById(id);
    ensureOwnerOrAdmin(booking, username, admin);
    return response(booking);
  }

  /** Отменяет бронирование и освобождает подтверждённый слот в Hotel Service. */
  public BookingResponse cancel(Long id, String username, boolean admin) {
    Booking booking = store.findById(id);
    ensureOwnerOrAdmin(booking, username, admin);
    if (booking.getStatus() == BookingStatus.CONFIRMED) {
      hotel.release(
          booking.getRoomId(),
          booking.getStartDate(),
          booking.getEndDate(),
          booking.getRequestId());
    }
    return response(store.updateStatus(id, BookingStatus.CANCELLED));
  }

  /** Возвращает постраничную историю только текущего пользователя. */
  public Page<BookingResponse> history(String username, Pageable pageable) {
    return bookings.findByUserUsername(username, pageable).map(this::response);
  }

  /** Выполняет идемпотентную компенсацию слота в Hotel Service. */
  private void compensate(Booking booking, Long roomId) {
    try {
      hotel.release(roomId, booking.getStartDate(), booking.getEndDate(), booking.getRequestId());
      log.info("bookingId={} compensation=RELEASED", booking.getId());
    } catch (RuntimeException releaseFailure) {
      log.error("bookingId={} compensation=FAILED", booking.getId(), releaseFailure);
    }
  }

  /** Запрещает доступ к бронированию пользователю, который не является его владельцем. */
  private void ensureOwnerOrAdmin(Booking booking, String username, boolean admin) {
    if (!admin && !booking.getUser().getUsername().equals(username)) {
      throw new SecurityException("Booking belongs to another user");
    }
  }

  /** Возвращает результат повторной доставки requestId только его владельцу. */
  private BookingResponse existingForUser(Booking booking, String username) {
    ensureOwnerOrAdmin(booking, username, false);
    return response(booking);
  }

  /** Проверяет, что дата заезда будущая, а дата выезда позже даты заезда. */
  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null
        || endDate == null
        || !startDate.isAfter(LocalDate.now())
        || !endDate.isAfter(startDate)) {
      throw new IllegalArgumentException("Dates must be valid and startDate must be in the future");
    }
  }

  /** Преобразует сущность бронирования в DTO ответа API. */
  private BookingResponse response(Booking booking) {
    return new BookingResponse(
        booking.getId(),
        booking.getUser().getUsername(),
        booking.getRoomId(),
        booking.getStartDate(),
        booking.getEndDate(),
        booking.getStatus().name(),
        booking.getCreatedAt(),
        booking.getRequestId());
  }
}
