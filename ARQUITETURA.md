# Sistema de Chamados de Suporte de TI — Arquitetura e Plano de Execução

> Documento para ser entregue a uma IA executora (cowork). Tudo o que está aqui é decisão tomada; o que depende de confirmação do aluno está na seção 14.
> Base: código real dos repositórios `ViniciusVChabariberi/LOGIN` (completo) e `ViniciusVChabariberi/GATEWAY`, mais o README do front-end React Native (não recebi o código do front).

---

## 0. Como a IA executora deve usar este documento

1. Leia tudo antes de criar qualquer arquivo.
2. Siga a **ordem de execução da seção 11** (Login → Gateway → atendimento → chamado → relatório → testes → scripts de demonstração).
3. **Um repositório por serviço. Sem monorepo. Sem módulo `commons`**: contratos de mensagem e classes utilitárias pequenas são duplicados de propósito (seção 3.4).
4. Cada serviço segue o **mesmo molde de Clean Architecture do Login** (módulos Maven `domain` e `spring`, seção 3).
5. Respeite as **convenções da seção 2** (camelCase etc.) e a **checklist de correções do professor** (seção 2.2). Cada arquivo novo deve ter um motivo de existir; se uma responsabilidade já cabe numa classe existente, ela vai lá.
6. Ao terminar cada fase, rode os critérios de aceite da seção 11.

---

## 1. Visão geral

### 1.1 Serviços, repositórios e portas

| Repositório | Papel | Porta | Banco | Mensageria |
|---|---|---|---|---|
| `gateway` (existente) | Entrada única, valida JWT/cookie, aplica roles, injeta identidade | 8080 | — | não |
| `login` (existente) | Cadastro, login, emissão do JWT e do cookie | 8090 | Mongo `loginDb` | não |
| `chamado-service` (novo) | Chamados (tickets), máquina de estados, histórico | 8091 | Mongo `chamadoDb` | publica e consome |
| `atendimento-service` (novo) | Departamentos, SLA, atendentes, **filas** e atribuição | 8092 | Mongo `atendimentoDb` | publica e consome |
| `relatorio-service` (novo) | Consolidado mensal (somente leitura) | 8093 | nenhum | **não** (HTTP) |
| `frontend` (existente) | React Native (Expo) | 8081 | — | — |

As portas 8090–8093 já são as usadas no `application.yaml` do Gateway.

### 1.2 Infraestrutura (localhost)

- **MongoDB 7 como replica set de 1 nó** (`rs0`). Já existe no `docker-compose.yaml` do Gateway e é **pré-requisito das transações** (Mongo só faz transação multi-documento em replica set).
- **RabbitMQ no CloudAMQP** (URL `amqps://...` em variável de ambiente). O compose do Gateway já tem um `rabbitmq` opcional (profile `rabbitLocal`) para desenvolver sem internet.
- Java 25 e Spring Boot 4.0.6 no Login e no chamado-service; os demais serviços novos permanecem em Java 17. O Gateway fica em Spring Boot 3.5.13 / Spring Cloud 2025.0.0 (não migrar).

### 1.3 Diagrama

```
 Front RN (8081) ──HTTP + cookie──▶ GATEWAY (8080) ──┬──▶ LOGIN (8090) ──▶ Mongo loginDb
                                     valida JWT,     │
                                     aplica roles,   ├──▶ CHAMADO (8091) ──▶ Mongo chamadoDb (+ outbox)
                                     injeta X-Usuario-*│        ▲   │
                                                     │        │   │ chamado.criado / resolvido / rejeitado
                                                     │  chamado.triado / atribuido │
                                                     │        │   ▼
                                                     ├──▶ ATENDIMENTO (8092) ──▶ Mongo atendimentoDb (+ outbox)
                                                     │
                                                     └──▶ RELATORIO (8093) ──HTTP (via gateway, repassa cookie)──▶ CHAMADO
                       CHAMADO ◀══ RabbitMQ (CloudAMQP) ══▶ ATENDIMENTO     (único pub/sub do sistema)
```

---

## 2. Convenções e correções do professor

### 2.1 Convenções (valem para os 5 repositórios)

| Item | Regra | Exemplo |
|---|---|---|
| Classes, records, enums, interfaces | PascalCase | `ChamadoService`, `StatusChamado` |
| Métodos, atributos, variáveis, parâmetros | camelCase | `alterarStatus`, `prazoSla` |
| Pacotes Java | minúsculo, sem separador | `com.suporteti.chamado.repository.orm` |
| Constantes de enum | MAIÚSCULAS_COM_UNDERSCORE (convenção Java) | `EM_ANDAMENTO` |
| Campos JSON e campos Mongo | camelCase | `solicitanteId`, `criadoEm` |
| Coleções Mongo | camelCase | `chamados`, `mensagensOutbox`, `mensagensProcessadas`, `filaAtendimento` |
| Ids de rota do Gateway, volumes Docker, cookie | camelCase | `chamadoApi`, `mongoData`, `tokenSuporteTi` |
| Exchanges e queues RabbitMQ | camelCase | `chamadosExchange`, `atendimentoEventosChamadoQueue` |
| Routing keys | duas palavras minúsculas separadas por ponto (o ponto é a sintaxe do topic exchange; cada segmento é uma palavra só, então não há camelCase em jogo) | `chamado.criado` |
| Variáveis de ambiente | MAIÚSCULAS_COM_UNDERSCORE (padrão do `.env` já usado no Login e no Gateway) | `JWT_SECRET` |
| Nomes de módulos Maven | `domain`, `spring` (como no Login) | |

(O Gateway original já usa `loginApi`, `mongoData` e `tokenAgendaFlow` — é o padrão que o professor cobrou. Estender o mesmo estilo.)

### 2.2 Checklist de correções (feedback recebido: CamelCase, re-criação de arquivos, funcionalidades que poderiam ser uma só)

Não recebi os projetos `produtos` e `cliente`. A lista abaixo vem do que aparece no Login/Gateway (mesmo estilo de código) e dos pontos que você citou. **Confira contra o feedback escrito do professor.**

| # | Ponto | Onde aparece hoje | Regra para os serviços novos |
|---|---|---|---|
| 1 | CamelCase | Padrão já parcialmente seguido | Seção 2.1, sem exceções não documentadas |
| 2 | Leitura dos headers `X-Usuario-*` repetida em cada método | `UsuarioController` do Login repete `@RequestHeader` ×6 e a função `ehAdmin` | **Um** `UsuarioLogadoResolver` (argument resolver) que entrega um `UsuarioLogado` pronto; controllers só declaram o parâmetro |
| 3 | Vários serviços/use cases por verbo (`CriarX`, `AtualizarX`...) | O Login usa um `AuthService` único (bom) | **Um service por agregado**; nunca um arquivo por verbo |
| 4 | Endpoints por ação (`/resolver`, `/rejeitar`, `/meus`) | — | Um endpoint com parâmetro: `PATCH /chamados/{id}/status`; `GET /chamados` já filtra por dono conforme a role |
| 5 | Mapeadores espalhados | Login tem 2 adapters estáticos (ok) | **Um adapter por camada por serviço** (controller e repository), com métodos estáticos para todos os agregados |
| 6 | Exceções redundantes | Login tem 4 | Máximo 3 por serviço: `AcessoNegadoException`(403), `RecursoNaoEncontradoException`(404), `RegraDeNegocioException`(409). Estado inválido, perda de concorrência e capacidade esgotada são `RegraDeNegocioException` |
| 7 | Código morto e imports sobrando | `Main.java` de demonstração no skeleton do Login; `import Token`/`Role` não usados no `AuthController`; `StandardCharsets` importado 2× no `JwtTokenEmissor` | Compilar com `-Xlint` e remover; nenhum `Main` de exemplo |
| 8 | Configuração duplicada | `application.yaml` e `.env` | Um `.env.example` por serviço, único lugar de segredos |
| 9 | Regras de negócio em controller | — | Controller só converte DTO ↔ domínio; regra fica no `service` do domínio |

---

## 3. Molde de Clean Architecture (igual ao Login)

### 3.1 Regra de dependência

```
spring (controllers, repositories mongo, rabbit, config)  ──depende de──▶  domain
domain (model, service, port, repository(interface), exception, event)  ──não depende de nada──
```

- O módulo `domain` é **Java puro** (só JUnit em teste). **Nenhuma anotação Spring** no domain, como no `AuthService` do Login.
- Os beans do domain são montados à mão em `config/ServiceConfig.java` (como o Login faz com `AuthService`).
- Tudo que o domain precisa do mundo externo entra por **interface**: `repository/*` (persistência) e `port/*` (transação, mensageria, cliente HTTP, geradores).
- Fluxo de dados: `Request DTO → (adapter) → domain → (adapter) → Response DTO`. O ORM Mongo (`*OrmMongo`, record com `@Document`) **nunca** sobe para o domain; o `Impl` do repository converte com o adapter.

### 3.2 Estrutura-padrão de um serviço

```
<servico>/
├── pom.xml                      # pom pai, packaging pom, módulos domain e spring
├── .env.example
├── .gitignore
├── Dockerfile
├── README.md
├── domain/
│   ├── pom.xml                  # só junit-jupiter + surefire (test)
│   └── src/{main,test}/java/com/suporteti/<servico>/
│       ├── model/               # records imutáveis + model/enumerable/
│       ├── event/               # eventos de saída (quando o serviço publica)
│       ├── port/                # interfaces de capacidades externas
│       ├── repository/          # interfaces de persistência
│       ├── service/             # regras de negócio (um por agregado)
│       └── exception/           # 3 exceções
└── spring/
    ├── pom.xml                  # depende de domain
    └── src/{main,test}/
        ├── java/com/suporteti/<servico>/
        │   ├── <Servico>Application.java
        │   ├── config/          # ServiceConfig, Mongo, Rabbit, Web
        │   ├── controller/      # controllers + dto/{request,response} + adapter/ + resolver/
        │   ├── repository/      # *Impl + adapter/ + mongo/ + orm/
        │   ├── messaging/       # publicador (outbox), relay, consumer, idempotência
        │   ├── persistence/     # TransacaoExecutorMongo, geradores
        │   └── exception/       # GlobalExceptionHandler
        └── resources/application.yaml
```

Pacote raiz: `com.suporteti.chamado`, `com.suporteti.atendimento`, `com.suporteti.relatorio` (o Login e o Gateway mantêm `com.vinijoao...`).

### 3.3 Dependências dos `spring/pom.xml` (novos serviços)

Mesmas do Login (`web`, `data-mongodb`, `validation`) + `spring-boot-starter-amqp` (chamado e atendimento) + `domain`. Relatório: sem Mongo e sem AMQP, com cliente HTTP (`RestClient`). Sem Spring Security nos serviços (como o Login: quem valida o token é o Gateway).

### 3.4 Duplicação consciente (consequência de "sem commons")

Cada serviço tem a sua cópia de: o trio de exceções, o `GlobalExceptionHandler`, o `UsuarioLogadoResolver`, o `TransacaoExecutorMongo`, o outbox e a idempotência (só chamado e atendimento) e os records de evento. O contrato entre serviços é o **JSON** (seção 5.3), não uma classe compartilhada. Justificativa para a defesa: acoplamento por binário (jar comum) obrigaria a publicar/versionar em conjunto; acoplamento por contrato JSON permite evoluir cada serviço sozinho.

---

## 4. Transações

### 4.1 O que é transação aqui

Mongo com replica set permite transação multi-documento (ACID dentro de um nó primário). Cada caso de uso que muda mais de um documento (ou muda dado + grava evento) roda em **uma** transação.

### 4.2 Port `TransacaoExecutor` (domain) e implementação (spring)

```java
// domain/port/TransacaoExecutor.java
public interface TransacaoExecutor {
    <T> T executar(Supplier<T> trabalho);
    void executar(Runnable trabalho);   // sobrecarga sem retorno
}
```

- Implementação: `spring/persistence/TransacaoExecutorMongo.java`, usando `TransactionTemplate` com `MongoTransactionManager` (bean declarado em `config/MongoConfig.java`; o Spring Data **não** liga transação Mongo sozinho).
- **Retry limitado (3 tentativas)** quando o erro tem o label `TransientTransactionError` (conflito de escrita entre transações concorrentes). Desembrulhar a causa (`MongoException`) porque o Spring encapsula em `UncategorizedMongoDbException`.
- Regra: **nunca chamar RabbitMQ, HTTP ou qualquer I/O externo dentro da transação.**

### 4.3 Padrão Outbox (resolve "gravar no banco E publicar mensagem")

Problema: se o serviço grava o chamado e depois publica no RabbitMQ, uma falha entre os dois deixa banco e fila inconsistentes (dual write).

Solução:

1. Dentro da **mesma transação** que muda o dado, `PublicadorEventosOutbox` (implementa `port/PublicadorEventos`) insere um documento em `mensagensOutbox` (`id` = messageId UUID, `chaveRoteamento`, `payload` JSON, `status` PENDENTE, `criadoEm`).
2. `OutboxRelay` (`@Scheduled`, a cada 1 s) lê PENDENTE em ordem de `criadoEm`, publica no RabbitMQ com **publisher confirm**, e só então marca ENVIADA. Se falhar, tenta de novo na próxima rodada.
3. Entrega é **at-least-once** → consumidores precisam ser idempotentes (4.4).

### 4.4 Idempotência no consumidor

Cada mensagem leva `messageId`. O consumidor, **dentro da transação do caso de uso**, insere o `messageId` em `mensagensProcessadas` (índice único). Se já existir (`DuplicateKeyException`), a mensagem é duplicada: a transação é revertida e a mensagem é confirmada (ack) sem reprocessar. Efeito: processamento "exatamente uma vez" do ponto de vista do negócio.

### 4.5 Onde há transação (resumo)

| Serviço | Caso de uso | Documentos alterados na mesma transação |
|---|---|---|
| chamado | abrir chamado | `chamados` (insert) + `mensagensOutbox` (`chamado.criado`) |
| chamado | alterar status (resolver/rejeitar) | `chamados` (status + histórico + horas/custo) + `mensagensOutbox` |
| chamado | aplicar triagem / atribuição (vindos do Rabbit) | `mensagensProcessadas` + `chamados` |
| atendimento | receber `chamado.criado` | `mensagensProcessadas` + `filaAtendimento` (insert) + `mensagensOutbox` (`chamado.triado`) |
| atendimento | **assumir chamado** | `atendentes` (reserva de capacidade) + `filaAtendimento` (reivindicação) + `mensagensOutbox` (`chamado.atribuido`) — **tudo ou nada** |
| atendimento | receber `chamado.resolvido`/`rejeitado` | `mensagensProcessadas` + `filaAtendimento` + `atendentes` (libera capacidade) |

---

## 5. Mensageria (RabbitMQ / CloudAMQP)

### 5.1 Regra do enunciado

Pub/sub **somente entre Chamado e Atendimento**. Login, Gateway e Relatório não publicam nem consomem.

### 5.2 Topologia

Dois **topic exchanges** duráveis, um por publicador (cada serviço é dono do que publica):

| Exchange | Publicador | Routing keys |
|---|---|---|
| `chamadosExchange` | chamado-service | `chamado.criado`, `chamado.resolvido`, `chamado.rejeitado` |
| `atendimentoExchange` | atendimento-service | `chamado.triado`, `chamado.atribuido` |

**Uma queue por serviço consumidor** (garante ordem entre eventos do mesmo chamado: `criado` sempre antes de `resolvido`; `triado` antes de `atribuido`; consumidor com `concurrency=1`):

| Queue (durável) | Consumidor | Ligada a | DLQ |
|---|---|---|---|
| `atendimentoEventosChamadoQueue` | atendimento-service | `chamadosExchange` com `chamado.criado`, `chamado.resolvido`, `chamado.rejeitado` | `atendimentoEventosChamadoDlq` |
| `chamadoEventosAtendimentoQueue` | chamado-service | `atendimentoExchange` com `chamado.triado`, `chamado.atribuido` | `chamadoEventosAtendimentoDlq` |

- Cada serviço **declara os dois exchanges** (declaração é idempotente), assim a ordem de subida não importa. Cada um declara só a sua queue, a sua DLQ e as bindings.
- Mensagens persistentes (`deliveryMode=2`), publisher confirms ligado (`spring.rabbitmq.publisher-confirm-type=correlated`).
- Falha no consumidor: 3 tentativas com backoff (`spring.rabbitmq.listener.simple.retry`), `default-requeue-rejected=false`; esgotadas, a mensagem vai para a DLQ (via `x-dead-letter-exchange`/`x-dead-letter-routing-key` na queue). A DLQ é mostrada na demonstração.
- Conexão: `spring.rabbitmq.uri=${CLOUDAMQP_URL}` (URL `amqps://usuario:senha@host/vhost`, TLS). **Atenção:** confirmar a classe do conversor JSON do Spring AMQP compatível com o Boot 4.0.6 (usa Jackson 3); alternativa segura: serializar o payload como `String` JSON no publicador e parsear no consumidor.
- O plano gratuito do CloudAMQP tem limites baixos de conexões/mensagens. Cada serviço abre **1 conexão**; não subir réplicas.

### 5.3 Contrato dos eventos (JSON, campos camelCase)

Metadados em headers AMQP: `messageId` (UUID), `timestamp`, `contentType=application/json`. Datas em ISO-8601 UTC.

**`chamado.criado`** (Chamado → Atendimento)
```json
{ "chamadoId": "…", "protocolo": "CH-2026-000042", "categoria": "REDE",
  "prioridade": "ALTA", "titulo": "…", "solicitanteId": "…", "criadoEm": "2026-10-11T23:45:00Z" }
```

**`chamado.triado`** (Atendimento → Chamado)
```json
{ "chamadoId": "…", "resultado": "ACEITO",              // ou "REJEITADO"
  "departamentoId": "…", "departamentoNome": "Redes",
  "prazoSla": "2026-10-12T07:45:00Z",
  "motivo": null }                                       // preenchido quando REJEITADO
```

**`chamado.atribuido`** (Atendimento → Chamado)
```json
{ "chamadoId": "…", "atendenteId": "…", "atendenteNome": "…", "atribuidoEm": "…" }
```

**`chamado.resolvido`** (Chamado → Atendimento): `{ "chamadoId": "…", "atendenteId": "…", "resolvidoEm": "…" }`

**`chamado.rejeitado`** (Chamado → Atendimento): `{ "chamadoId": "…", "atendenteId": "…", "motivo": "…" }` (rejeição feita pelo técnico depois de assumir; libera a capacidade do atendente)

`atendenteId` aqui é o **id do usuário no Login** (`uid` do JWT), usado como identificador do atendente nos dois serviços.

---

## 6. Domínio e máquina de estados

### 6.1 Status do chamado

`CRIADO, EM_ANALISE, EM_ANDAMENTO, AGUARDANDO_TERCEIRO, RESOLVIDO, FECHADO, REJEITADO`

| De | Para | Quem dispara | Como |
|---|---|---|---|
| (abertura) | `CRIADO` | Usuário | `POST /api/chamados` |
| `CRIADO` | `EM_ANALISE` | Atendimento | evento `chamado.triado` com `ACEITO` (categoria e prioridade válidas, chamado na fila certa) |
| `CRIADO` | `REJEITADO` | Atendimento | evento `chamado.triado` com `REJEITADO` (categoria/prioridade inválida) |
| `EM_ANALISE` | `EM_ANDAMENTO` | Atendimento | evento `chamado.atribuido` (técnico assumiu) |
| `EM_ANDAMENTO` | `AGUARDANDO_TERCEIRO` | Técnico atribuído | `PATCH /status` |
| `AGUARDANDO_TERCEIRO` | `EM_ANDAMENTO` ou `RESOLVIDO` | Técnico atribuído | `PATCH /status` |
| `EM_ANDAMENTO` | `RESOLVIDO` | Técnico atribuído | `PATCH /status` com `horasTrabalhadas` e `custo` obrigatórios |
| `EM_ANDAMENTO` | `REJEITADO` | Técnico atribuído | `PATCH /status` com `motivo` obrigatório |
| `RESOLVIDO` | `FECHADO` | Solicitante (dono) ou ADMIN | `PATCH /status` |
| `FECHADO`, `REJEITADO` | — | terminais | — |

- A tabela de transições vive no próprio enum `StatusChamado` (`podeIrPara(StatusChamado)`); o `ChamadoService` aplica e quem pode disparar cada uma é regra do service. Transição inválida → `RegraDeNegocioException` (409).
- `Chamado` é um record imutável; `chamado.comStatus(novo, usuario, instante, observacao)` devolve um novo registro com o histórico acrescido (`MudancaStatus`).
- O front mostra os status em português; o backend usa as constantes.
- Os status `EM_ANDAMENTO` e `EM_ANALISE` chegam por evento: a atualização é **eventualmente consistente** (milissegundos). O front deve consultar de novo (polling ou botão atualizar).

### 6.2 Item da fila (atendimento)

`StatusItemFila`: `EM_ANALISE` (recebido, ainda em triagem) → `NA_FILA` → `ATRIBUIDO` → `CONCLUIDO`; ou `EM_ANALISE` → `REJEITADO`. É um único agregado (`ItemFila`) que representa triagem e fila.

### 6.3 Roles

Dois roles, como o enunciado: `USUARIO` (solicitante) e `ADMIN` (admin/técnico). Todo ADMIN pode ter perfil de atendente.

### 6.4 SLA e prioridades

`Prioridade` (enum no chamado-service): `BAIXA, MEDIA, ALTA, CRITICA`. O atendimento guarda o SLA em horas por prioridade (seed: BAIXA 72, MEDIA 24, ALTA 8, CRITICA 2) e calcula `prazoSla = criadoEm + horas` na triagem. O prazo volta no evento `chamado.triado` e fica no chamado.

### 6.5 Categorias

O **atendimento é a fonte da verdade**: cada departamento tem uma lista de categorias (seed: Infraestrutura, Redes, Software, AcessoContas, Hardware). O chamado só exige `categoria` não vazia; a validação de existência acontece na triagem (categoria inexistente → `REJEITADO` com motivo). O front obtém a lista em `GET /api/atendimento/categorias`.

---

## 7. Serviços

### 7.1 LOGIN — alterações (repositório `login`, base: `ViniciusVChabariberi/LOGIN`)

> **Atenção:** o fork `VNav4rr0/LOGIN` hoje é só um esqueleto (um `Login` record, `Token`, `Role{ADMIN,USER}`, um `LoginRepository` e dois `Main` de exemplo). O Login completo (AuthService, JWT, cookie, Mongo, BCrypt) está em `ViniciusVChabariberi/LOGIN`. Este plano parte do **completo**. Se a sua cópia local é outra, avise.

| Arquivo | Ação | O que muda e por quê |
|---|---|---|
| `domain/.../model/enumerable/Role.java` | **Modificar** | `ADMIN, COMUM` → `ADMIN, USUARIO` (nome do enunciado) |
| `domain/.../service/AuthService.java` | **Modificar** | `Role.COMUM` → `Role.USUARIO` no cadastro padrão |
| `spring/.../controller/dto/request/CadastroRequest.java` | **Modificar** | `@Pattern(regexp = "(?i)ADMIN\|USUARIO")` |
| `domain/src/test/.../AuthServiceTest.java` | **Modificar** | Ajustar `COMUM` → `USUARIO` |
| `spring/.../controller/dto/response/AuthResponse.java` | **Excluir** | O token **não deve** ir no corpo: o login é por cookie HttpOnly e devolver o JWT no JSON convida o front a guardá-lo em `localStorage` (anula o HttpOnly). `register` e `login` passam a devolver `UsuarioResponse` |
| `spring/.../controller/adapter/AuthControllerAdapter.java` | **Modificar** | Remover `paraAuthResponse`; manter `paraRole` e `paraResposta` |
| `spring/.../controller/AuthController.java` | **Modificar** | Retornar `UsuarioResponse`; remover imports mortos (`Token`) |
| `spring/.../controller/UsuarioController.java` | **Modificar** | Trocar os 6 `@RequestHeader` + `ehAdmin` por um parâmetro `UsuarioLogado` (ponto 2 da checklist) |
| `spring/.../controller/resolver/UsuarioLogadoResolver.java` | **Criar** | `HandlerMethodArgumentResolver` que lê `X-Usuario-Id`, `X-Usuario-Role`, `X-Usuario-Nome` (decodificado de URL-encoding UTF-8) |
| `spring/.../config/WebConfig.java` | **Criar** | Registra o resolver (`WebMvcConfigurer`) |
| `domain/.../model/UsuarioLogado.java` | **Criar** | `record UsuarioLogado(String id, String nome, boolean admin)` — substitui os parâmetros soltos `solicitanteId, solicitanteAdmin` nos métodos do `AuthService` |
| `spring/.../security/JwtTokenEmissor.java` | **Modificar** | Remover import duplicado de `StandardCharsets` |
| `.env.example` | **Modificar** | `COOKIE_NOME=tokenSuporteTi`; `ADMIN_EMAIL=admin@suporteti.com` |
| `README.md` | **Modificar** | Trocar "AgendaFlow" por "Suporte TI"; remover a menção ao token no corpo; documentar o cookie |
| `spring/.../resources/application.yaml` | Manter | Porta 8090, `login-service`, Mongo `loginDb` (trocar `login_db` por `loginDb` por causa do camelCase) |

Não mudam: `AuthService` (lógica), BCrypt, `JwtTokenEmissor` (claims `uid`, `nome`, `roles:["ROLE_ADMIN"]`), `TokenCookieFactory` (HttpOnly, `Path=/`, `SameSite=Lax`), repositório Mongo, `DadosIniciais` (admin inicial).
Apagar o banco `login_db` antigo (os usuários guardam `COMUM`).

### 7.2 GATEWAY — alterações (repositório `gateway`)

| Arquivo | Ação | O que muda e por quê |
|---|---|---|
| `src/main/resources/application.yaml` | **Modificar** | Rotas novas (abaixo); `cookie.nome` default `tokenSuporteTi`; incluir origem do front se for outra |
| `config/SecurityConfig.java` | **Modificar** | Regras de acesso novas (abaixo) |
| `filter/UsuarioHeadersFilter.java` | **Modificar** | Passar também `X-Usuario-Nome` (valor `URLEncoder.encode(nome, UTF_8)`, porque header HTTP não aceita acento) e remover o header recebido do cliente, como já faz com os outros |
| `config/JwtDecoderConfig.java` | **Modificar** | `secretKey.getBytes(StandardCharsets.UTF_8)` (o Login usa UTF-8; sem charset explícito o segredo pode divergir) |
| `src/test/.../SecurityConfigTest.java` | **Modificar** | Novos casos (abaixo); trocar `@MockBean` (depreciado no Boot 3.5) por `@MockitoBean` |
| `.env.example`, `README.md` | **Modificar** | Nomes, portas, cookie, tabela de rotas |
| `docker-compose.yaml` | **Modificar** | Volume `mongoData` já está em camelCase (manter); manter `rabbitmq` com profile `rabbitLocal`; ver seção 9 |
| `config/CorsConfig.java`, `security/CookieBearerTokenConverter.java` | Manter | Sem mudança |

**Rotas** (`spring.cloud.gateway.server.webflux.routes`, ids camelCase):

```yaml
- id: loginApi
  uri: lb://login-service
  predicates: [ "Path=/api/auth/**, /api/usuarios/**" ]   # /api/usuarios/** NÃO existe hoje: o UsuarioController do Login fica inalcançável
- id: chamadoApi
  uri: lb://chamado-service
  predicates: [ "Path=/api/chamados/**" ]
- id: atendimentoApi
  uri: lb://atendimento-service
  predicates: [ "Path=/api/atendimento/**" ]
- id: relatorioApi
  uri: lb://relatorio-service
  predicates: [ "Path=/api/relatorio/**" ]
```

Instâncias (`spring.cloud.discovery.client.simple.instances`): `login-service` 8090, `chamado-service` 8091, `atendimento-service` 8092, `relatorio-service` 8093, com variáveis `LOGIN_URL`, `CHAMADO_URL`, `ATENDIMENTO_URL`, `RELATORIO_URL`. Remover as rotas/instâncias de agendamento e calendário.

**Regras de acesso** (ordem importa; a primeira que casa vale):

| Padrão | Regra |
|---|---|
| `OPTIONS /**` | liberado |
| `POST /api/auth/register`, `/login`, `/logout` | liberado |
| `/api/relatorio/**` | `ROLE_ADMIN` |
| `GET /api/atendimento/categorias` | autenticado (o usuário precisa da lista para abrir o chamado) |
| `/api/atendimento/**` | `ROLE_ADMIN` (gestão de filas) |
| `/api/chamados/**` | autenticado (dono/ADMIN é decidido no service) |
| `/api/auth/me`, `/api/usuarios/**` | autenticado (dono/ADMIN no service do Login) |
| qualquer outra | autenticado |

Testes do Gateway (`WebTestClient`, JWT simulado): sem token → 401 em `/api/chamados`; USUARIO em `/api/relatorio/mensal` → 403; USUARIO em `/api/atendimento/fila` → 403; USUARIO em `GET /api/atendimento/categorias` → passa do Gateway; ADMIN em `/api/relatorio/mensal` → passa do Gateway; header `X-Usuario-Role` forjado pelo cliente é descartado.

Não há rota para `/interno/**` (continua assim: nenhum endpoint interno é exposto).

### 7.3 `chamado-service` (porta 8091)

#### Árvore completa

```
chamado-service/
├── pom.xml  .env.example  .gitignore  Dockerfile  README.md
├── domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/suporteti/chamado/
│       │   ├── model/
│       │   │   ├── Chamado.java
│       │   │   ├── MudancaStatus.java
│       │   │   ├── UsuarioLogado.java
│       │   │   ├── FiltroChamados.java
│       │   │   ├── Pagina.java
│       │   │   ├── ResultadoTriagem.java
│       │   │   ├── Atribuicao.java
│       │   │   └── enumerable/{StatusChamado.java, Prioridade.java}
│       │   ├── event/EventoSaida.java
│       │   ├── port/{PublicadorEventos.java, TransacaoExecutor.java, GeradorProtocolo.java}
│       │   ├── repository/ChamadoRepository.java
│       │   ├── service/ChamadoService.java
│       │   └── exception/{AcessoNegadoException, RecursoNaoEncontradoException, RegraDeNegocioException}.java
│       └── test/java/com/suporteti/chamado/service/{ChamadoServiceTest.java, StatusChamadoTest.java}
└── spring/
    ├── pom.xml
    └── src/
        ├── main/java/com/suporteti/chamado/
        │   ├── ChamadoApplication.java            # @SpringBootApplication + @EnableScheduling
        │   ├── config/{ServiceConfig.java, MongoConfig.java, RabbitConfig.java, WebConfig.java}
        │   ├── controller/
        │   │   ├── ChamadoController.java
        │   │   ├── adapter/ChamadoControllerAdapter.java
        │   │   ├── resolver/UsuarioLogadoResolver.java
        │   │   └── dto/{request/{AbrirChamadoRequest, AlterarStatusRequest}.java,
        │   │            response/{ChamadoResponse, PaginaResponse}.java}
        │   ├── repository/
        │   │   ├── ChamadoRepositoryImpl.java
        │   │   ├── adapter/ChamadoRepositoryAdapter.java
        │   │   ├── mongo/ChamadoMongoRepository.java
        │   │   └── orm/ChamadoOrmMongo.java
        │   ├── messaging/
        │   │   ├── PublicadorEventosOutbox.java
        │   │   ├── OutboxRelay.java
        │   │   ├── MensagemOutboxOrm.java
        │   │   ├── MensagemProcessadaOrm.java
        │   │   ├── RegistroMensagensProcessadas.java
        │   │   └── ConsumidorEventosAtendimento.java
        │   ├── persistence/{TransacaoExecutorMongo.java, GeradorProtocoloMongo.java}
        │   └── exception/GlobalExceptionHandler.java
        └── main/resources/application.yaml
```

#### Arquivos e responsabilidades

| Arquivo | Camada | Responsabilidade |
|---|---|---|
| `Chamado` | domain/model | Record: `id, protocolo, titulo, descricao, categoria, prioridade, status, solicitanteId, solicitanteNome, departamentoId, departamentoNome, atendenteId, atendenteNome, prazoSla, criadoEm, analisadoEm, atribuidoEm, resolvidoEm, fechadoEm, horasTrabalhadas (BigDecimal), custo (BigDecimal), motivo, historico (List<MudancaStatus>), versao`. Métodos `comStatus(...)`, `comTriagem(...)`, `comAtribuicao(...)` devolvem novo registro |
| `MudancaStatus` | domain/model | Record: `de, para, porUsuarioId, em, observacao` — trilha de auditoria exibida no detalhe |
| `UsuarioLogado` | domain/model | `record(String id, String nome, boolean admin)` |
| `FiltroChamados` | domain/model | `solicitanteId (nullable), status (lista), atendenteId, resolvidoDe, resolvidoAte, pagina, tamanho` |
| `Pagina<T>` | domain/model | `itens, total, pagina, tamanho` |
| `ResultadoTriagem`, `Atribuicao` | domain/model | Entradas dos casos de uso disparados por evento (campos do contrato 5.3) |
| `StatusChamado` | domain/enumerable | Constantes + `podeIrPara(...)` (tabela da seção 6.1) |
| `Prioridade` | domain/enumerable | `BAIXA, MEDIA, ALTA, CRITICA` |
| `EventoSaida` | domain/event | Interface selada com os records aninhados `ChamadoCriado`, `ChamadoResolvido`, `ChamadoRejeitado`; cada um expõe `chaveRoteamento()` |
| `PublicadorEventos` | domain/port | `void publicar(EventoSaida evento)` (a implementação grava no outbox) |
| `TransacaoExecutor` | domain/port | Seção 4.2 |
| `GeradorProtocolo` | domain/port | `String proximo(Instant agora)` → `CH-2026-000001` |
| `ChamadoRepository` | domain/repository | `save`, `findById`, `buscar(FiltroChamados)` |
| `ChamadoService` | domain/service | `abrir(usuario, titulo, descricao, categoria, prioridade)`; `listar(usuario, filtro)` (USUARIO: força `solicitanteId = usuario.id()`; ADMIN: sem restrição); `consultar(usuario, id)` (dono ou ADMIN); `alterarStatus(usuario, id, novoStatus, horas, custo, motivo)` (valida a transição **e quem pode**: atribuído para andamento/aguardando/resolvido/rejeitado; dono ou ADMIN para `FECHADO`); `aplicarTriagem(ResultadoTriagem)`; `aplicarAtribuicao(Atribuicao)`. Todo caso de uso que grava roda em `transacao.executar(...)` |
| Exceções ×3 | domain | Seção 2.2, ponto 6 |
| `ServiceConfig` | spring/config | Cria `ChamadoService` (e `Clock` em `America/Sao_Paulo` para metas de data) |
| `MongoConfig` | spring/config | `MongoTransactionManager`; índices (`chamados`: `solicitanteId`, `status`, `resolvidoEm`; `mensagensProcessadas`: `messageId` único) |
| `RabbitConfig` | spring/config | Exchanges, queue + DLQ + bindings, conversor JSON, `RabbitTemplate` com confirms |
| `WebConfig` | spring/config | Registra o `UsuarioLogadoResolver` |
| `ChamadoController` | spring/controller | Endpoints da tabela abaixo; só converte DTO e chama o service |
| `ChamadoControllerAdapter` | spring/controller/adapter | Request → parâmetros do domain; `Chamado` → `ChamadoResponse`; `Pagina` → `PaginaResponse` |
| `UsuarioLogadoResolver` | spring/controller/resolver | Monta `UsuarioLogado` a partir dos headers do Gateway |
| DTOs | spring/controller/dto | `AbrirChamadoRequest{titulo, descricao, categoria, prioridade}` com validação; `AlterarStatusRequest{status, horasTrabalhadas?, custo?, motivo?}`; `ChamadoResponse` completo com histórico; `PaginaResponse` |
| `ChamadoRepositoryImpl` | spring/repository | Implementa a interface do domain; consultas dinâmicas com `MongoTemplate`; `save` com `@Version` (bloqueio otimista) |
| `ChamadoRepositoryAdapter` | spring/repository/adapter | `paraDominio` / `paraOrm` |
| `ChamadoMongoRepository` | spring/repository/mongo | `MongoRepository<ChamadoOrmMongo, String>` |
| `ChamadoOrmMongo` | spring/repository/orm | Record `@Document(collection="chamados")` com `@Version` |
| `PublicadorEventosOutbox` | spring/messaging | Insere em `mensagensOutbox` dentro da transação corrente |
| `OutboxRelay` | spring/messaging | `@Scheduled`: publica PENDENTE com confirm e marca ENVIADA |
| `MensagemOutboxOrm`, `MensagemProcessadaOrm` | spring/messaging | Documentos Mongo |
| `RegistroMensagensProcessadas` | spring/messaging | `void registrar(String messageId)` — lança exceção de duplicidade tratada pelo consumidor |
| `ConsumidorEventosAtendimento` | spring/messaging | `@RabbitListener(queues="chamadoEventosAtendimentoQueue")`; um método, `switch` pela routing key (`chamado.triado` → `aplicarTriagem`, `chamado.atribuido` → `aplicarAtribuicao`); registra idempotência dentro da transação |
| `TransacaoExecutorMongo` | spring/persistence | Seção 4.2 |
| `GeradorProtocoloMongo` | spring/persistence | `findAndModify` com `$inc` na coleção `contadores` (`_id = "protocolo2026"`), upsert |
| `GlobalExceptionHandler` | spring/exception | 403/404/409/400/500 no formato `{"erro": "..."}` (igual ao Login) |

#### API (todas via Gateway, prefixo `/api/chamados`, autenticadas)

| Método e caminho | Quem | Descrição |
|---|---|---|
| `POST /api/chamados` | USUARIO e ADMIN | Abre chamado → `201` `{id, protocolo, status:"CRIADO"}` |
| `GET /api/chamados?status=&atendenteId=&resolvidoDe=&resolvidoAte=&pagina=&tamanho=` | USUARIO (vê só os seus) e ADMIN (vê todos) | Lista paginada. É também a fonte do Relatório |
| `GET /api/chamados/{id}` | dono ou ADMIN | Detalhe com histórico |
| `PATCH /api/chamados/{id}/status` | conforme transição (6.1) | Corpo `AlterarStatusRequest` |

### 7.4 `atendimento-service` (porta 8092)

#### Árvore completa

```
atendimento-service/
├── pom.xml  .env.example  .gitignore  Dockerfile  README.md
├── scripts/testeConcorrencia.mjs                    # demonstração (seção 8.3)
├── domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/suporteti/atendimento/
│       │   ├── model/{Departamento, Atendente, ItemFila, Sla, UsuarioLogado, ChamadoRecebido}.java
│       │   │        enumerable/StatusItemFila.java
│       │   ├── event/EventoSaida.java                 # ChamadoTriado, ChamadoAtribuido
│       │   ├── port/{PublicadorEventos.java, TransacaoExecutor.java}
│       │   ├── repository/{DepartamentoRepository, AtendenteRepository, FilaRepository, SlaRepository}.java
│       │   ├── service/{FilaService.java, CadastroService.java}
│       │   └── exception/{AcessoNegadoException, RecursoNaoEncontradoException, RegraDeNegocioException}.java
│       └── test/java/.../service/{FilaServiceTest.java, CadastroServiceTest.java}
└── spring/
    ├── pom.xml
    └── src/
        ├── main/java/com/suporteti/atendimento/
        │   ├── AtendimentoApplication.java
        │   ├── config/{ServiceConfig, MongoConfig, RabbitConfig, WebConfig, DadosIniciais}.java
        │   ├── controller/
        │   │   ├── FilaController.java
        │   │   ├── CadastroController.java
        │   │   ├── adapter/AtendimentoControllerAdapter.java
        │   │   ├── resolver/UsuarioLogadoResolver.java
        │   │   └── dto/{request/{DepartamentoRequest, AtendenteRequest, SlaRequest}.java,
        │   │            response/{ItemFilaResponse, DepartamentoResponse, AtendenteResponse, SlaResponse}.java}
        │   ├── repository/
        │   │   ├── {DepartamentoRepositoryImpl, AtendenteRepositoryImpl, FilaRepositoryImpl, SlaRepositoryImpl}.java
        │   │   ├── adapter/AtendimentoRepositoryAdapter.java
        │   │   ├── mongo/{DepartamentoMongoRepository, AtendenteMongoRepository, ItemFilaMongoRepository, SlaMongoRepository}.java
        │   │   └── orm/{DepartamentoOrm, AtendenteOrm, ItemFilaOrm, SlaOrm}.java
        │   ├── messaging/{PublicadorEventosOutbox, OutboxRelay, MensagemOutboxOrm, MensagemProcessadaOrm,
        │   │              RegistroMensagensProcessadas, ConsumidorEventosChamado}.java
        │   ├── persistence/TransacaoExecutorMongo.java
        │   └── exception/GlobalExceptionHandler.java
        ├── main/resources/application.yaml
        └── test/java/.../AtribuicaoConcorrenteIT.java
```

#### Responsabilidades (diferenças em relação ao molde)

| Arquivo | Responsabilidade |
|---|---|
| `Departamento` | `id, nome, categorias (List<String>), ativo` |
| `Atendente` | `id` (= id do usuário no Login), `nome, departamentoId, capacidade, chamadosAtivos, ativo` |
| `ItemFila` | `id, chamadoId (único), protocolo, categoria, prioridade, departamentoId, status, atendenteId, prazoSla, criadoEm, atribuidoEm, concluidoEm, motivo, versao` |
| `Sla` | `prioridade, horas` |
| `ChamadoRecebido` | Entrada do caso de uso `triar` (campos do evento `chamado.criado`) |
| `StatusItemFila` | `EM_ANALISE, NA_FILA, ATRIBUIDO, CONCLUIDO, REJEITADO` |
| `EventoSaida` | `ChamadoTriado` (`chamado.triado`) e `ChamadoAtribuido` (`chamado.atribuido`) |
| `FilaRepository` (interface) | `inserir(ItemFila)`; `buscarPorChamadoId`; `listar(departamentoId, status)`; **`Optional<ItemFila> reivindicar(chamadoId, departamentoId, atendenteId, instante)`** — contrato: atômico, devolve vazio se o item não estiver `NA_FILA` naquele departamento; `concluir(chamadoId, instante)` |
| `AtendenteRepository` (interface) | `findById`, `save`, `listar`; **`boolean reservarCapacidade(atendenteId)`** (atômico: só incrementa `chamadosAtivos` se `< capacidade` e `ativo`); `liberarCapacidade(atendenteId)` |
| `FilaService` | `triar(ChamadoRecebido)`: cria o item `EM_ANALISE`, valida categoria (existe em algum departamento ativo) e prioridade (tem SLA); se OK → `NA_FILA` no departamento da categoria, calcula `prazoSla`, publica `ChamadoTriado(ACEITO)`; senão → `REJEITADO` + `ChamadoTriado(REJEITADO, motivo)`. `assumir(usuario, chamadoId)`: ver 8.1. `concluir(chamadoId, atendenteId)`: marca `CONCLUIDO`, libera capacidade. `listar(departamentoId, status)` |
| `CadastroService` | CRUD de departamentos, atendentes e SLA (o ADMIN gerencia filas e atendentes). `listarCategorias()` para o front |
| `ConsumidorEventosChamado` | `@RabbitListener(queues="atendimentoEventosChamadoQueue")`; `switch` pela routing key: `chamado.criado` → `triar`; `chamado.resolvido` e `chamado.rejeitado` → `concluir` |
| `DadosIniciais` | Seed: departamentos (Infraestrutura, Redes, Software, AcessoContas, Hardware) com categorias e os 4 SLAs |
| `scripts/testeConcorrencia.mjs` | Demonstração com requisições reais (8.3) |

#### API (via Gateway, prefixo `/api/atendimento`)

| Método e caminho | Quem | Descrição |
|---|---|---|
| `GET /categorias` | autenticado | Lista de categorias para o formulário de abertura |
| `GET /fila?departamentoId=&status=` | ADMIN | Gestão de filas |
| `POST /fila/{chamadoId}/assumir` | ADMIN com perfil de atendente | Atribuição concorrente-segura → `200` `ItemFilaResponse` ou `409` (já assumido / sem capacidade / outro departamento) |
| `GET/POST /departamentos`, `PUT /departamentos/{id}` | ADMIN | Departamentos e categorias |
| `GET/POST /atendentes`, `PUT /atendentes/{id}` | ADMIN | Atendentes (`POST` sem `id` cria o perfil do próprio ADMIN logado) |
| `GET /slas`, `PUT /slas/{prioridade}` | ADMIN | SLA por prioridade |

### 7.5 `relatorio-service` (porta 8093)

Sem banco e sem mensageria (regra do enunciado: pub/sub só entre Chamado e Atendimento). Busca os dados por HTTP **pelo Gateway**, repassando o cookie/Authorization da requisição original (as transações e o acesso a dados continuam entrando pelo Gateway). Os cálculos ficam no domain, testáveis sem Spring.

```
relatorio-service/
├── pom.xml  .env.example  .gitignore  Dockerfile  README.md
├── domain/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/suporteti/relatorio/
│       │   ├── model/{ChamadoConcluido.java, RelatorioMensal.java}
│       │   ├── port/ChamadosClient.java
│       │   ├── service/RelatorioService.java
│       │   └── exception/{AcessoNegadoException, RegraDeNegocioException, ServicoIndisponivelException}.java
│       └── test/java/.../service/RelatorioServiceTest.java
└── spring/
    ├── pom.xml
    └── src/main/java/com/suporteti/relatorio/
        ├── RelatorioApplication.java
        ├── config/{ServiceConfig.java, HttpClientConfig.java}
        ├── controller/{RelatorioController.java, adapter/RelatorioControllerAdapter.java, dto/response/RelatorioMensalResponse.java}
        ├── client/{ChamadosClientHttp.java, ChamadoConcluidoDto.java}
        └── exception/GlobalExceptionHandler.java
```

| Arquivo | Responsabilidade |
|---|---|
| `ChamadoConcluido` | Record com os campos usados nas métricas: `protocolo, categoria, prioridade, departamentoNome, atendenteId, atendenteNome, criadoEm, atribuidoEm, resolvidoEm, prazoSla, horasTrabalhadas, custo` |
| `RelatorioMensal` | Record: `ano, mes, totalConcluidos, tempoMedioAtendimentoHoras, tempoMedioPrimeiraRespostaHoras, percentualSlaCumprido, horasTotais, custoTotal, porCategoria, porPrioridade, porAtendente` (listas de linhas `{chave, quantidade, horas, custo}`) |
| `ChamadosClient` | `List<ChamadoConcluido> listarConcluidos(YearMonth periodo)` |
| `RelatorioService` | `gerar(boolean admin, YearMonth periodo)`: exige ADMIN, valida período, chama o client, calcula. **Definições:** concluído = status `RESOLVIDO` ou `FECHADO` com `resolvidoEm` dentro do mês no fuso `America/Sao_Paulo`; tempo de atendimento = `resolvidoEm − criadoEm`; primeira resposta = `atribuidoEm − criadoEm`; SLA cumprido = `resolvidoEm <= prazoSla`; custo e horas = soma dos valores registrados na resolução |
| `ChamadosClientHttp` | `RestClient` → `GET {GATEWAY_URL}/api/chamados?status=RESOLVIDO,FECHADO&resolvidoDe=&resolvidoAte=&tamanho=200&pagina=N` em laço até esgotar; repassa o header `Cookie` da requisição corrente (`RequestContextHolder`). Falha → `ServicoIndisponivelException` (502) |
| `RelatorioController` | `GET /api/relatorio/mensal?ano=&mes=` (padrão: mês corrente). Defesa em profundidade: lê `X-Usuario-Role` e passa `admin` ao service (além do bloqueio no Gateway) |
| `HttpClientConfig` | `RestClient` com base `GATEWAY_URL` e timeouts |

---

## 8. Concorrência

### 8.1 Problema e solução

Dois técnicos clicam em "assumir" no mesmo chamado ao mesmo tempo; ou um técnico tenta passar da sua capacidade. Camadas de proteção, todas dentro de **uma transação** (`FilaService.assumir`):

1. **Capacidade** — `AtendenteRepository.reservarCapacidade(id)`: `findAndModify` com filtro `{_id, ativo:true, $expr:{$lt:["$chamadosAtivos","$capacidade"]}}` e `$inc: {chamadosAtivos: 1}`. Sem leitura-depois-escrita, então não há corrida. Se não modificar nada → `RegraDeNegocioException("Capacidade esgotada")`.
2. **Reivindicação do item** — `FilaRepository.reivindicar(...)`: `findAndModify` com filtro `{chamadoId, departamentoId, status:"NA_FILA"}` e `$set: {status:"ATRIBUIDO", atendenteId, atribuidoEm}`. Só **um** documento satisfaz o filtro; o perdedor recebe vazio → `RegraDeNegocioException("Chamado já assumido")` → **409**. A exceção reverte a reserva de capacidade do passo 1 (rollback).
3. **Evento** — `ChamadoAtribuido` gravado no outbox na mesma transação.
4. **Conflito entre transações** — duas transações tocando o mesmo documento geram `WriteConflict` (`TransientTransactionError`); o `TransacaoExecutorMongo` repete até 3 vezes e, na repetição, o filtro do passo 2 já não casa → 409 limpo.
5. **Rede de segurança** — `@Version` em `ItemFilaOrm`, `AtendenteOrm` e `ChamadoOrmMongo` (bloqueio otimista); índice único em `filaAtendimento.chamadoId`.

### 8.2 Testes automatizados

| Teste | Onde | O que verifica |
|---|---|---|
| `FilaServiceTest` | atendimento/domain (fakes, JUnit) | Regras: triagem aceita/rejeita, capacidade, departamento errado, transições |
| `AtribuicaoConcorrenteIT` | atendimento/spring (Testcontainers com `MongoDBContainer("mongo:7")`, que já sobe como replica set; exige Docker ligado) | **Cenário A:** 1 item `NA_FILA`, 10 atendentes, 10 threads com `CountDownLatch` chamando `assumir` ao mesmo tempo → exatamente 1 sucesso, 9 `RegraDeNegocioException`; o item tem 1 só `atendenteId`; soma de `chamadosAtivos` = 1. **Cenário B:** 1 atendente com capacidade 3 e 10 itens → 10 threads → no máximo 3 sucessos e `chamadosAtivos` ≤ 3. Verificar o nome do artefato do Testcontainers MongoDB na versão gerenciada pelo Boot 4.0.6 |
| `StatusChamadoTest` | chamado/domain | Todas as transições válidas e inválidas da tabela 6.1 |
| `ChamadoServiceTest` | chamado/domain (fakes) | Quem pode cada transição; USUARIO só vê os próprios; resolver exige horas e custo |
| `RelatorioServiceTest` | relatorio/domain | Médias, SLA, somas e agrupamentos com dados fixos; fronteira de mês no fuso de São Paulo |
| `SecurityConfigTest` | gateway | Seção 7.2 |

### 8.3 Demonstração ao vivo — `scripts/testeConcorrencia.mjs` (Node 18+, sem dependências)

1. Faz login via Gateway como USUARIO e como N ADMINs (o `fetch` do Node não guarda cookie: ler `set-cookie` da resposta e reenviar no header `Cookie`).
2. Abre um chamado e espera `status = EM_ANALISE` (polling de `GET /api/chamados/{id}`).
3. Dispara **N `POST /api/atendimento/fila/{chamadoId}/assumir` ao mesmo tempo** (`Promise.all`), um por ADMIN.
4. Imprime a tabela de resultados: **1×200 e (N−1)×409**.
5. Confere por `GET /api/chamados/{id}` que o chamado ficou `EM_ANDAMENTO` com um único atendente.

---

## 9. Docker e variáveis de ambiente

### 9.1 Infra (`docker-compose.yaml` do repositório `gateway`)

Já tem Mongo 7 como replica set `rs0` (com healthcheck que faz o `rs.initiate`) e RabbitMQ local opcional. Manter. Subir: `docker compose up -d mongo` (e `docker compose --profile rabbitLocal up -d rabbitmq` só se não for usar o CloudAMQP).

### 9.2 Rodar os serviços

- **Modo desenvolvimento (recomendado para a apresentação):** infra no Docker; cada serviço no host (`mvn -pl spring spring-boot:run` ou `./mvnw spring-boot:run`). URI do Mongo: `mongodb://localhost:27017/<banco>?replicaSet=rs0`.
- **Modo containers (opcional):** o `chamado-service` usa Docker multi-estágio com Eclipse Temurin 25 (JDK para build e JRE para execução); os demais serviços usam as versões definidas em seus próprios Dockerfiles. Para os containers alcançarem o Mongo, usar `mongodb://mongo:27017/<banco>?directConnection=true` (com `directConnection` o driver não segue o endereço `localhost:27017` anunciado pelo replica set, e transações continuam funcionando porque o nó único é o primário). Um `docker-compose.apps.yaml` no repositório do Gateway pode subir tudo; é opcional.

### 9.3 `.env.example` por serviço

| Serviço | Variáveis |
|---|---|
| gateway | `JWT_SECRET` (≥ 32 caracteres, **igual ao do Login**), `COOKIE_NOME=tokenSuporteTi`, `LOGIN_URL`, `CHAMADO_URL`, `ATENDIMENTO_URL`, `RELATORIO_URL` |
| login | `JWT_SECRET`, `JWT_EXPIRACAO_MINUTOS=60`, `COOKIE_NOME=tokenSuporteTi`, `ADMIN_CODIGO`, `ADMIN_EMAIL`, `ADMIN_SENHA`, `MONGO_URI` |
| chamado | `MONGO_URI` (banco `chamadoDb`), `CLOUDAMQP_URL`, `TZ_APP=America/Sao_Paulo` |
| atendimento | `MONGO_URI` (banco `atendimentoDb`), `CLOUDAMQP_URL`, `TZ_APP=America/Sao_Paulo` |
| relatorio | `GATEWAY_URL=http://localhost:8080`, `TZ_APP=America/Sao_Paulo` |

`.env` nunca vai para o Git (`.gitignore`); o CloudAMQP exige URL secreta.

---

## 10. Fluxos ponta a ponta

### 10.1 Abrir e acompanhar (USUARIO)

```
1  Front  POST /api/chamados                         → Gateway valida cookie → chamado-service
2  chamado  [TX] insert chamados(CRIADO) + insert mensagensOutbox(chamado.criado)   → 201 {protocolo}
3  OutboxRelay → chamadosExchange / chamado.criado → atendimentoEventosChamadoQueue
4  atendimento  [TX] mensagensProcessadas + insert filaAtendimento(EM_ANALISE→NA_FILA, prazoSla) + outbox(chamado.triado)
5  OutboxRelay → atendimentoExchange / chamado.triado → chamadoEventosAtendimentoQueue
6  chamado  [TX] mensagensProcessadas + chamado: CRIADO → EM_ANALISE (ou REJEITADO), departamento e prazoSla preenchidos
7  Front  GET /api/chamados / GET /api/chamados/{id}  (USUARIO só vê os seus)
```

### 10.2 Atender (ADMIN/técnico)

```
1  Front  GET /api/atendimento/fila?departamentoId=…        (ADMIN)
2  Front  POST /api/atendimento/fila/{chamadoId}/assumir
3  atendimento  [TX] reservarCapacidade + reivindicar + outbox(chamado.atribuido)   → 200 | 409
4  chamado  recebe chamado.atribuido: EM_ANALISE → EM_ANDAMENTO (atendente preenchido)
5  Front  PATCH /api/chamados/{id}/status {AGUARDANDO_TERCEIRO}  → … → {EM_ANDAMENTO}
6  Front  PATCH /api/chamados/{id}/status {RESOLVIDO, horasTrabalhadas, custo}
7  chamado  [TX] chamado RESOLVIDO + outbox(chamado.resolvido)
8  atendimento  recebe chamado.resolvido: item CONCLUIDO + libera capacidade
9  USUARIO  PATCH /api/chamados/{id}/status {FECHADO}
```

### 10.3 Relatório (ADMIN)

`GET /api/relatorio/mensal?ano=2026&mes=10` → Gateway (ADMIN) → relatório → `GET /api/chamados?status=RESOLVIDO,FECHADO&resolvidoDe=…&resolvidoAte=…` pelo Gateway com o mesmo cookie → cálculo no domain → JSON.

---

## 11. Ordem de execução e critérios de aceite

| Fase | Entrega | Critério de aceite |
|---|---|---|
| 1 | **Login** (7.1) | `POST /api/auth/register` com `role=USUARIO` e com `role=ADMIN` + `codigoAdmin`; resposta sem token no corpo; `Set-Cookie: tokenSuporteTi=…; HttpOnly; SameSite=Lax`; testes do domain verdes |
| 2 | **Gateway** (7.2) | Sobe com as 4 rotas; `SecurityConfigTest` verde; `GET /api/auth/me` com cookie → 200; sem cookie → 401; `/api/usuarios/{id}` alcançável |
| 3 | **atendimento-service** (7.4), sem Rabbit ainda | Seed carregado; testes do domain verdes; `AtribuicaoConcorrenteIT` verde (cenários A e B) |
| 4 | **chamado-service** (7.3), sem Rabbit ainda | Abrir/listar/detalhar/alterar status via Gateway; testes do domain verdes (todas as transições) |
| 5 | **Mensageria** (outbox, relay, consumidores, topologia, DLQ) nos dois serviços | Fluxo 10.1 completo até `EM_ANALISE`; fluxo 10.2 completo; derrubar o consumidor, abrir chamados, subir de novo → chamados são processados (nada se perde); mensagem duplicada não duplica efeito; mensagem inválida cai na DLQ |
| 6 | **relatorio-service** (7.5) | `RelatorioServiceTest` verde; relatório do mês bate com os chamados resolvidos; USUARIO recebe 403 |
| 7 | **Front** (ajustes) | Seção 12.1 |
| 8 | **Script de concorrência** e ensaio da apresentação | `testeConcorrencia.mjs` mostra 1×200 e N−1×409 |

---

## 12. Apresentação

### 12.1 Ajustes mínimos no front (não recebi o código; confirmar com ele)

- Base URL `http://localhost:8080/api` (Gateway); toda chamada com `credentials: "include"` (e CORS do Gateway com a origem do front, já tem 8081/19006).
- **Não** ler nem guardar token: o login é por cookie HttpOnly.
- Cadastro com seletor de perfil (Usuário / Admin-Técnico) e campo "código de administrador".
- Telas: login/cadastro; **usuário**: abrir chamado, lista dos meus, detalhe com histórico, fechar chamado resolvido; **admin**: fila por departamento com botão "assumir", meus chamados em atendimento com troca de status e resolução (horas e custo), gestão de departamentos/atendentes/SLA, relatório mensal.
- Isolamento de rotas no front: esconder o que o role não pode ver (conveniência); **a segurança real é do Gateway** (mostrar na demonstração).

### 12.2 Roteiro (sugestão de 10–12 minutos)

1. **Arquitetura (2 min)** — abrir este desenho: 5 serviços, Gateway na frente, único pub/sub entre Chamado e Atendimento. Explicar o porquê de cada decisão (seção 13).
2. **Fluxo do usuário (2 min)** — cadastrar USUARIO, abrir chamado, ver `CRIADO → EM_ANALISE` aparecer (mensageria acontecendo), abrir o painel do CloudAMQP mostrando exchanges, queues e mensagens.
3. **Isolamento por auth (2 min)** — com o usuário logado, tentar abrir `/api/relatorio/mensal` e `/api/atendimento/fila` (403) e a tela de admin no front (bloqueada); sem login → 401.
4. **Cookie (1 min)** — DevTools → Application → Cookies: `tokenSuporteTi` com HttpOnly e SameSite=Lax; no Console, `document.cookie` não mostra o token; apagar o cookie → próxima requisição dá 401; fazer logout.
5. **Fluxo do admin (2 min)** — cadastrar ADMIN (com código), ver a fila, assumir, mudar status (Aguardando Terceiro → Em Andamento), resolver com horas e custo; mostrar o usuário vendo o status; usuário fecha o chamado.
6. **Teste de concorrência (2 min)** — rodar `node scripts/testeConcorrencia.mjs` (1×200, N−1×409) e mostrar o `AtribuicaoConcorrenteIT` verde.
7. **Relatório (1 min)** — consolidado do mês: concluídos, tempo médio, SLA, horas e custo.
8. **Perguntas** — seção 13.

Antes de apresentar: Docker com Mongo ligado, `.env` preenchidos, CloudAMQP acessível, dados limpos (apagar `chamadoDb`/`atendimentoDb`), 3 ADMINs e 1 USUARIO já cadastrados, uma aba com o painel do CloudAMQP, uma aba com o DevTools.

---

## 13. Defesa: pontos que o professor tende a cobrar

> Não tenho a rubrica oficial do professor. Esta tabela é construída a partir dos requisitos que você listou e das correções que ele fez no trabalho anterior. **Substitua/complete com a pontuação real que ele passar**, mantendo a coluna "onde mostrar no código".

| Critério | O que ele quer ver | Onde mostrar | Resposta curta |
|---|---|---|---|
| Clean Architecture | Domain sem framework; dependências apontando para dentro | `domain/pom.xml` (sem Spring), `ServiceConfig`, `port/` e `repository/` como interfaces | "O domain só conhece interfaces. Trocar Mongo ou Rabbit não altera nenhuma regra de negócio; os testes do domain rodam sem Spring." |
| Separação ORM × domínio | Record de domínio ≠ documento Mongo | `ChamadoOrmMongo`, `ChamadoRepositoryAdapter` | "O ORM vive na borda; o adapter converte." |
| Transações | Operação multi-documento atômica | `TransacaoExecutor`, `FilaService.assumir`, `MongoConfig` | "Reservar capacidade, reivindicar o item e gravar o evento são tudo-ou-nada. Mongo exige replica set; por isso o `rs0`." |
| Por que Outbox | Consistência entre banco e fila | `PublicadorEventosOutbox`, `OutboxRelay` | "Sem outbox, uma queda entre gravar e publicar deixaria banco e fila divergentes (dual write)." |
| Idempotência | Duplicatas não duplicam efeito | `RegistroMensagensProcessadas`, índice único | "O Rabbit entrega pelo menos uma vez; o `messageId` único torna o efeito único." |
| Mensageria assíncrona | Pub/sub real, só entre os dois serviços | `RabbitConfig`, contrato 5.3 | "Topic exchange: o publicador não conhece o consumidor; dá para acrescentar outro consumidor sem mudar quem publica." |
| Resiliência | Falha de consumidor | DLQ, retry, queue durável | "Mensagem não se perde: persiste, tenta 3 vezes e vai para a DLQ." |
| Concorrência | Dois atendentes, um chamado | `reivindicar` (filtro por status atômico), `AtribuicaoConcorrenteIT`, script | "O filtro `status=NA_FILA` no `findAndModify` deixa passar só um; o perdedor recebe 409." |
| Máquina de estados | Transições controladas | `StatusChamado.podeIrPara`, `StatusChamadoTest` | "Transição inválida nunca chega ao banco; tabela única, testada." |
| Autenticação por cookie | HttpOnly, validação no Gateway | `TokenCookieFactory`, `CookieBearerTokenConverter` | "O JS não lê o token (mitiga XSS); `SameSite=Lax` e CORS por lista mitigam CSRF — por isso o CSRF está desabilitado, com justificativa no código." |
| Autorização em camadas | Gateway + serviço | `SecurityConfig` + regra no service | "O Gateway barra por role; o service decide dono × ADMIN (um usuário não lê chamado de outro)." |
| Gateway como único ponto | Serviços não confiam em header do cliente | `UsuarioHeadersFilter` | "O filtro apaga `X-Usuario-*` vindos de fora e escreve os do JWT." |
| Sem commons | Justificar a duplicação | Seção 3.4 | "Acoplar por contrato JSON evita publicar e versionar um jar comum." |
| CamelCase | Nomes consistentes | Seção 2.1 | Mostrar rotas, coleções, queues, volume e cookie em camelCase |
| Sem re-criação / encapsulamento | Um resolver, um service por agregado, um adapter por camada | Seção 2.2 | "Um endpoint de status em vez de um por ação; um `UsuarioLogadoResolver` em vez de header em cada método." |
| Testes | Domínio e concorrência | Seção 8.2 | Rodar `mvn test` ao vivo |
| Relatório | Métricas corretas | `RelatorioService` | Definições explícitas (7.5) |

Perguntas que costumam aparecer e a resposta honesta:

- **"E se alguém chamar o serviço direto, sem o Gateway?"** Forjaria `X-Usuario-*`. Em localhost, mitigar subindo os serviços só em `127.0.0.1`; em produção, rede privada ou um segredo compartilhado Gateway↔serviço. Está registrado como limitação.
- **"Por que o relatório não consome mensagens?"** Porque o enunciado limita o pub/sub a Chamado e Atendimento; o relatório lê pelo Gateway e calcula na hora.
- **"O status é imediato?"** `EM_ANALISE` e `EM_ANDAMENTO` chegam por evento (milissegundos de atraso): consistência eventual, aceita na mensageria assíncrona.
- **"Por que Mongo e não SQL?"** O Login e o Gateway já usam Mongo com replica set preparado para transação; reaproveitar infraestrutura.

---

## 14. Pontos que dependem do aluno (confirmar antes ou durante a execução)

1. **Fork do Login:** o fork `VNav4rr0/LOGIN` é um esqueleto; o plano usa o `ViniciusVChabariberi/LOGIN` completo. Confirmar qual é o repositório de trabalho.
2. **"Todas as transações passam pelo Gateway":** interpretei como "toda requisição de cliente e toda leitura de dados entre serviços HTTP passam pelo Gateway" (por isso o Relatório consulta o Chamado via Gateway repassando o cookie). Se o professor quis dizer outra coisa (por exemplo, que o Gateway orquestra a transação), ajustar a seção 7.5 e o fluxo 10.3.
3. **Dois roles (USUARIO e ADMIN):** "Admin/Técnico" foi tratado como um role só. Se o professor quiser três (USUARIO, TECNICO, ADMIN), acrescentar o enum e uma regra no Gateway.
4. **Projetos `produtos` e `cliente`:** não recebi; a seção 2.2 deve ser conferida com o feedback escrito do professor.
5. **Código do front:** não recebi; a seção 12.1 descreve só o necessário. Mandar o código para fechar o cliente HTTP e as telas.
6. **Rubrica real do professor:** substituir a seção 13.
7. **Conversor JSON do Spring AMQP no Boot 4.0.6 e artefato do Testcontainers:** confirmar os nomes ao montar os `pom.xml` (seções 5.2 e 8.2).
8. **Não incluído de propósito (extensões, se sobrar tempo):** devolver chamado à fila, reabrir chamado, notificações, paginação no relatório além do laço, fila de reprocessamento da DLQ.
