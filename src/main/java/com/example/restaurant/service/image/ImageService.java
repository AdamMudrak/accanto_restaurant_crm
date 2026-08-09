package com.example.restaurant.service.image;

import com.example.restaurant.entity.image.Image;
import com.example.restaurant.exception.types.badrequest.InvalidImageException;
import com.example.restaurant.exception.types.internalserver.ImageIoException;
import com.example.restaurant.exception.types.notfound.ImageNotFoundException;
import com.example.restaurant.repository.image.ImageRepository;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageService {
    private static final List<String> ALLOWED_EXTENSIONS =
            List.of("image/jpg", "image/jpeg", "image/png");
    private final ImageRepository imageRepository;

    public Image save(MultipartFile file) {
        throwIfEmpty(file);

        try {
            Image image = new Image(
                    getFileName(file),
                    getContentType(file),
                    file.getBytes()
            );

            return imageRepository.save(image);
        } catch (IOException e) {
            throw new ImageIoException("Could not save image.", e);
        }
    }

    public Image findById(Long id) {
        return imageRepository.findById(id)
                .orElseThrow(() -> new ImageNotFoundException("Image with id "
                        + id + " not found."));
    }

    public void delete(Long id) {
        if (!imageRepository.existsById(id)) {
            throw new ImageNotFoundException("Image with id " + id + " not found.");
        }
        imageRepository.deleteById(id);
    }

    private String getFileName(MultipartFile file) {
        return file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "image";
    }

    private String getContentType(MultipartFile file) {
        if (file.getContentType() == null
                || !ALLOWED_EXTENSIONS.contains(file.getContentType())) {
            throw new InvalidImageException(
                    "Can't process this image. Please use jpg, jpeg or png.");
        }

        return file.getContentType();
    }

    private void throwIfEmpty(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidImageException("File you offered is empty!");
        }
    }
}
