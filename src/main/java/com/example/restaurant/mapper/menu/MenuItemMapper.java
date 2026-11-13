package com.example.restaurant.mapper.menu;

import com.example.restaurant.dto.request.menu.MenuItemRequest;
import com.example.restaurant.dto.request.menu.UpdateMenuItemRequest;
import com.example.restaurant.dto.response.menu.MenuItemResponse;
import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.entity.menu.MenuCategory;
import com.example.restaurant.entity.menu.MenuItem;
import com.example.restaurant.repository.management.RestaurantRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MenuItemMapper {
    private final RestaurantRepository restaurantRepository;

    public MenuItemResponse toMenuItemResponse(MenuItem menuItem) {
        return toMenuItemResponse(menuItem,
                restaurantRepository.findSingleton().orElseThrow().getCurrency());
    }

    private MenuItemResponse toMenuItemResponse(MenuItem menuItem, Restaurant.Currency currency) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getNameDefaultLanguage(),
                menuItem.getNameSecondLanguage(),
                menuItem.getDescriptionDefaultLanguage(),
                menuItem.getDescriptionSecondLanguage(),
                menuItem.getIngredientsDefaultLanguage() == null ? List.of()
                        : List.copyOf(menuItem.getIngredientsDefaultLanguage()),
                menuItem.getIngredientsSecondLanguage() == null ? List.of()
                        : List.copyOf(menuItem.getIngredientsSecondLanguage()),
                menuItem.isAllergenic(),
                menuItem.isVegetarian(),
                menuItem.isVegan(),
                menuItem.isAvailable(),
                menuItem.getMaxQuantityPerPerson(),
                menuItem.getWeightGrams(),
                menuItem.getPrice(),
                getImageId(menuItem),
                getMenuCategoryId(menuItem),
                currency
        );
    }

    public List<MenuItemResponse> toMenuItemResponseList(List<MenuItem> menuItems) {
        List<MenuItemResponse> menuItemResponseList = new ArrayList<>();
        Restaurant.Currency currency = restaurantRepository.findSingleton().orElseThrow()
                .getCurrency();

        for (MenuItem menuItem : menuItems) {
            menuItemResponseList.add(toMenuItemResponse(menuItem, currency));
        }

        return menuItemResponseList;
    }

    public MenuItem toCreateEntity(MenuItemRequest menuItemRequest) {
        MenuItem menuItem = new MenuItem();
        menuItem.setNameDefaultLanguage(menuItemRequest.nameDefaultLanguage());
        menuItem.setNameSecondLanguage(menuItemRequest.nameSecondLanguage());
        menuItem.setDescriptionDefaultLanguage(menuItemRequest.descriptionDefaultLanguage());
        menuItem.setDescriptionSecondLanguage(menuItemRequest.descriptionSecondLanguage());
        menuItem.setIngredientsDefaultLanguage(menuItemRequest.ingredientsDefaultLanguage());
        menuItem.setIngredientsSecondLanguage(menuItemRequest.ingredientsSecondLanguage());
        menuItem.setAllergenic(menuItemRequest.isAllergenic());
        menuItem.setVegetarian(menuItemRequest.isVegetarian());
        menuItem.setVegan(menuItemRequest.isVegan());
        menuItem.setMaxQuantityPerPerson(menuItemRequest.maxQuantityPerPerson());
        menuItem.setWeightGrams(menuItemRequest.weightGrams());
        menuItem.setPrice(menuItemRequest.price());
        menuItem.setMenuCategory(getMenuCategory(menuItemRequest.childCategoryId()));

        return menuItem;
    }

    public MenuItem toUpdateEntity(MenuItem menuItem, UpdateMenuItemRequest request) {
        if (request.nameDefaultLanguage() != null && !request.nameDefaultLanguage().isBlank()) {
            menuItem.setNameDefaultLanguage(request.nameDefaultLanguage());
        }
        if (request.nameSecondLanguage() != null && !request.nameSecondLanguage().isBlank()) {
            menuItem.setNameSecondLanguage(request.nameSecondLanguage());
        }
        boolean hasDefaultDescription = request.descriptionDefaultLanguage() != null
                && !request.descriptionDefaultLanguage().isBlank();
        if (hasDefaultDescription) {
            menuItem.setDescriptionDefaultLanguage(request.descriptionDefaultLanguage());
        }
        boolean hasSecondDescription = request.descriptionSecondLanguage() != null
                && !request.descriptionSecondLanguage().isBlank();
        if (hasSecondDescription) {
            menuItem.setDescriptionSecondLanguage(request.descriptionSecondLanguage());
        }
        boolean hasDefaultIngredients = request.ingredientsDefaultLanguage() != null
                && !request.ingredientsDefaultLanguage().isEmpty();
        if (hasDefaultIngredients) {
            menuItem.setIngredientsDefaultLanguage(request.ingredientsDefaultLanguage());
        }
        boolean hasSecondIngredients = request.ingredientsSecondLanguage() != null
                && !request.ingredientsSecondLanguage().isEmpty();
        if (hasSecondIngredients) {
            menuItem.setIngredientsSecondLanguage(request.ingredientsSecondLanguage());
        }
        if (request.isAllergenic() != null) {
            menuItem.setAllergenic(request.isAllergenic());
        }
        if (request.isVegan() != null) {
            menuItem.setVegan(request.isVegan());
        }
        if (request.isVegetarian() != null) {
            menuItem.setVegetarian(request.isVegetarian());
        }
        if (request.isAvailable() != null) {
            menuItem.setAvailable(request.isAvailable());
        }
        if (request.maxQuantityPerPerson() != null) {
            menuItem.setMaxQuantityPerPerson(request.maxQuantityPerPerson());
        }
        if (request.weightGrams() != null) {
            menuItem.setWeightGrams(request.weightGrams());
        }
        if (request.price() != null) {
            menuItem.setPrice(request.price());
        }
        if (request.childCategoryId() != null) {
            menuItem.setMenuCategory(getMenuCategory(request.childCategoryId()));
        }

        return menuItem;
    }

    private MenuCategory getMenuCategory(Long id) {
        return new MenuCategory(id);
    }

    private Long getImageId(MenuItem menuItem) {
        if (menuItem.getItemImage() != null) {
            return menuItem.getItemImage().getId();
        }
        return null;
    }

    private Long getMenuCategoryId(MenuItem menuItem) {
        if (menuItem.getMenuCategory() != null) {
            return menuItem.getMenuCategory().getId();
        }
        return null;
    }
}
