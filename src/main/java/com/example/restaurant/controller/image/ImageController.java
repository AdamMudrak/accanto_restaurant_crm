package com.example.restaurant.controller.image;

import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.INTERNAL_ERROR;
import static com.example.restaurant.controller.openapi.ManagementOpenApiExamples.VALIDATION_ERROR;

import com.example.restaurant.entity.image.Image;
import com.example.restaurant.exception.types.notfound.ImageNotFoundException;
import com.example.restaurant.service.image.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/images")
@Tag(
        name = "Images",
        description = "Image retrieval endpoint.")
public class ImageController {
    private final ImageService imageService;

    @Operation(
            summary = "Get image",
            description = "Returns image binary content by identifier.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Image binary content.",
                    content = @Content(mediaType = "image/*")),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Path variable validation failed.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = VALIDATION_ERROR))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Image was not found."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error.",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = INTERNAL_ERROR)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<Resource> findById(@PathVariable @Positive Long id) {
        Image image = imageService.findById(id);

        Resource resource = toResource(image);

        return toResponseEntity(image, resource);
    }

    private Resource toResource(Image image) {
        return new ByteArrayResource(image.getData()) {
            @Override
            public String getFilename() {
                return image.getName();
            }
        };
    }

    private ResponseEntity<Resource> toResponseEntity(Image image, Resource resource) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getType()))
                .contentLength(image.getData().length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(image.getName())
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @ExceptionHandler(ImageNotFoundException.class)
    public ResponseEntity<Void> handleImageNotFoundException() {
        return ResponseEntity.notFound().build();
    }
}
