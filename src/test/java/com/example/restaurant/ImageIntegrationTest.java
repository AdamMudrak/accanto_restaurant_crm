package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.restaurant.exception.types.badrequest.InvalidImageException;
import com.example.restaurant.exception.types.notfound.ImageNotFoundException;
import com.example.restaurant.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;

class ImageIntegrationTest extends AbstractIntegrationTest {
    @Test
    void uploadedImageIsRetrievedPubliclyWithCorrectBytesHeadersAndCanBeDeleted() throws Exception {
        var saved = images.save(png());
        var response = request("GET", "/images/" + saved.getId(), null, null);
        assertThat(response.status()).isEqualTo(200);
        assertThat(response.bytes()).isEqualTo(png().getBytes());
        assertThat(response.headers().firstValue("content-type")).contains("image/png");
        assertThat(response.headers().firstValue("content-disposition").orElseThrow()).contains("test.png");
        images.delete(saved.getId());
        assertThat(request("GET", "/images/" + saved.getId(), null, null).status()).isEqualTo(404);
        assertThatThrownBy(() -> images.delete(saved.getId())).isInstanceOf(ImageNotFoundException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg", "image/jpg"})
    void supportedImageTypesAreStored(String type) throws Exception {
        var saved = images.save(new MockMultipartFile("image", "test", type, png().getBytes()));
        assertThat(images.findById(saved.getId()).getType()).isEqualTo(type);
    }

    @ParameterizedTest
    @ValueSource(strings = {"text/html", "image/svg+xml", "application/octet-stream", "missing"})
    void unsupportedImageTypesDoNotCreateDatabaseRows(String type) throws Exception {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM images", Long.class);
        assertThatThrownBy(() -> images.save(new MockMultipartFile("image", "file",
                type.equals("missing") ? null : type, png().getBytes())))
                .isInstanceOf(InvalidImageException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM images", Long.class)).isEqualTo(before);
    }

    @Test
    void emptyUploadAndInvalidImageIdAreRejected() throws Exception {
        assertThatThrownBy(() -> images.save(new MockMultipartFile("image", new byte[0])))
                .isInstanceOf(InvalidImageException.class);
        assertThat(request("GET", "/images/0", null, null).status()).isEqualTo(400);
    }
}
