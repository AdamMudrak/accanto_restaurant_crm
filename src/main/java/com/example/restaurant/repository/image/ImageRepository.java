package com.example.restaurant.repository.image;

import com.example.restaurant.entity.image.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
