package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.domain.Hotel;
import com.hotelbooking.hotel.domain.Room;
import com.hotelbooking.hotel.domain.RoomSlot;
import com.hotelbooking.hotel.repo.HotelRepository;
import com.hotelbooking.hotel.repo.RoomRepository;
import com.hotelbooking.hotel.repo.SlotRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Управляет гостиницами, номерами и занятостью их временных слотов. */
@Service
public class HotelService {
  private static final Logger log = LoggerFactory.getLogger(HotelService.class);
  private final HotelRepository hotels;
  private final RoomRepository rooms;
  private final SlotRepository slots;

  /** Создаёт сервис с локальными репозиториями Hotel Service. */
  public HotelService(HotelRepository hotels, RoomRepository rooms, SlotRepository slots) {
    this.hotels = hotels;
    this.rooms = rooms;
    this.slots = slots;
  }

  /** DTO номера в публичном API. */
  public record RoomView(
      Long id, Long hotelId, String number, boolean available, long timesBooked) {}

  /** Сводная загрузка одного номера для административного отчёта. */
  public record RoomStatistics(
      Long roomId,
      Long hotelId,
      String hotelName,
      String number,
      boolean available,
      long timesBooked) {}

  /** Данные для внутренней команды подтверждения или освобождения периода. */
  public record AvailabilityRequest(LocalDate startDate, LocalDate endDate, String requestId) {}

  /** Создаёт гостиницу. */
  @Transactional
  public Hotel addHotel(String name, String address) {
    return hotels.save(new Hotel(name, address));
  }

  /** Добавляет номер в существующую гостиницу. */
  @Transactional
  public Room addRoom(Long hotelId, String number) {
    Hotel hotel =
        hotels.findById(hotelId).orElseThrow(() -> new NoSuchElementException("Hotel not found"));
    return rooms.save(new Room(hotel, number));
  }

  /** Возвращает список всех гостиниц. */
  @Transactional(readOnly = true)
  public List<Hotel> allHotels() {
    return hotels.findAll();
  }

  /** Обновляет название и адрес гостиницы. */
  @Transactional
  public Hotel updateHotel(Long id, String name, String address) {
    Hotel hotel =
        hotels.findById(id).orElseThrow(() -> new NoSuchElementException("Hotel not found"));
    hotel.update(name, address);
    return hotel;
  }

  /** Удаляет гостиницу, если в ней нет номеров. */
  @Transactional
  public void deleteHotel(Long id) {
    if (!hotels.existsById(id)) throw new NoSuchElementException("Hotel not found");
    if (rooms.existsByHotelId(id))
      throw new IllegalStateException("Delete hotel rooms before deleting the hotel");
    hotels.deleteById(id);
  }

  /** Меняет эксплуатационную доступность номера. */
  @Transactional
  public Room setRoomAvailability(Long id, boolean available) {
    Room room = rooms.findById(id).orElseThrow(() -> new NoSuchElementException("Room not found"));
    room.setAvailable(available);
    return room;
  }

  /** Обновляет номер комнаты, не меняя гостиницу и историю бронирований. */
  @Transactional
  public Room updateRoomNumber(Long id, String number) {
    Room room = rooms.findById(id).orElseThrow(() -> new NoSuchElementException("Room not found"));
    if (rooms.existsByHotelIdAndNumberAndIdNot(room.getHotel().getId(), number, id)) {
      throw new IllegalStateException("Room number already exists in this hotel");
    }
    room.setNumber(number);
    return room;
  }

  /** Удаляет номер, если у него нет связанной истории слотов. */
  @Transactional
  public void deleteRoom(Long id) {
    if (!rooms.existsById(id)) throw new NoSuchElementException("Room not found");
    if (slots.existsByRoomId(id))
      throw new IllegalStateException("Room has booking history and cannot be deleted");
    rooms.deleteById(id);
  }

  /** Возвращает свободные номера на период, при необходимости сортируя по загрузке. */
  @Transactional(readOnly = true)
  public List<RoomView> freeRooms(LocalDate startDate, LocalDate endDate, boolean recommend) {
    validateDates(startDate, endDate);
    Comparator<Room> ordering =
        recommend
            ? Comparator.comparingLong(Room::getTimesBooked).thenComparing(Room::getId)
            : Comparator.comparing(Room::getId);
    return rooms.findByAvailableTrue().stream()
        .filter(room -> slots.findOverlaps(room.getId(), startDate, endDate).isEmpty())
        .sorted(ordering)
        .map(this::toView)
        .toList();
  }

  /** Возвращает загрузку всех номеров, включая временно недоступные. */
  @Transactional(readOnly = true)
  public List<RoomStatistics> roomStatistics() {
    return rooms.findAll().stream()
        .sorted(
            Comparator.comparingLong(Room::getTimesBooked)
                .reversed()
                .thenComparing(Room::getId))
        .map(
            room ->
                new RoomStatistics(
                    room.getId(),
                    room.getHotel().getId(),
                    room.getHotel().getName(),
                    room.getNumber(),
                    room.isAvailable(),
                    room.getTimesBooked()))
        .toList();
  }

  /** Блокирует номер, проверяет пересечения и создаёт слот с requestId. */
  @Transactional
  public void confirm(Long roomId, AvailabilityRequest request) {
    validateDates(request.startDate(), request.endDate());
    Room room =
        rooms.findLocked(roomId).orElseThrow(() -> new NoSuchElementException("Room not found"));
    RoomSlot previous = slots.findByRequestId(request.requestId()).orElse(null);
    if (previous != null) {
      verifySameRequest(previous, roomId, request);
      if (previous.isActive()) {
        log.info("requestId={} roomId={} slot=ALREADY_CONFIRMED", request.requestId(), roomId);
        return;
      }
      throw new IllegalStateException("Booking was already released");
    }
    if (!room.isAvailable()) throw new IllegalStateException("Room is out of service");
    if (!slots.findOverlaps(roomId, request.startDate(), request.endDate()).isEmpty()) {
      throw new IllegalStateException("Room is already booked for requested dates");
    }
    slots.save(new RoomSlot(room, request.startDate(), request.endDate(), request.requestId()));
    room.booked();
    log.info(
        "requestId={} roomId={} slot=CONFIRMED startDate={} endDate={}",
        request.requestId(),
        roomId,
        request.startDate(),
        request.endDate());
  }

  /** Освобождает активный слот и идемпотентно уменьшает счётчик загрузки. */
  @Transactional
  public void release(Long roomId, AvailabilityRequest request) {
    Room room =
        rooms.findLocked(roomId).orElseThrow(() -> new NoSuchElementException("Room not found"));
    RoomSlot slot = slots.findByRequestId(request.requestId()).orElse(null);
    if (slot == null) {
      log.info("requestId={} roomId={} release=NOOP", request.requestId(), roomId);
      return;
    }
    verifySameRequest(slot, roomId, request);
    if (!slot.isActive()) {
      log.info("requestId={} roomId={} release=ALREADY_RELEASED", request.requestId(), roomId);
      return;
    }
    slot.release();
    room.released();
    log.info("requestId={} roomId={} slot=RELEASED", request.requestId(), roomId);
  }

  /** Проверяет совпадение ключа, номера и дат с ранее обработанной командой. */
  private void verifySameRequest(RoomSlot slot, Long roomId, AvailabilityRequest request) {
    if (!slot.getRoom().getId().equals(roomId)
        || !slot.getStartDate().equals(request.startDate())
        || !slot.getEndDate().equals(request.endDate())) {
      throw new IllegalStateException("Idempotency key belongs to a different booking");
    }
  }

  /** Проверяет корректность полуоткрытого интервала [startDate, endDate). */
  private void validateDates(LocalDate startDate, LocalDate endDate) {
    if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
      throw new IllegalArgumentException("endDate must be after startDate");
    }
  }

  /** Преобразует JPA-сущность номера в DTO API. */
  private RoomView toView(Room room) {
    return new RoomView(
        room.getId(),
        room.getHotel().getId(),
        room.getNumber(),
        room.isAvailable(),
        room.getTimesBooked());
  }
}
