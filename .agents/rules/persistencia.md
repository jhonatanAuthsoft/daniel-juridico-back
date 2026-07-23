# Regras de Persistência e Exclusão - EcoPragas

Este documento descreve as diretrizes de persistência e políticas de exclusão de registros no sistema EcoPragas.

## 1. Política Geral de Exclusão (DELETE)

Para garantir a integridade dos dados e o histórico das operações, **não é permitida a exclusão física de registros que possuam vínculos ou dependências (chaves estrangeiras/referências) ativas em outras tabelas do sistema.**

### Fluxo de Validação
Ao acionar um endpoint de exclusão (`DELETE`), o respectivo serviço deve:
1. Verificar se a entidade a ser excluída existe (retornar `404 Not Found` caso contrário).
2. Consultar se existem registros vinculados em outras entidades do sistema.
3. Se houver dependências ativas:
   - Bloquear a exclusão.
   - Lançar um erro `CustomError` com status `400 Bad Request` informando que o registro possui dependências vinculadas.
4. Se não houver dependências, realizar a exclusão.

---

## 2. Mapeamento de Relacionamentos e Restrições

| Entidade | Dependências Mapeadas no Backend | Ação no DELETE se houver dependência |
|---|---|---|
| **Cliente** | `OrdemServicoEntity` e `AgendamentoEntity` | Bloqueado (`400 Bad Request`) |
| **Ordem de Serviço (OS)** | `AgendamentoEntity` | Bloqueado (`400 Bad Request`) |
| **Agendamento** | Nenhuma dependência externa impeditiva (atividades são filhas e são excluídas em cascata) | Permitido |
| **Lead** | Nenhuma dependência ativa | Permitido |
| **Técnico** | `OrdemServicoEntity` e `AgendamentoEntity` | Bloqueado (`400 Bad Request`) |
| **Usuário** | Se for perfil `TECNICO`, segue a restrição de Técnico. Senão, nenhuma dependência física direta | Permitido (se sem vínculos de técnico) |

---

## 3. Exemplos de Implementação

### Validação de Exclusão de Cliente
```java
boolean hasOS = ordemServicoRepository.existsByClienteId(id);
boolean hasAgendamento = agendamentoRepository.existsByClienteId(id);
if (hasOS || hasAgendamento) {
    throw new CustomError("Não é possível excluir o cliente pois ele possui ordens de serviço ou agendamentos vinculados", HttpStatus.BAD_REQUEST);
}
```

### Validação de Exclusão de Ordem de Serviço
```java
boolean hasAgendamento = agendamentoRepository.existsByOrdemServicoId(id);
if (hasAgendamento) {
    throw new CustomError("Não é possível excluir esta Ordem de Serviço pois ela está vinculada a um agendamento", HttpStatus.BAD_REQUEST);
}
```

### Validação de Exclusão de Técnico
```java
boolean hasOS = ordemServicoRepository.existsByTecnicoId(id);
boolean hasAgendamento = agendamentoRepository.existsByTecnicoId(id);
if (hasOS || hasAgendamento) {
    throw new CustomError("Não é possível excluir o técnico pois ele possui ordens de serviço ou agendamentos vinculados", HttpStatus.BAD_REQUEST);
}
```
