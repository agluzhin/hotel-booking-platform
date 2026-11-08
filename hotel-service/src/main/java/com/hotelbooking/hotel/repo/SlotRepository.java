package com.hotelbooking.hotel.repo;

import com.hotelbooking.hotel.domain.RoomSlot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Репозиторий занятых периодов номеров. */
public interface SlotRepository extends JpaRepository<RoomSlot, Long> {
  /** Ищет слот по уникальному ключу запроса. */
  Optional<RoomSlot> findByRequestId(String requestId);

  /** Проверяет, есть ли у номера сохранённая история слотов. */
  boolean existsByRoomId(Long roomId);

  /** Возвращает активные интервалы номера, пересекающиеся с заданными датами. */
  @Query(
      "select s from RoomSlot s where s.room.id = :roomId and s.active = true "
          + "and s.startDate < :endDate and s.endDate > :startDate")
  List<RoomSlot> findOverlaps(
      @Param("roomId") Long roomId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}
