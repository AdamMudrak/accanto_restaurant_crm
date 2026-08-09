INSERT INTO menu_categories (id, name_default_language, name_second_language, parent_id)
VALUES (1, 'Drinks', 'Напої', null),
       (2, 'Food', 'Їжа', null),
       (3, 'Coffee', 'Кава', 1),
       (4, 'Tea', 'Чай', 1),
       (5, 'Other', 'Інше', 1),
       (6, 'Burgers', 'Бургери', 2),
       (7, 'Salads', 'Салати', 2),
       (8, 'Sweeties', 'Солодощі', 2);
