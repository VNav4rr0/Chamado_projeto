# Login

Cadastro, login e emissao do JWT do AgendaFlow. Porta 8090.

## Endpoints (via Gateway, prefixo /api)
- POST /auth/register  { nome, email, senha, role?, codigoAdmin? }
- POST /auth/login     { email, senha }  -> { token, usuario } + cookie
- POST /auth/logout
- GET  /auth/me
- GET|PUT|DELETE /usuarios/{id}  (proprio usuario ou ADMIN)

## Como rodar
Requer Java 25 (LTS) e Maven.

1. docker compose up -d mongo (compose no repositorio do Gateway)
2. Copie .env.example para .env e preencha JWT_SECRET (o mesmo do Gateway)
3. mvn -pl spring spring-boot:run

## Decisoes
- Domain sem Spring; BCrypt e JWT entram por portas.
- O Login nao valida token: quem valida e o Gateway.
- Cadastro como ADMIN exige ADMIN_CODIGO.
- Cookie HttpOnly + SameSite=Lax. Em producao com HTTPS passa a ser Secure.