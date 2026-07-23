---
trigger: always_on
---

# Padrão de Resposta da API

## Objetivo

Todas as APIs devem retornar uma estrutura padronizada para facilitar o consumo pelo frontend, logs, monitoramento e tratamento de erros.

---

# Estrutura Base

```json
{
  "success": true,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Operação realizada com sucesso",
  "data": {},
  "errors": []
}
```

## Campos

| Campo     | Tipo    | Obrigatório | Descrição               |
| --------- | ------- | ----------- | ----------------------- |
| success   | boolean | Sim         | Indica sucesso ou falha |
| timestamp | string  | Sim         | Data/hora ISO-8601      |
| message   | string  | Sim         | Mensagem amigável       |
| data      | object  | Não         | Dados retornados        |
| errors    | array   | Não         | Lista de erros          |

---

# Retorno de Sucesso

## HTTP 200 - OK

```json
{
  "success": true,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Consulta realizada com sucesso",
  "data": {
    "id": 1,
    "nome": "João"
  }
}
```

---

## HTTP 201 - Created

```json
{
  "success": true,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Usuário criado com sucesso",
  "data": {
    "id": 1
  }
}
```

---

## HTTP 204 - No Content

Sem corpo de resposta.

---

# Retorno de Erro

## Estrutura

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Erro ao processar requisição",
  "errors": [
    {
      "code": "USER_NOT_FOUND",
      "field": null,
      "detail": "Usuário não encontrado"
    }
  ]
}
```

---

# HTTP 400 - Bad Request

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Dados inválidos",
  "errors": [
    {
      "code": "INVALID_REQUEST",
      "field": "email",
      "detail": "Formato inválido"
    }
  ]
}
```

---

# HTTP 401 - Unauthorized

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Não autenticado",
  "errors": [
    {
      "code": "UNAUTHORIZED",
      "detail": "Token inválido ou expirado"
    }
  ]
}
```

---

# HTTP 403 - Forbidden

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Acesso negado",
  "errors": [
    {
      "code": "FORBIDDEN",
      "detail": "Permissão insuficiente"
    }
  ]
}
```

---

# HTTP 404 - Not Found

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Recurso não encontrado",
  "errors": [
    {
      "code": "RESOURCE_NOT_FOUND",
      "detail": "Registro não encontrado"
    }
  ]
}
```

---

# HTTP 422 - Validation Error

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Erro de validação",
  "errors": [
    {
      "code": "REQUIRED_FIELD",
      "field": "nome",
      "detail": "Campo obrigatório"
    },
    {
      "code": "INVALID_EMAIL",
      "field": "email",
      "detail": "Email inválido"
    }
  ]
}
```

---

# HTTP 500 - Internal Server Error

```json
{
  "success": false,
  "timestamp": "2026-06-18T14:30:00Z",
  "message": "Erro interno do servidor",
  "errors": [
    {
      "code": "INTERNAL_ERROR",
      "detail": "Erro inesperado"
    }
  ]
}
```

---

# Paginação

```json
{
  "success": true,
  "message": "Consulta realizada com sucesso",
  "data": [
    {
      "id": 1,
      "nome": "João"
    }
  ],
  "pagination": {
    "page": 1,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5
  }
}
```

---

# Convenções

* Utilizar mensagens em português.
* Utilizar códigos de erro em UPPER_SNAKE_CASE.
* Nunca retornar stacktrace para o cliente.
* Sempre preencher timestamp.
* Utilizar HTTP Status apropriado.
* Em erros de validação retornar todos os erros encontrados.
* Campos nulos devem ser omitidos quando possível.
* Toda API REST deve seguir este padrão sem exceções.
