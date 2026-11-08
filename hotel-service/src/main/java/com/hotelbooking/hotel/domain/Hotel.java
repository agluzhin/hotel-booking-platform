package com.hotelbooking.hotel.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA-сущность гостиницы. */
@Entity
@Table(name = "hotels")
public class Hotel {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String address;

  /** Конструктор JPA для загрузки сущности. */
  protected Hotel() {}

  /** Создаёт гостиницу с названием и адресом. */
  public Hotel(String name, String address) {
    this.name = name;
    this.address = address;
  }

  /** Возвращает идентификатор гостиницы. */
  public Long getId() {
    return id;
  }

  /** Возвращает название гостиницы. */
  public String getName() {
    return name;
  }

  /** Возвращает адрес гостиницы. */
  public String getAddress() {
    return address;
  }

  /** Обновляет название и адрес гостиницы. */
  public void update(String name, String address) {
    this.name = name;
    this.address = address;
  }
}
