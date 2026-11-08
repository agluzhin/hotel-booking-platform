package com.hotelbooking.hotel.api;

import com.hotelbooking.hotel.domain.Hotel;
import com.hotelbooking.hotel.domain.Room;
import com.hotelbooking.hotel.service.HotelService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST API Hotel Service. */
@RestController
@RequestMapping("/api")
public class HotelController {
  private final HotelService service;
  private final String internalKey;

  /** DTO создания или изменения гостиницы. */
  public record HotelRequest(@NotBlank String name, @NotBlank String address) {}

  /** DTO добавления номера в гостиницу. */
  public record RoomRequest(@NotNull Long hotelId, @NotBlank String number) {}

  /** Создаёт контроллер Hotel Service. */
  public HotelController(
      HotelService service, @Value("${security.internal-key}") String internalKey) {
    this.service = service;
    this.internalKey = internalKey;
  }

  /** Создаёт гостиницу; доступно ADMIN. */
  @PostMapping("/hotels")
  @PreAuthorize("hasRole('ADMIN')")
  public Hotel addHotel(@Valid @RequestBody HotelRequest request) {
    return service.addHotel(request.name(), request.address());
  }

  /** Возвращает гостиницы; доступно любому аутентифицированному пользователю. */
  @GetMapping("/hotels")
  public List<Hotel> hotels() {
    return service.allHotels();
  }

  /** Обновляет гостиницу; доступно ADMIN. */
  @PatchMapping("/hotels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public Hotel updateHotel(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
    return service.updateHotel(id, request.name(), request.address());
  }

  /** Удаляет гостиницу, если номера предварительно удалены. */
  @DeleteMapping("/hotels/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteHotel(@PathVariable Long id) {
    service.deleteHotel(id);
  }

  /** Добавляет номер; доступно ADMIN. */
  @PostMapping("/rooms")
  @PreAuthorize("hasRole('ADMIN')")
  public Room addRoom(@Valid @RequestBody RoomRequest request) {
    return service.addRoom(request.hotelId(), request.number());
  }

  /** Меняет операционную доступность номера; доступно ADMIN. */
  @PatchMapping("/rooms/{id}/availability")
  @PreAuthorize("hasRole('ADMIN')")
  public HotelService.RoomView setAvailability(
      @PathVariable Long id, @RequestParam boolean available) {
    Room room = service.setRoomAvailability(id, available);
    return new HotelService.RoomView(
        room.getId(),
        room.getHotel().getId(),
        room.getNumber(),
        room.isAvailable(),
        room.getTimesBooked());
  }

  /** Удаляет номер без сохранённой истории слотов; доступно ADMIN. */
  @DeleteMapping("/rooms/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public void deleteRoom(@PathVariable Long id) {
    service.deleteRoom(id);
  }

  /** Возвращает свободные номера на указанные даты. */
  @GetMapping("/rooms")
  public List<HotelService.RoomView> rooms(
      @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
    return service.freeRooms(startDate, endDate, false);
  }

  /** Возвращает рекомендованные номера, отсортированные по timesBooked и ID. */
  @GetMapping("/rooms/recommend")
  public List<HotelService.RoomView> recommend(
      @RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
    return service.freeRooms(startDate, endDate, true);
  }

  /** Внутренняя команда временной блокировки номера на даты. */
  @PostMapping("/rooms/{id}/confirm-availability")
  public void confirm(
      @PathVariable Long id,
      @RequestHeader(value = "X-Internal-Key", required = false) String suppliedKey,
      @Valid @RequestBody HotelService.AvailabilityRequest request) {
    checkInternalKey(suppliedKey);
    service.confirm(id, request);
  }

  /** Внутренняя компенсирующая команда освобождения слота. */
  @PostMapping("/rooms/{id}/release")
  public void release(
      @PathVariable Long id,
      @RequestHeader(value = "X-Internal-Key", required = false) String suppliedKey,
      @Valid @RequestBody HotelService.AvailabilityRequest request) {
    checkInternalKey(suppliedKey);
    service.release(id, request);
  }

  /** Сравнивает переданный внутренний ключ с настроенным секретом. */
  private void checkInternalKey(String suppliedKey) {
    if (suppliedKey == null
        || !MessageDigest.isEqual(
            internalKey.getBytes(StandardCharsets.UTF_8),
            suppliedKey.getBytes(StandardCharsets.UTF_8))) {
      throw new AccessDeniedException("Internal service authentication required");
    }
  }
}
