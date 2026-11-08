package com.hotelbooking.hotel.repo;

import com.hotelbooking.hotel.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

/** Репозиторий гостиниц. */
public interface HotelRepository extends JpaRepository<Hotel, Long> {}
