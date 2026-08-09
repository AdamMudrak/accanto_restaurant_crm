package com.example.restaurant.service.management;

import com.example.restaurant.dto.response.management.RestaurantResponse;
import com.example.restaurant.entity.image.Image;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.exception.types.notfound.RestaurantNotFoundException;
import com.example.restaurant.mapper.management.RestaurantMapper;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.service.image.ImageService;
import com.example.restaurant.service.payment.PaymentProviderType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final ImageService imageService;

    public RestaurantResponse getRestaurant() {
        return restaurantMapper.toResponse(findRestaurant());

    }

    public RestaurantResponse updateRestaurant(
            String name,
            Restaurant.DefaultRegion defaultRegion,
            MultipartFile logo,
            MultipartFile backgroundImage) {
        Restaurant restaurant = findRestaurant();
        updatePresentFields(restaurant, name, defaultRegion, logo, backgroundImage);
        return restaurantMapper.toResponse(restaurantRepository.save(restaurant));
    }

    private Restaurant findRestaurant() {
        return restaurantRepository.findSingleton()
                .orElseThrow(() -> new RestaurantNotFoundException("Couldn't obtain restaurant"));
    }

    private void updatePresentFields(Restaurant restaurant,
                                     String name,
                                     Restaurant.DefaultRegion defaultRegion,
                                     MultipartFile logo,
                                     MultipartFile backgroundImage) {
        if (name != null && !name.isBlank()) {
            restaurant.setName(name);
        }

        if (defaultRegion != null) {
            applyRegionSettings(restaurant, defaultRegion);
        }

        if (logo != null) {
            Image image = imageService.save(logo);
            if (restaurant.getLogo() != null) {
                imageService.delete(restaurant.getLogo().getId());
            }
            restaurant.setLogo(image);
        }

        if (backgroundImage != null) {
            Image image = imageService.save(backgroundImage);
            if (restaurant.getBackgroundImage() != null) {
                imageService.delete(restaurant.getBackgroundImage().getId());
            }
            restaurant.setBackgroundImage(image);
        }
    }

    private void applyRegionSettings(Restaurant restaurant, Restaurant.DefaultRegion region) {
        restaurant.setDefaultRegion(region);

        if (region == Restaurant.DefaultRegion.EUROPE) {
            restaurant.setCurrency(Restaurant.Currency.EUR);
            restaurant.setPaymentProvider(PaymentProviderType.STRIPE);
            restaurant.setLanguage(Restaurant.Language.EN);
        }

        if (region == Restaurant.DefaultRegion.UKRAINE) {
            restaurant.setCurrency(Restaurant.Currency.UAH);
            restaurant.setPaymentProvider(PaymentProviderType.MONOBANK);
            restaurant.setLanguage(Restaurant.Language.UA);
        }
    }
}
