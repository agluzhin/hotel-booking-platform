package com.hotelbooking.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** JPA-сущность занятого периода номера. */
@Entity
@Table(
    name = "room_slots",
    indexes = @Index(name = "idx_slot_overlap", columnList = "room_id,start_date,end_date"))
public class RoomSlot {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "request_id", nullable = false, unique = true)
  private String requestId;

  @Column(nullable = false)
  private boolean active = true;

  /** Конструктор JPA для загрузки сущности. */
  protected RoomSlot() {}

  /** Создаёт занятый период номера с ключом идемпотентности. */
  public RoomSlot(Room room, LocalDate startDate, LocalDate endDate, String requestId) {
    this.room = room;
    this.startDate = startDate;
    this.endDate = endDate;
    this.requestId = requestId;
  }

  /** Возвращает идентификатор слота. */
  public Long getId() {
    return id;
  }

  /** Возвращает номер, к которому относится слот. */
  public Room getRoom() {
    return room;
  }

  /** Возвращает дату начала периода. */
  public LocalDate getStartDate() {
    return startDate;
  }

  /** Возвращает дату окончания периода. */
  public LocalDate getEndDate() {
    return endDate;
  }

  /** Возвращает ключ идемпотентности. */
  public String getRequestId() {
    return requestId;
  }

  /** Проверяет, активен ли период бронирования. */
  public boolean isActive() {
    return active;
  }

  /** Помечает слот как освобождённый. */
  public void release() {
    active = false;
  }
}
