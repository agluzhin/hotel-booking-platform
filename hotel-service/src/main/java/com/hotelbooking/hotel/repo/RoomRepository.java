package com.hotelbooking.hotel.repo;

import com.hotelbooking.hotel.domain.Room;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Репозиторий номеров с блокировкой строки при подтверждении. */
public interface RoomRepository extends JpaRepository<Room, Long> {
  /** Находит номер и блокирует его строку на время текущей транзакции. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from Room r where r.id = :id")
  Optional<Room> findLocked(@Param("id") Long id);

  /** Возвращает номера, доступные к эксплуатации. */
  List<Room> findByAvailableTrue();

  /** Проверяет, содержит ли гостиница хотя бы один номер. */
  boolean existsByHotelId(Long hotelId);

  /** Проверяет уникальность номера комнаты в гостинице при изменении. */
  boolean existsByHotelIdAndNumberAndIdNot(Long hotelId, String number, Long id);
}
