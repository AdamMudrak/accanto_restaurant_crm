package com.example.restaurant.repository.management;

import com.example.restaurant.entity.management.Restaurant;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Restaurant r")
    Optional<Restaurant> lockSingleton();

    @Query("SELECT r FROM Restaurant r")
    Optional<Restaurant> findSingleton();

    @Query("SELECT r.name AS name FROM Restaurant r")
    Optional<RestaurantNameView> findSingletonName();

    @Query("SELECT r.language AS language FROM Restaurant r")
    Optional<RestaurantLanguageView> findSingletonLanguage();

    interface RestaurantNameView {
        String getName();
    }

    interface RestaurantLanguageView {
        Restaurant.Language getLanguage();
    }
}
