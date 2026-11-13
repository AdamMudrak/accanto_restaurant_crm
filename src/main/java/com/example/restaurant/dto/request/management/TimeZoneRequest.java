package com.example.restaurant.dto.request.management;

import jakarta.validation.constraints.NotBlank;

public record TimeZoneRequest(@NotBlank String timeZone) {}
