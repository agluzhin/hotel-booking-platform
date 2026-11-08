package com.hotelbooking.booking.repo;

import com.hotelbooking.booking.domain.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Репозиторий для хранения и поиска пользователей. */
public interface UserRepository extends JpaRepository<AppUser, Long> {
  /** Ищет пользователя по уникальному имени. */
  Optional<AppUser> findByUsername(String username);

  /** Проверяет, занято ли имя пользователя. */
  boolean existsByUsername(String username);
}
