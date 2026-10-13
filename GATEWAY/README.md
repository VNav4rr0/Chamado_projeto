# Gateway

Entrada unica do AgendaFlow. Valida o JWT (header Authorization ou cookie), aplica as regras de role e repassa a identidade aos servicos.

## Rotas

| Caminho | Servico | Porta |
|---|---|---|
| `/api/auth/**` | Login | 8090 |
| `/api/servicos/*/horarios`, `/api/calendario/**` | Calendario | 8092 |
| `/api/servicos/**`, `/api/agendamentos/**` | Agendamento | 8091 |
| `/api/relatorio/**` | Relatorio | 8093 |

A rota de horarios vem antes da de servicos de proposito. `/interno/**` nao tem rota.

## Acesso

- Liberado: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/logout` e `OPTIONS`.
- Somente ADMIN: `/api/relatorio/**`, `/api/calendario/**` e escrita em `/api/servicos/**`.
- Demais rotas: qualquer usuario autenticado.

## Cookie

O Login cria o cookie `tokenAgendaFlow` (HttpOnly, SameSite=Lax). O `CookieBearerTokenConverter` procura primeiro o header `Authorization: Bearer` e, na falta dele, o cookie. Em producao com HTTPS o cookie deve passar a ser `Secure`.

## Identidade para os servicos

O `UsuarioHeadersFilter` apaga `X-Usuario-Id` e `X-Usuario-Role` vindos do cliente e, com JWT valido, grava os valores das claims `uid` e `roles`.

## CSRF

Desabilitado porque o cookie e SameSite=Lax, as chamadas sao JSON e o CORS so aceita as origens listadas.

## Como rodar

1. `docker compose up -d mongo`
2. Defina `JWT_SECRET` (igual ao do Login). Sem ele o Gateway nao sobe.
3. `./mvnw spring-boot:run`
