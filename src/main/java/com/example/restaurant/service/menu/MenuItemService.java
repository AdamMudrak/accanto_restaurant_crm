package com.example.restaurant.service.menu;

import com.example.restaurant.dto.request.menu.MenuItemRequest;
import com.example.restaurant.dto.request.menu.UpdateMenuItemRequest;
import com.example.restaurant.dto.response.list.menu.MenuItemListResponse;
import com.example.restaurant.dto.response.menu.MenuItemResponse;
import com.example.restaurant.entity.image.Image;
import com.example.restaurant.entity.menu.MenuItem;
import com.example.restaurant.exception.types.notfound.MenuCategoryNotFoundException;
import com.example.restaurant.exception.types.notfound.MenuItemNotFoundException;
import com.example.restaurant.mapper.menu.MenuItemMapper;
import com.example.restaurant.repository.menu.MenuCategoryRepository;
import com.example.restaurant.repository.menu.MenuItemRepository;
import com.example.restaurant.service.image.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class MenuItemService {
    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;
    private final MenuCategoryRepository menuCategoryRepository;
    private final ImageService imageService;

    public MenuItemResponse findMenuItemById(Long id) {
        return menuItemMapper.toMenuItemResponse(getAvailableMenuItemById(id));
    }

    public MenuItemListResponse findAllMenuItems(Pageable pageable) {
        Page<MenuItem> items = menuItemRepository.findAllByIsAvailableTrue(pageable);
        List<MenuItemResponse> itemResponses =
                menuItemMapper.toMenuItemResponseList(items.stream().toList());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = items.getTotalElements();

        return new MenuItemListResponse(
                itemResponses,
                pageNumber,
                pageSize,
                totalElements
        );
    }

    public MenuItemListResponse findAllMenuItemsByCategoryId(
            Pageable pageable, Long childCategoryId) {
        if (!menuCategoryRepository.existsByIdAndParentCategoryIsNotNull(childCategoryId)) {
            throw new MenuCategoryNotFoundException(
                    "Category with id " + childCategoryId + "not found or is not a child category");
        }
        Page<MenuItem> items = menuItemRepository
                .findAllByMenuCategoryIdAndIsAvailableTrue(childCategoryId, pageable);
        List<MenuItemResponse> itemResponses =
                menuItemMapper.toMenuItemResponseList(items.toList());
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        long totalElements = items.getTotalElements();

        return new MenuItemListResponse(
                itemResponses,
                pageNumber,
                pageSize,
                totalElements
        );
    }

    public MenuItemResponse createMenuItem(MenuItemRequest request,
                                           MultipartFile itemImage) {
        Long childCategoryId = request.childCategoryId();
        MenuItem menuItem = menuItemMapper.toCreateEntity(request);

        if (!menuCategoryRepository.existsByIdAndParentCategoryIsNotNull(childCategoryId)) {
            throw new MenuCategoryNotFoundException(
                    "Category with id " + childCategoryId + "not found or is not a child category");
        }

        if (itemImage != null) {
            Image image = imageService.save(itemImage);
            menuItem.setItemImage(image);
        }

        return menuItemMapper.toMenuItemResponse(menuItemRepository.save(menuItem));
    }

    public MenuItemResponse updateMenuItem(Long menuItemId,
                                           UpdateMenuItemRequest request,
                                           MultipartFile itemImage) {
        MenuItem menuItem = getAnyMenuItemById(menuItemId);

        if (request != null) {
            Long childCategoryId = request.childCategoryId();
            if (childCategoryId != null
                    && !menuCategoryRepository.existsByIdAndParentCategoryIsNotNull(
                            childCategoryId)) {
                throw new MenuCategoryNotFoundException(
                        "Category with id " + childCategoryId
                                + "not found or is not a child category");
            }
            menuItem = menuItemMapper.toUpdateEntity(menuItem, request);
        }

        setImage(itemImage, menuItem);

        return menuItemMapper.toMenuItemResponse(menuItem);
    }

    public void deleteMenuItem(Long menuItemId) {
        MenuItem menuItem = getAnyMenuItemById(menuItemId);
        menuItem.setAvailable(false);
        menuItemRepository.save(menuItem);
    }

    private MenuItem getAvailableMenuItemById(Long id) {
        return menuItemRepository.findByIdAndIsAvailableTrue(id)
                .orElseThrow(() ->
                        new MenuItemNotFoundException("MenuItem with id " + id + " not found"));
    }

    private MenuItem getAnyMenuItemById(Long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() ->
                        new MenuItemNotFoundException("MenuItem with id " + id + " not found"));
    }

    private void setImage(MultipartFile itemImage, MenuItem menuItem) {
        if (itemImage != null) {
            Image image = imageService.save(itemImage);

            Long prevItemImageId = null;
            if (menuItem.getItemImage() != null) {
                prevItemImageId = menuItem.getItemImage().getId();
            }
            menuItem.setItemImage(image);
            menuItemRepository.save(menuItem);
            menuItemRepository.flush();

            if (prevItemImageId != null) {
                imageService.delete(prevItemImageId);
            }
        } else {
            menuItemRepository.save(menuItem);
        }
    }
}
