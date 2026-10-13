# chamado-service

Microsserviço de gestão de chamados (tickets), máquina de estados e histórico do Sistema de Chamados de Suporte de TI.

## Porta
- `8091`

## Tecnologias
- Java 25 LTS
- Spring Boot 4.0.6
- MongoDB 7 (Replica Set `chamadoDb`)
- Clean Architecture (módulos `domain` e `spring`)

## Endpoints
- `POST /api/chamados` - Abertura de chamado (USUARIO ou ADMIN)
- `GET /api/chamados` - Listagem pagina com filtros (USUARIO vê apenas seus chamados; ADMIN vê todos)
- `GET /api/chamados/{id}` - Detalhes do chamado com histórico (Dono ou ADMIN)
- `PATCH /api/chamados/{id}/status` - Alteração de status conforme máquina de estados e permissões

## Execução Local

Use a JDK 25 to build and run this service.

```bash
# Compilar e rodar testes
mvn clean package

# Executar aplicação Spring Boot
mvn -pl spring spring-boot:run
```
