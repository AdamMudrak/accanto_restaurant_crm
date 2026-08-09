package com.example.restaurant.mapper.management;

import com.example.restaurant.dto.response.management.RestaurantResponse;
import com.example.restaurant.entity.management.Restaurant;
import org.springframework.stereotype.Component;

@Component
public class RestaurantMapper {
    public RestaurantResponse toResponse(Restaurant restaurant) {
        return new RestaurantResponse(
                restaurant.getId(),
                restaurant.getName(),
                getLogoIdIfPresent(restaurant),
                getBackgroundIdIfPresent(restaurant),
                restaurant.getDefaultRegion(),
                restaurant.getLanguage(),
                restaurant.getCurrency(),
                restaurant.getPaymentProvider()
        );
    }

    private Long getLogoIdIfPresent(Restaurant restaurant) {
        if (restaurant.getLogo() != null) {
            return restaurant.getLogo().getId();
        }
        return null;
    }

    private Long getBackgroundIdIfPresent(Restaurant restaurant) {
        if (restaurant.getBackgroundImage() != null) {
            return restaurant.getBackgroundImage().getId();
        }
        return null;
    }
}
