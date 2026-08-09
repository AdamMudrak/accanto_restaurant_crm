package com.example.restaurant.mapper.order;

import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public Guest toCreateGuest(Order order) {
        Guest guest = new Guest();
        guest.setUuid(UUID.randomUUID().toString());
        guest.setOrder(order);

        return guest;
    }
}
