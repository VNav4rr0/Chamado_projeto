# Login

Cadastro, login e emissao do JWT do Sistema de Chamados de Suporte de TI. Porta 8090.

## Endpoints (via Gateway, prefixo /api)
- POST /auth/register  { nome, email, senha, role?, codigoAdmin? }  -> { id, nome, email, role } + cookie
- POST /auth/login     { email, senha }  -> { id, nome, email, role } + cookie
- POST /auth/logout
- GET  /auth/me
- GET|PUT|DELETE /usuarios/{id}  (proprio usuario ou ADMIN)

Roles: USUARIO (padrao) e ADMIN. O token NAO vai no corpo da resposta: so no cookie HttpOnly `tokenSuporteTi`.

## Como rodar
1. docker compose up -d mongo (compose no repositorio do Gateway)
2. Copie .env.example para .env e preencha JWT_SECRET (o mesmo do Gateway)
3. mvn install -DskipTests e depois mvn -pl spring spring-boot:run

## Decisoes
- Domain sem Spring; BCrypt e JWT entram por portas.
- O Login nao valida token: quem valida e o Gateway, que repassa a identidade em X-Usuario-Id, X-Usuario-Role e X-Usuario-Nome (lidos por um unico UsuarioLogadoResolver).
- Cadastro como ADMIN exige ADMIN_CODIGO.
- Cookie HttpOnly + SameSite=Lax. Em producao com HTTPS passa a ser Secure.