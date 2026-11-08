package com.hotelbooking.booking.repo;

import com.hotelbooking.booking.domain.Booking;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Репозиторий для запросов и хранения бронирований. */
public interface BookingRepository extends JpaRepository<Booking, Long> {
  /** Ищет бронирование по ключу идемпотентности. */
  Optional<Booking> findByRequestId(String requestId);

  /** Возвращает страницу бронирований указанного пользователя. */
  Page<Booking> findByUserUsername(String username, Pageable pageable);
}
