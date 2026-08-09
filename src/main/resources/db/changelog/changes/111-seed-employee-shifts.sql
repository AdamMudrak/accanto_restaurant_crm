INSERT INTO employee_shifts (
    id,
    employee_id,
    work_date,
    starts_at,
    ends_at,
    note,
    created_at,
    updated_at
)
VALUES (1, 2, '2026-08-09', '09:00:00', '17:00:00', 'Morning service',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (2, 3, '2026-08-09', '12:00:00', '20:00:00', 'Lunch and dinner service',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (3, 4, '2026-08-10', '10:00:00', '18:00:00', 'Regular shift',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (4, 2, '2026-08-11', '14:00:00', '22:00:00', 'Evening service',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
       (5, 3, '2026-08-12', '09:00:00', '17:00:00', 'Morning service',
        CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
