package com.example.restaurant.repository.menu;

import com.example.restaurant.entity.menu.MenuCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    boolean existsByIdAndParentCategoryIsNull(Long id);

    boolean existsByIdAndParentCategoryIsNotNull(Long id);

    boolean existsByParentCategoryId(Long parentCategoryId);

    Page<MenuCategory> findAllByParentCategoryIsNull(Pageable pageable);

    Page<MenuCategory> findAllByParentCategoryId(Long parentId, Pageable pageable);
}
