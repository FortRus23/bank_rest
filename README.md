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

При старте автоматически применяются Liquibase-миграции:
- `users`
- `roles`
- `user_roles`
- `cards`
- начальные роли `ROLE_ADMIN`, `ROLE_USER`

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
  -d '{"email":"user@test.com","password":"Password123"}'
```

Ответ вернет JWT-токен.

### Проверка защищенного endpoint

```bash
curl http://localhost:8080/api/test/secure \
  -H "Authorization: Bearer <TOKEN>"
```

Без токена endpoint вернет `401 Unauthorized`.

## OpenAPI

- Спецификация: `docs/openapi.yaml`
