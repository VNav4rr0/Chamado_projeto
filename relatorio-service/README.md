# relatorio-service

Microsserviço de relatórios mensais e consolidados do Sistema de Chamados de Suporte de TI.

## Porta
- `8093`

## Características
- Sem banco de dados próprio e sem RabbitMQ (somente leitura HTTP).
- Consulta chamados concluídos através do Gateway (`GATEWAY_URL`), repassando o cookie do solicitante.
- Cálculos analíticos isolados no `domain` (Clean Architecture).

## Endpoint
- `GET /api/relatorio/mensal?ano=&mes=` (Acesso restrito a ADMIN)
