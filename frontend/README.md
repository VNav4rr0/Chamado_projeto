# Central de Chamados - Portal Web Corporativo (React Native Web + TypeScript)

Portal Web Corporativo para gerenciamento de suporte técnico desenvolvido com **React Native for Web**, **TypeScript**, **Expo** e **Material Design 3 (React Native Paper)**.

O sistema foi arquitetado para visualização em **navegadores desktop**, contando com top navbar fixa, layout em duas colunas, grids responsivos de cards, dashboard com KPIs, monitoramento em tempo real de SLA e simulação completa de mensageria assíncrona orientada a eventos (**RabbitMQ & Transactional Outbox**).

---

## 💻 Características do Portal Web

### 1. 🌐 Top Navbar Corporativa Fixa (`WebNavbar.tsx`)
- Identidade visual com logo estilizado e indicação de status do sistema (*Portal Web Enterprise*).
- Navegação desktop por abas no topo:
  - 📋 **Meus Chamados**
  - ➕ **Abrir Chamado**
  - 👥 **Equipe Técnica**
  - ⚙️ **Configurações**
- Indicador em tempo real de infraestrutura: *RabbitMQ Ativo* ou *Gateway Conectado*.
- Alternador de tema claro/escuro e perfil do usuário logado.

### 2. 📊 Dashboard de Chamados (`ChamadosListScreen.tsx`)
- **4 Cards Horizontais de KPI**: Total Geral, Em Andamento, Fila de Espera e Resolvidos com micro-ícones e tipografia display de alta fidelidade.
- **Barra de Controle & Filtros**:
  - Busca em tempo real por protocolo (ex: `CH-2026-000101`), título ou técnico.
  - Abas de status com badges contadores numéricos.
  - Seletor de departamentos (*Rede*, *Hardware*, *Software*, *Acesso*).
- **Grid de Cards Responsivo**: Exibição em duas colunas no desktop com barra lateral de acento colorido, SLA dinâmico e cursor pointer.

### 3. ➕ Abertura de Chamado em Duas Colunas (`NovoChamadoScreen.tsx`)
- **Coluna Principal (60%)**:
  - Seleção de categoria em grid 2x2 com cards ilustrados e checkmark ativo.
  - Seleção de prioridade com cálculo automático de SLA (4h, 8h, 24h e 72h).
  - Campos de entrada de título e descrição multilinha com validação em tempo real.
- **Coluna Lateral (40%)**:
  - Explicação do ciclo de vida assíncrono (Transactional Outbox e HTTP 202 Accepted).
  - Card de previsão de atendimento mostrando os técnicos ativos da especialidade e suas cargas atuais.
  - Botão de confirmação em gradiente com feedback visual imediato.

### 4. 🔍 Detalhes do Chamado em Layout Master-Detail (`ChamadoDetailScreen.tsx`)
- Breadcrumbs no topo para navegação fluida.
- **Coluna Principal (65%)**:
  - Protocolo em fonte destacada e pill de status.
  - Descrição detalhada do problema.
  - Linha do tempo visual cronológica (`TimelineView.tsx`) mostrando todas as transições de status e eventos RabbitMQ.
- **Coluna Lateral (35%)**:
  - Acordo de Nível de Serviço (SLA) com barra regressiva dinâmica.
  - Card completo do técnico alocado com avatar em gradiente e status.
  - Botão de ação para resolução (`PATCH /api/chamados/{id}/resolver`) com liberação de carga e desrepresamento.

### 5. 👥 Painel da Equipe Técnica (`PainelTecnicosScreen.tsx`)
- Medidor amplo de ocupação geral da equipe técnica com percentual e alertas por cor.
- Grid dos 8 técnicos oficiais pré-cadastrados (*Roberto Redes*, *Renata Roteadores*, *Hugo Hardware*, *Helena Hard*, *Sofia Software*, *Samuel Sistemas*, *Alice Acessos*, *Arthur Autenticação*).
- Barras de capacidade individual (*Carga Atual / Capacidade Máxima*).

### 6. ⚙️ Configurações & Topologia (`SettingsScreen.tsx`)
- Alternador de Modo Simulado (Mock RabbitMQ) vs Conexão com o Spring Cloud Gateway na porta `8080`.
- Campo para configuração da URL da API e identificador do usuário (`X-Usuario-Id`).
- Visão da topologia de microsserviços do backend (Gateway, chamado-service, atendimento-service e RabbitMQ).
- Botão para restauração de dados originais de fábrica.

---

## 🚀 Como Executar no Navegador Web

No terminal, dentro da pasta do projeto:

```bash
cd c:\Users\vn120\Downloads\chamado
```

### 1. Iniciar a aplicação web
```bash
npm start
```
*(O comando `npm start` já está configurado para abrir diretamente a versão Web!)*

Ou execute explicitamente:
```bash
npx expo start --web
```

A aplicação abrirá no seu navegador padrão (geralmente em `http://localhost:8081`).
