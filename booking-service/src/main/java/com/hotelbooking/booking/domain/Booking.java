package com.hotelbooking.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

/** JPA-сущность бронирования и его состояния. */
@Entity
@Table(
    name = "bookings",
    indexes = {
      @Index(name = "idx_booking_user_created", columnList = "user_id,created_at"),
      @Index(name = "idx_booking_request", columnList = "request_id", unique = true)
    })
public class Booking {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.EAGER)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "room_id", nullable = false)
  private Long roomId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @jakarta.persistence.Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private BookingStatus status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "request_id", nullable = false, unique = true, length = 80)
  private String requestId;

  /** Конструктор JPA для загрузки сущности. */
  protected Booking() {}

  /** Создаёт новое бронирование в состоянии PENDING. */
  public Booking(
      AppUser user, Long roomId, LocalDate startDate, LocalDate endDate, String requestId) {
    this.user = user;
    this.roomId = roomId;
    this.startDate = startDate;
    this.endDate = endDate;
    this.requestId = requestId;
    this.status = BookingStatus.PENDING;
    this.createdAt = Instant.now();
  }

  /** Возвращает идентификатор бронирования. */
  public Long getId() {
    return id;
  }

  /** Возвращает пользователя, создавшего бронирование. */
  public AppUser getUser() {
    return user;
  }

  /** Возвращает идентификатор номера. */
  public Long getRoomId() {
    return roomId;
  }

  /** Возвращает дату начала проживания. */
  public LocalDate getStartDate() {
    return startDate;
  }

  /** Возвращает дату окончания проживания. */
  public LocalDate getEndDate() {
    return endDate;
  }

  /** Возвращает текущее состояние бронирования. */
  public BookingStatus getStatus() {
    return status;
  }

  /** Возвращает время создания бронирования. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /** Возвращает ключ идемпотентности. */
  public String getRequestId() {
    return requestId;
  }

  /** Устанавливает новое состояние бронирования. */
  public void setStatus(BookingStatus status) {
    this.status = status;
  }
}
