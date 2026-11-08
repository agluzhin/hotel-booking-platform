package com.hotelbooking.hotel.service;

import com.hotelbooking.hotel.domain.Hotel;
import com.hotelbooking.hotel.domain.Room;
import com.hotelbooking.hotel.repo.HotelRepository;
import com.hotelbooking.hotel.repo.RoomRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Добавляет демонстрационные гостиницы и номера при запуске. */
@Component
public class DataSeeder implements CommandLineRunner {
  private final HotelRepository hotels;
  private final RoomRepository rooms;

  /** Создаёт загрузчик демоданных. */
  public DataSeeder(HotelRepository hotels, RoomRepository rooms) {
    this.hotels = hotels;
    this.rooms = rooms;
  }

  /** Создаёт две гостиницы и десять номеров при пустой базе. */
  @Override
  public void run(String... args) {
    if (hotels.count() > 0) return;
    Hotel central = hotels.save(new Hotel("Demo Central", "1 University Avenue"));
    Hotel riverside = hotels.save(new Hotel("Demo Riverside", "25 River Road"));
    for (int number = 1; number <= 5; number++) {
      rooms.save(new Room(central, "10" + number));
      rooms.save(new Room(riverside, "20" + number));
    }
  }
}
