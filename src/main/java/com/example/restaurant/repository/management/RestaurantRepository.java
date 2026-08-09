package com.example.restaurant.repository.management;

import com.example.restaurant.entity.management.Restaurant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
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
