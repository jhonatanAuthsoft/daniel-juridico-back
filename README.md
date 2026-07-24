# Laweact Server

API Java 21 + Spring Boot 3 em arquitetura em camadas (padrão ecopragas), Postgres, JWT, Flyway e Swagger.

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

## Cadastro (escopo DER)

Cadastro genérico de usuário foi removido. Use as rotas específicas:

### Cliente — `POST /clientes/cadastrar`

Cria `usuarios` + `clientes` + `enderecos` e retorna JWT (envelope `ApiResponse`).

```bash
curl -s -X POST http://localhost:8080/clientes/cadastrar \
  -H 'Content-Type: application/json' \
  -d '{
    "nomeCompleto": "Maria Silva",
    "email": "maria@laweact.com",
    "senha": "Secret12",
    "profissao": "Analista",
    "tipoDocumento": "CPF",
    "numeroDocumento": "52998224725",
    "rg": "1234567",
    "dataNascimento": "1990-05-20",
    "pronomes": "ELA",
    "telefone": "11999999999",
    "cep": "01310-100",
    "logradouro": "Av. Paulista",
    "numero": "1000",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "aceiteTermos": true
  }'
```

### Advogado — `POST /advogados/cadastrar`

Cria `usuarios` + `advogados` + `enderecos` + `oabs` + `areas_atuacao_advogado` e retorna JWT.

```bash
curl -s -X POST http://localhost:8080/advogados/cadastrar \
  -H 'Content-Type: application/json' \
  -d '{
    "nomeCompleto": "João Advogado",
    "email": "joao@laweact.com",
    "senha": "Secret12",
    "rg": "1234567",
    "rgOrgaoEmissor": "SSP",
    "rgUf": "SP",
    "cpf": "39053344705",
    "nomePai": "José Advogado",
    "nomeMae": "Ana Advogada",
    "pronomeTratamento": "DOUTOR",
    "telefone": "11988887777",
    "universidade": "USP",
    "curso": "Direito",
    "anoFormacao": 2015,
    "atuacaoDesde": "2016-01-10",
    "cep": "01310-100",
    "logradouro": "Av. Paulista",
    "numero": "1500",
    "bairro": "Bela Vista",
    "cidade": "São Paulo",
    "estado": "SP",
    "oabPrincipal": { "numero": "123456", "uf": "SP" },
    "areasAtuacao": [{ "estado": "SP", "cidade": "São Paulo" }],
    "aceiteTermos": true
  }'
```

### Login

```bash
curl -s -X POST http://localhost:8080/usuarios/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@laweact.com","senha":"Secret12"}'
```

Altere `jwt.secret` em `application-local.properties` antes de ambientes compartilhados.
