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
    "estado": "SP"
  }'
```

Depois do cadastro/login (com JWT):

```bash
curl -s -X POST http://localhost:8080/usuarios/aceitar-termos \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{"checkboxConfirmado": true, "scrollConfirmado": true}'
```

`usuario.termosAceitos` vem `false` no cadastro/login até esse aceite.

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
    "oabPrincipal": { "numero": "123456", "uf": "SP", "dataExpedicao": "2016-03-15" },
    "areasAtuacao": [{ "estado": "SP", "cidade": "São Paulo" }],
    "modalidades": ["GENERALISTA"],
    "especialidades": [{ "especialidadeCodigo": "CIVIL" }],
    "formasCobranca": ["HONORARIOS_CONTRATUAIS"]
  }'
```

### Login

```bash
curl -s -X POST http://localhost:8080/usuarios/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@laweact.com","senha":"Secret12"}'
```

### Recuperação de senha

Código de 4 dígitos (15 min, uso único). Resposta genérica (não revela se o e-mail existe). Sem SMTP configurado, o código só aparece no log do servidor.

```bash
# 1) Solicitar código
curl -s -X POST http://localhost:8080/usuarios/recuperar-senha \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@laweact.com"}'

# 2) Validar código (não consome)
curl -s -X POST http://localhost:8080/usuarios/validar-codigo-recuperacao \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@laweact.com","codigo":"1234"}'

# 3) Redefinir senha
curl -s -X POST http://localhost:8080/usuarios/redefinir-senha \
  -H 'Content-Type: application/json' \
  -d '{"email":"maria@laweact.com","codigo":"1234","novaSenha":"NovaSenha1","confirmarSenha":"NovaSenha1"}'
```

Eventos ficam em `auditoria_eventos` com `evento = recuperacao_senha`.

## Testes E2E

Os testes sobem a API em porta aleatória e batem nos endpoints HTTP de verdade.

**Padrão:** um único Postgres (Testcontainers singleton) para toda a suíte.
Isolamento = `TRUNCATE` entre testes — o container **não** é destruído a cada classe.

```bash
# Docker Desktop aberto
./mvnw test
```

**Alternativa manual** (banco fixo na porta 5433):

```bash
docker compose up -d db-test
```

Cobertura:
- `POST /clientes/cadastrar` — sucesso, e-mail/documento duplicados, validações de senha/e-mail, CPF/CNPJ
- `POST /advogados/cadastrar` — sucesso, e-mail/CPF/OAB duplicados, modalidades
- `POST /usuarios/login` — cliente/advogado, JWT em `/me`, senha/e-mail inválidos
- `POST /usuarios/aceitar-termos` — registra aceite; `usuario.termosAceitos` no login/cadastro
- `POST /usuarios/recuperar-senha` / `validar-codigo-recuperacao` / `redefinir-senha` — código, invalidação, login com nova senha

Cada teste limpa o banco (`TRUNCATE … CASCADE`) no `beforeEach`/`afterEach` (AAA + isolamento).

Altere `jwt.secret` em `application-local.properties` antes de ambientes compartilhados.

