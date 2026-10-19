# Restaurant Backend

Backend API for a restaurant ordering and management system. The application
covers guest ordering flows, employee/admin management flows, menu management,
cashless payments, payment status synchronization, analytics, and API
documentation through Swagger/OpenAPI.

## Core functionality

- Guest-facing order flow:
  - create and read current table order;
  - add or update guest order items;
  - return the full order with the current guest identified first, so the client
    can highlight this guest's items and still show the whole table order.
- Employee/admin order flow:
  - manage dining tables and orders;
  - update item statuses and employee comments;
  - view order history.
- Menu management:
  - menu categories and child categories;
  - menu items, localized names/descriptions, ingredients, prices, and images;
  - soft-delete behavior for menu entities.
- Employee and security flows:
  - employee login/logout;
  - JWT-based access/refresh cookies;
  - employee profile update;
  - registration, email change, password reset, and related email actions.
- Payments:
  - guest payment creation;
  - employee/admin payment lookup;
  - Monobank and Stripe integrations;
  - provider webhook handling;
  - scheduled pending payment status synchronization.
- Management and analytics:
  - restaurant configuration;
  - schedule and employee shifts;
  - monthly cashless revenue/tips analytics;
  - cashless tip payout tracking.

## Technology stack

- Java 25
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA / Hibernate
- MySQL 8.4
- Liquibase
- Maven
- Lombok
- Bean Validation
- Springdoc OpenAPI / Swagger UI
- JWT (`jjwt`)
- Resend Java SDK
- Monobank API integration
- Stripe API integration
- ZXing for QR-code generation
- Docker / Docker Compose

## API documentation

Swagger UI is available at:

- Local: <http://localhost:8083/swagger-ui/index.html>
- Remote: <https://accanto.adammudrak.pp.ua/swagger-ui/index.html>

OpenAPI JSON is available at:

- Local: <http://localhost:8083/v3/api-docs>
- Remote: <https://accanto.adammudrak.pp.ua/v3/api-docs>

The local environment uses dummy secrets by default. It is intended for local
startup, schema validation, and non-provider development flows.

The project is 100% functional only on the remote environment, where real email,
payment provider, public webhook URL, cookie, and production CORS settings are
configured.

## Local startup

Integration tests can run individually or as a suite with a shared MySQL and Spring
context. See [integration testing](docs/integration-tests.md) for commands and the
business scenario inventory.

See [frontend API changes](docs/frontend-api-changes.uk.md) for payment, currency,
employee deletion, review, and other API changes. Local Compose explicitly enables
demo data; production migrations preserve existing data.

See [RUN_ME.md](RUN_ME.md) for the exact local Docker commands.

Short version:

```bash
docker-compose --env-file .env.local up --build
```

Backend URL:

```text
http://localhost:8083
```

## License

This project is proprietary. Copyright and all rights are reserved by Adam
Mudrak, resident of Ukraine.

See [LICENSE.md](LICENSE.md) for permitted educational use and usage restrictions.
