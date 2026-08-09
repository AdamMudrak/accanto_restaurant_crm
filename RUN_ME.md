# Run The Application

## Important

Local `.env.local` values are intentionally dummy values. They are enough for the
application to start locally, run migrations, and test most non-provider flows.

The project is 100% functional only on the remote environment, where real
payment provider, email, public URL, webhook, and production cookie settings are
configured.

## Folder layout

`restaurant` and `Restaurant-CRM` must be in the same parent folder:

```text
all_restaurant/
  restaurant/
  Restaurant-CRM/
```

## Full stack: database + backend + frontend

From `restaurant`:

```bash
docker-compose --env-file .env.local --profile frontend up --build
```

Frontend URL:

```text
http://localhost:3000
```

If the frontend folder is missing, use the backend-only command below.

## Backend only: database + backend

From `restaurant`:

```bash
docker-compose --env-file .env.local up --build
```

Backend URL:

```text
http://localhost:8083
```

Swagger/OpenAPI URL:

```text
http://localhost:8083/swagger-ui/index.html
```

Local limitations:

- email sending uses a dummy Resend key;
- Monobank/Stripe payment creation and provider sync need real remote secrets;
- webhooks need a public URL and provider configuration;
- production cookie/CORS behavior is represented only on the remote deployment.

## Stop all

From `restaurant`:

```bash
docker-compose --env-file .env.local down
```

## Stop all and delete database volume

From `restaurant`:

```bash
docker-compose --env-file .env.local down -v
```
