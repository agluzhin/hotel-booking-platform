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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/** JPA-сущность номера и показателей его загрузки. */
@Entity
@Table(
    name = "rooms",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_hotel_room_number",
            columnNames = {"hotel_id", "number"}),
    indexes = @Index(name = "idx_room_load", columnList = "times_booked,id"))
public class Room {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "hotel_id", nullable = false)
  private Hotel hotel;

  @Column(nullable = false)
  private String number;

  @Column(nullable = false)
  private boolean available = true;

  @Column(name = "times_booked", nullable = false)
  private long timesBooked;

  @Version private long version;

  /** Конструктор JPA для загрузки сущности. */
  protected Room() {}

  /** Создаёт номер в гостинице. */
  public Room(Hotel hotel, String number) {
    this.hotel = hotel;
    this.number = number;
  }

  /** Возвращает идентификатор номера. */
  public Long getId() {
    return id;
  }

  /** Возвращает гостиницу, которой принадлежит номер. */
  public Hotel getHotel() {
    return hotel;
  }

  /** Возвращает номер комнаты в гостинице. */
  public String getNumber() {
    return number;
  }

  /** Возвращает признак эксплуатационной доступности номера. */
  public boolean isAvailable() {
    return available;
  }

  /** Возвращает число активных подтверждённых бронирований номера. */
  public long getTimesBooked() {
    return timesBooked;
  }

  /** Меняет операционную доступность номера. */
  public void setAvailable(boolean available) {
    this.available = available;
  }

  /** Увеличивает счётчик бронирований после подтверждения слота. */
  public void booked() {
    timesBooked++;
  }

  /** Уменьшает счётчик после освобождения активного слота. */
  public void released() {
    if (timesBooked > 0) timesBooked--;
  }
}
