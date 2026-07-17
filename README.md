# Laweact Server

API Java 21 + Spring Boot 3 em arquitetura em camadas (padrão ecopragas), Postgres, JWT e Swagger.

## Estrutura

```
com.laweact/
├── controller/
├── service/ + service/imp/
├── repository/
├── model/entity/ + model/enums/
├── dto/
├── mapper/
├── config/
└── util/
```

## Pré-requisitos

- Java 21+
- Docker / Docker Compose

## Subir o banco (Postgres)

```bash
docker compose up -d
```

Credenciais: `laweact` / `laweact` — database `laweact` em `localhost:5432`.

## Rodar a API

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## Exemplos (curl)

Cadastrar:

```bash
curl -s -X POST http://localhost:8080/usuarios/cadastrar \
  -H 'Content-Type: application/json' \
  -d '{"nomeCompleto":"Demo User","email":"demo@laweact.com","senha":"secret12","perfil":"CLIENTE"}'
```

Login:

```bash
curl -s -X POST http://localhost:8080/usuarios/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@laweact.com","senha":"secret12"}'
```

Me (substitua `TOKEN`):

```bash
curl -s http://localhost:8080/usuarios/me -H "Authorization: Bearer TOKEN"
```

Altere `jwt.secret` em `application-local.properties` antes de ambientes compartilhados.
