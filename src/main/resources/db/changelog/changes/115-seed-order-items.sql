INSERT INTO order_items (
    id,
    guest_id,
    order_id,
    menu_item_id,
    payment_id,
    item_name_snapshot,
    item_price_snapshot,
    quantity,
    status,
    client_note,
    employee_note
)
VALUES (1, 1, 1, 6, 1, 'Classic Burger', 240.00, 2, 'PAID_CARD', NULL, NULL),
       (2, 2, 2, 7, 2, 'Black Burger', 270.00, 1, 'PAID_CARD', NULL, NULL),
       (3, 2, 2, 2, 2, 'Latte', 105.00, 1, 'PAID_CARD', NULL, NULL),
       (4, 3, 3, 8, NULL, 'Fruit Salad', 155.00, 1, 'PAID_CASH', NULL, NULL),
       (5, 3, 3, 9, NULL, 'Cheesecake', 165.00, 1, 'PAID_CASH', NULL, NULL),
       (6, 4, 4, 1, NULL, 'Espresso', 75.00, 2, 'PAID_CASH', NULL, NULL),
       (7, 4, 4, 10, NULL, 'Macaron Assortment', 190.00, 1, 'PAID_CASH', NULL, NULL),
       (8, 5, 5, 4, NULL, 'Orange Juice', 95.00, 1, 'PAID_CASH', NULL, NULL),
       (9, 5, 5, 5, NULL, 'Chocolate Milkshake', 145.00, 1, 'PAID_CASH', NULL, NULL);
