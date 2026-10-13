# Atendimento Service

Serviço de Departamentos, Categorias, SLAs, Atendentes e Filas de Atendimento.
Porta 8092.

## Endpoints (via Gateway, prefixo /api/atendimento)
- `GET /categorias` - Lista de categorias ativas (para o formulário de abertura de chamado)
- `GET /fila?departamentoId=&status=` - Gestão de fila de atendimento (ADMIN)
- `POST /fila/{chamadoId}/assumir` - Atribuição concorrente-segura a atendente (ADMIN com perfil de atendente)
- `GET|POST /departamentos`, `PUT /departamentos/{id}` - Gestão de departamentos (ADMIN)
- `GET|POST /atendentes`, `PUT /atendentes/{id}` - Gestão de atendentes (ADMIN)
- `GET /slas`, `PUT /slas/{prioridade}` - Gestão de prazos de SLA (ADMIN)

## Como rodar
1. `docker compose up -d mongo` (compose no repositório GATEWAY)
2. Copie `.env.example` para `.env`
3. `mvn -pl spring spring-boot:run`
