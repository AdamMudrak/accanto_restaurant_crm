package com.example.restaurant.service.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Component;

@Component
public class JsonConverter {
    private final ObjectMapper objectMapper;

    public JsonConverter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public <T> T fromJson(String json, Class<T> targetClass) {
        try {
            return objectMapper.readValue(json, targetClass);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to deserialize JSON into " + targetClass.getSimpleName(),
                    exception
            );
        }
    }

    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException(
                    "Failed to serialize object into JSON",
                    exception
            );
        }
    }
}
