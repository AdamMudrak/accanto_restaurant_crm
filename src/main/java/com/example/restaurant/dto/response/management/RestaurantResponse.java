package com.example.restaurant.dto.response.management;

import com.example.restaurant.entity.management.Restaurant;
import com.example.restaurant.service.payment.PaymentProviderType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Restaurant profile returned by the API.")
public record RestaurantResponse(
        @Schema(description = "Restaurant identifier.", example = "1")
        Long id,
        @Schema(description = "Restaurant display name.", example = "Ukrainian Restaurant")
        String name,
        @Schema(description = "Logo image identifier.", example = "1")
        Long logoId,
        @Schema(description = "Background image identifier.", example = "2")
        Long backgroundImageId,
        @Schema(description = "Default business region.", example = "UKRAINE")
        Restaurant.DefaultRegion defaultRegion,
        @Schema(description = "Language used by restaurant-generated text.",
                example = "UA")
        Restaurant.Language language,
        @Schema(description = "Currency used by the restaurant.", example = "UAH")
        Restaurant.Currency currency,
        @Schema(description = "Payment provider used by the restaurant.", example = "MONOBANK")
        PaymentProviderType paymentProvider,
        @Schema(description = "IANA business time zone.", example = "Europe/Kyiv")
        String timeZone
) {}
