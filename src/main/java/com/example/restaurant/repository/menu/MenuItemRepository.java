package com.example.restaurant.repository.menu;

import com.example.restaurant.entity.menu.MenuItem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    boolean existsByMenuCategoryId(Long menuCategoryId);

    Page<MenuItem> findAllByIsAvailableTrue(Pageable pageable);

    Page<MenuItem> findAllByMenuCategoryIdAndIsAvailableTrue(Long menuCategoryId,
                                                             Pageable pageable);

    Optional<MenuItem> findByIdAndIsAvailableTrue(Long id);

}
