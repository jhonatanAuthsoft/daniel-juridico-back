# Laweact Server

API Java 21 + Spring Boot 3 com Clean Architecture (seção `auth` de exemplo), Postgres e Swagger.

## Pré-requisitos

- Java 21+
- Maven 3.9+ (ou o wrapper `./mvnw` após gerá-lo)
- Docker / Docker Compose

## Subir o banco (só Postgres)

```bash
docker compose up -d
```

Credenciais padrão: `laweact` / `laweact` — database `laweact` em `localhost:5432`.

## Rodar a API

```bash
./mvnw spring-boot:run
# ou
mvn spring-boot:run
```

- API: http://localhost:8080  
- Swagger UI: http://localhost:8080/swagger-ui.html  
- Health: http://localhost:8080/actuator/health  

## Endpoints de auth

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/auth/register` | público |
| POST | `/api/auth/login` | público |
| GET | `/api/auth/me` | Bearer JWT |

Exemplo:

```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@laweact.com","password":"secret12"}'
```

## Estrutura

```
com.laweact
├── shared/     # security, config, exceptions
└── auth/       # feature de exemplo
    ├── domain/
    ├── application/
    ├── infrastructure/
    └── presentation/
```

Troque `laweact.security.jwt.secret` em `application.yml` antes de qualquer ambiente compartilhado.
