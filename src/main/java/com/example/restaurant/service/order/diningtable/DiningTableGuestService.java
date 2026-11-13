package com.example.restaurant.service.order.diningtable;

import com.example.restaurant.entity.order.DiningTable;
import com.example.restaurant.entity.order.Guest;
import com.example.restaurant.entity.order.Order;
import com.example.restaurant.exception.types.notfound.DiningTableNotFoundException;
import com.example.restaurant.mapper.order.GuestMapper;
import com.example.restaurant.mapper.order.OrderMapper;
import com.example.restaurant.repository.management.RestaurantRepository;
import com.example.restaurant.repository.order.DiningTableRepository;
import com.example.restaurant.repository.order.GuestRepository;
import com.example.restaurant.repository.order.OrderRepository;
import com.example.restaurant.service.utils.CookieExtractor;
import com.example.restaurant.service.utils.CookieSetter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DiningTableGuestService {
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderMapper orderMapper;
    private final GuestRepository guestRepository;
    private final GuestMapper guestMapper;
    private final CookieSetter cookieSetter;
    private final DiningTableRepository diningTableRepository;
    @Value("${security.guest.cookie.expiration}")
    private String guestExpiration;
    @Value("${security.table.cookie.expiration}")
    private String tableExpiration;
    @Value("${security.order.cookie.expiration}")
    private String orderExpiration;

    public void interactWithTableByQrUuid(String tableUuid,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        restaurantRepository.lockSingleton().orElseThrow();
        DiningTable diningTable = diningTableRepository
                .findByQrUuidAndIsActiveTrue(tableUuid).orElseThrow(
                        () -> new DiningTableNotFoundException(
                                "Active dining table with uuid " + tableUuid + " not found"));

        Optional<Long> optionalOrder = orderRepository
                .findOpenOrderIdByTableId(diningTable.getId());

        if (optionalOrder.isPresent()) {
            Order order = orderRepository.lockById(optionalOrder.get()).orElseThrow();
            if (order.getStatus() == Order.OrderStatus.OPENED) {
                joinOrder(tableUuid, order, request, response);
            } else {
                initializeOrder(tableUuid, diningTable.getId(), response);
            }
        } else {
            initializeOrder(tableUuid, diningTable.getId(), response);
        }
    }

    private void joinOrder(String tableUuid,
                           Order order,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        Optional<Guest> guest = findGuestByRequestCookie(request);
        if (guest.isEmpty() || !guest.get().getOrder().getId().equals(order.getId())) {
            createGuestAndAddCookies(order, tableUuid, response);
            return;
        }

        addCookies(guest.get(), tableUuid, order, response);
    }

    private void initializeOrder(String tableUuid,
                                 Long diningTableId,
                                 HttpServletResponse response) {
        Order order = createOrder(diningTableId);
        createGuestAndAddCookies(order, tableUuid, response);
    }

    private Order createOrder(Long diningTableId) {
        Order order = orderMapper.toCreateOrder(diningTableId);
        orderRepository.save(order);
        orderRepository.flush();

        return order;
    }

    private Guest createGuest(Order order) {
        Guest guest = guestMapper.toCreateGuest(order);

        return guestRepository.save(guest);
    }

    private void createGuestAndAddCookies(Order order,
                                          String tableUuid,
                                          HttpServletResponse response) {
        Guest guest = createGuest(order);
        addCookies(guest, tableUuid, order, response);
    }

    private void addCookies(Guest guest,
                            String tableUuid,
                            Order order,
                            HttpServletResponse response) {
        cookieSetter.addCookie("guestUuid", guest.getUuid(), guestExpiration, response);
        cookieSetter.addCookie("tableUuid", tableUuid, tableExpiration, response);
        cookieSetter.addCookie("orderUuid", order.getUuid(), orderExpiration, response);
    }

    private Optional<Guest> findGuestByRequestCookie(HttpServletRequest request) {
        String guestUuid = CookieExtractor.extractCookieValueByName(request, "guestUuid");
        if (guestUuid == null || guestUuid.isBlank()) {
            return Optional.empty();
        }

        return guestRepository.findByUuid(guestUuid);
    }
}
