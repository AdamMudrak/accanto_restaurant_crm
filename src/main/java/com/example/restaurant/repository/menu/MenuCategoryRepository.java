package com.example.restaurant.repository.menu;

import com.example.restaurant.entity.menu.MenuCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    boolean existsByIdAndParentCategoryIsNull(Long id);

    boolean existsByIdAndParentCategoryIsNotNull(Long id);

    boolean existsByParentCategoryId(Long parentCategoryId);

    @Query("select mc.parentCategory.id from MenuCategory mc where mc.id = :id")
    Long findParentCategoryIdById(Long id);

    Page<MenuCategory> findAllByParentCategoryIsNull(Pageable pageable);

    Page<MenuCategory> findAllByParentCategoryId(Long parentId, Pageable pageable);
}
