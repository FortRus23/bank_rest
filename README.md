# Система Управления Банковскими Картами

## Запуск
### 1) Поднять PostgreSQL

```bash
docker compose up -d
```

### 2) Запустить приложение

```bash
./mvnw spring-boot:run
```

При старте автоматически применяются Liquibase-миграции.

## Проверка аутентификации

### Готовый admin

- email: `admin@bank.local`
- password: `Admin123!`

### Регистрация

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@test.com","password":"Password123","fullName":"Test User"}'
```

### Логин

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bank.local","password":"Admin123!"}'
```

Ответ вернет JWT-токен.

## Основные API

- Auth: `/api/auth/*`
- Cards admin: `/api/cards/admin*`
- Cards me: `/api/cards/me*`
- Transfers: `POST /api/transfers/me`

## OpenAPI

- Спецификация: `docs/openapi.yaml`
