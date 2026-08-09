package com.example.restaurant.mapper.menu;

import com.example.restaurant.dto.request.menu.MenuCategoryRequest;
import com.example.restaurant.dto.response.menu.MenuCategoryResponse;
import com.example.restaurant.entity.menu.MenuCategory;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MenuCategoryMapper {
    public MenuCategoryResponse toResponse(MenuCategory menuCategory) {
        return new MenuCategoryResponse(
                menuCategory.getId(),
                menuCategory.getNameDefaultLanguage(),
                menuCategory.getNameSecondLanguage(),
                getParentCategoryId(menuCategory)
        );
    }

    public List<MenuCategoryResponse> toResponse(List<MenuCategory> menuCategories) {
        List<MenuCategoryResponse> responses = new ArrayList<>();
        for (MenuCategory menuCategory : menuCategories) {
            responses.add(toResponse(menuCategory));
        }

        return responses;
    }

    public MenuCategory toParentEntity(MenuCategoryRequest request) {
        MenuCategory menuCategory = new MenuCategory();
        menuCategory.setNameDefaultLanguage(request.nameDefaultLanguage());
        menuCategory.setNameSecondLanguage(request.nameSecondLanguage());
        return menuCategory;
    }

    public MenuCategory toUpdateParentEntity(Long parentCategoryId, MenuCategoryRequest request) {
        MenuCategory menuCategory = new MenuCategory();
        menuCategory.setId(parentCategoryId);
        if (request.nameDefaultLanguage() != null) {
            menuCategory.setNameDefaultLanguage(request.nameDefaultLanguage());
        }
        if (request.nameSecondLanguage() != null) {
            menuCategory.setNameSecondLanguage(request.nameSecondLanguage());
        }
        return menuCategory;
    }

    public MenuCategory toChildEntity(Long parentCategoryId, MenuCategoryRequest request) {
        MenuCategory menuCategory = new MenuCategory();
        menuCategory.setNameDefaultLanguage(request.nameDefaultLanguage());
        menuCategory.setNameSecondLanguage(request.nameSecondLanguage());
        menuCategory.setParentCategory(new MenuCategory(parentCategoryId));
        return menuCategory;
    }

    public MenuCategory toUpdateChildEntity(Long childCategoryId,
                                            Long parentCategoryId,
                                            MenuCategoryRequest request) {
        MenuCategory menuCategory = new MenuCategory();
        menuCategory.setId(childCategoryId);
        menuCategory.setNameDefaultLanguage(request.nameDefaultLanguage());
        menuCategory.setNameSecondLanguage(request.nameSecondLanguage());
        menuCategory.setParentCategory(new MenuCategory(parentCategoryId));
        return menuCategory;
    }

    private Long getParentCategoryId(MenuCategory menuCategory) {
        if (menuCategory.getParentCategory() != null) {
            return menuCategory.getParentCategory().getId();
        }
        return null;
    }
}
