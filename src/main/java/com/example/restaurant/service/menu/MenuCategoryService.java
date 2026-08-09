package com.example.restaurant.service.menu;

import com.example.restaurant.dto.request.menu.MenuCategoryRequest;
import com.example.restaurant.dto.response.list.menu.MenuCategoryListResponse;
import com.example.restaurant.dto.response.menu.MenuCategoryResponse;
import com.example.restaurant.entity.menu.MenuCategory;
import com.example.restaurant.exception.types.conflict.MenuCategoryIsUsedException;
import com.example.restaurant.exception.types.notfound.MenuCategoryNotFoundException;
import com.example.restaurant.mapper.menu.MenuCategoryMapper;
import com.example.restaurant.repository.menu.MenuCategoryRepository;
import com.example.restaurant.repository.menu.MenuItemRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class MenuCategoryService {
    private final MenuCategoryRepository menuCategoryRepository;
    private final MenuCategoryMapper menuCategoryMapper;
    private final MenuItemRepository menuItemRepository;

    public MenuCategoryResponse findById(Long id) {
        return menuCategoryMapper.toResponse(
                menuCategoryRepository.findById(id).orElseThrow(
                        () -> new MenuCategoryNotFoundException(
                                "MenuCategory with id " + id + " not found")));
    }

    public MenuCategoryListResponse findAllParentCategories(Pageable pageable) {
        Page<MenuCategory> menuCategories =
                menuCategoryRepository.findAllByParentCategoryIsNull(pageable);
        List<MenuCategoryResponse> menuCategoryResponses =
                menuCategoryMapper.toResponse(menuCategories.toList());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = menuCategories.getTotalElements();

        return new MenuCategoryListResponse(
                menuCategoryResponses,
                pageNumber,
                pageSize,
                totalElements);
    }

    public MenuCategoryListResponse findAllChildCategories(
            Long parentCategoryId, Pageable pageable) {
        validateParentCategoryExists(parentCategoryId);
        Page<MenuCategory> menuCategories =
                menuCategoryRepository.findAllByParentCategoryId(parentCategoryId, pageable);
        List<MenuCategoryResponse> menuCategoryResponses =
                menuCategoryMapper.toResponse(menuCategories.toList());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElement = menuCategories.getTotalElements();

        return new MenuCategoryListResponse(
                menuCategoryResponses,
                pageNumber,
                pageSize,
                totalElement);
    }

    public MenuCategoryResponse createParentCategory(MenuCategoryRequest request) {
        return menuCategoryMapper.toResponse(
                menuCategoryRepository.save(
                        menuCategoryMapper.toParentEntity(request)));
    }

    public MenuCategoryResponse createChildCategory(
            Long parentCategoryId, MenuCategoryRequest request) {
        validateParentCategoryExists(parentCategoryId);

        return menuCategoryMapper.toResponse(
                menuCategoryRepository.save(
                        menuCategoryMapper.toChildEntity(parentCategoryId, request)));
    }

    public MenuCategoryResponse updateParentCategory(
            Long parentCategoryId, MenuCategoryRequest request) {
        validateParentCategoryExists(parentCategoryId);
        MenuCategory menuCategory = getMenuCategoryById(parentCategoryId);
        updatePresentFields(menuCategory, request);

        return menuCategoryMapper.toResponse(
                menuCategoryRepository.save(menuCategory));
    }

    public MenuCategoryResponse updateChildCategory(
            Long childCategoryId, MenuCategoryRequest request) {
        validateChildCategoryExists(childCategoryId);
        MenuCategory menuCategory = getMenuCategoryById(childCategoryId);
        updatePresentFields(menuCategory, request);

        return menuCategoryMapper.toResponse(
                    menuCategoryRepository.save(menuCategory));
    }

    public void deleteParentCategoryById(Long id) {
        validateParentCategoryExists(id);
        validateParentCategoryHasNoChildren(id);

        menuCategoryRepository.deleteById(id);
    }

    public void deleteChildCategoryById(Long id) {
        validateChildCategoryExists(id);
        validateChildCategoryHasNoMenuItems(id);

        menuCategoryRepository.deleteById(id);
    }

    private void validateParentCategoryExists(Long id) {
        if (!menuCategoryRepository.existsByIdAndParentCategoryIsNull(id)) {
            throw new MenuCategoryNotFoundException(
                    "MenuCategory with id " + id + " not found or is not a parent category.");
        }
    }

    private MenuCategory getMenuCategoryById(Long id) {
        return menuCategoryRepository.findById(id).orElseThrow(
                () -> new MenuCategoryNotFoundException(
                        "MenuCategory with id " + id + " not found"));
    }

    private void updatePresentFields(MenuCategory menuCategory, MenuCategoryRequest request) {
        if (request.nameDefaultLanguage() != null && !request.nameDefaultLanguage().isBlank()) {
            menuCategory.setNameDefaultLanguage(request.nameDefaultLanguage());
        }
        if (request.nameSecondLanguage() != null && !request.nameSecondLanguage().isBlank()) {
            menuCategory.setNameSecondLanguage(request.nameSecondLanguage());
        }
    }

    private void validateChildCategoryExists(Long id) {
        if (!menuCategoryRepository.existsByIdAndParentCategoryIsNotNull(id)) {
            throw new MenuCategoryNotFoundException(
                    "MenuCategory with id " + id + " not found or is not a child category.");
        }
    }

    private void validateParentCategoryHasNoChildren(Long id) {
        if (menuCategoryRepository.existsByParentCategoryId(id)) {
            throw new MenuCategoryIsUsedException(
                    "Can't delete parent category with id " + id + " while it has children.");
        }
    }

    private void validateChildCategoryHasNoMenuItems(Long id) {
        if (menuItemRepository.existsByMenuCategoryId(id)) {
            throw new MenuCategoryIsUsedException(
                    "Can't delete child category with id " + id + " while it has menu items.");
        }
    }
}
