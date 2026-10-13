# Aplica a Fase 1 no LOGIN. Rode DENTRO da pasta LOGIN (a que tem as pastas domain e spring).
if (-not (Test-Path 'domain') -or -not (Test-Path 'spring')) { Write-Host 'ERRO: rode dentro da pasta LOGIN.' -ForegroundColor Red; return }
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Salvar($caminho, $texto) {
  $completo = Join-Path (Get-Location) $caminho
  New-Item -ItemType Directory -Force -Path (Split-Path $completo) | Out-Null
  [System.IO.File]::WriteAllText($completo, $texto, $utf8)
  Write-Host "OK  $caminho"
}

Salvar '.env.example' @'
# Copie para .env (ja ignorado pelo git).
# JWT_SECRET deve ser IDENTICO ao do Gateway, com no minimo 32 caracteres.
JWT_SECRET=sua_chave_secreta_aqui_minimo_32_caracteres
JWT_EXPIRACAO_MINUTOS=60
COOKIE_NOME=tokenSuporteTi
ADMIN_CODIGO=codigo_para_criar_admin
ADMIN_EMAIL=admin@suporteti.com
ADMIN_SENHA=senha_do_admin
MONGO_URI=mongodb://localhost:27017/loginDb?replicaSet=rs0
'@

Salvar 'README.md' @'
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
'@

Salvar 'domain\src\main\java\com\vinijoao\login\model\UsuarioLogado.java' @'
package com.vinijoao.login.model;

/** Quem esta fazendo a requisicao (identidade que o Gateway repassa nos headers). */
public record UsuarioLogado(String id, String nome, boolean admin) {
}
'@

Salvar 'domain\src\main\java\com\vinijoao\login\model\enumerable\Role.java' @'
package com.vinijoao.login.model.enumerable;

public enum Role {
    ADMIN, USUARIO
}
'@

Salvar 'domain\src\main\java\com\vinijoao\login\service\AuthService.java' @'
package com.vinijoao.login.service;

import com.vinijoao.login.exception.AcessoNegadoException;
import com.vinijoao.login.exception.CredenciaisInvalidasException;
import com.vinijoao.login.exception.RecursoNaoEncontradoException;
import com.vinijoao.login.exception.RegraDeNegocioException;
import com.vinijoao.login.model.Sessao;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.UsuarioLogado;
import com.vinijoao.login.model.enumerable.Role;
import com.vinijoao.login.port.SenhaCodificador;
import com.vinijoao.login.port.TokenEmissor;
import com.vinijoao.login.repository.UsuarioRepository;

public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final SenhaCodificador senhaCodificador;
    private final TokenEmissor tokenEmissor;
    private final String codigoAdmin;

    public AuthService(
            UsuarioRepository usuarioRepository,
            SenhaCodificador senhaCodificador,
            TokenEmissor tokenEmissor,
            String codigoAdmin
    ) {
        this.usuarioRepository = usuarioRepository;
        this.senhaCodificador = senhaCodificador;
        this.tokenEmissor = tokenEmissor;
        this.codigoAdmin = codigoAdmin;
    }

    private static String normalizar(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    public Usuario cadastrar(String nome, String email, String senha, Role roleSolicitada, String codigoAdminInformado) {
        String emailFinal = normalizar(email);
        if (usuarioRepository.existsByEmail(emailFinal)) {
            throw new RegraDeNegocioException("E-mail ja cadastrado.");
        }

        Role role = Role.USUARIO;
        if (roleSolicitada == Role.ADMIN) {
            if (codigoAdminInformado == null || !codigoAdminInformado.equals(codigoAdmin)) {
                throw new AcessoNegadoException("Codigo de administrador invalido.");
            }
            role = Role.ADMIN;
        }

        String senhaHash = senhaCodificador.codificar(senha);
        Usuario usuario = new Usuario(null, nome, emailFinal, senhaHash, role);
        return usuarioRepository.save(usuario);
    }

    public Sessao autenticar(String email, String senha) {
        Usuario usuario = usuarioRepository.findByEmail(normalizar(email))
                .orElseThrow(CredenciaisInvalidasException::new);
        if (!senhaCodificador.confere(senha, usuario.senhaHash())) {
            throw new CredenciaisInvalidasException();
        }
        return iniciarSessao(usuario);
    }

    public Usuario buscarPorId(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado."));
    }

    public Sessao iniciarSessao(Usuario usuario) {
        return new Sessao(usuario, tokenEmissor.emitir(usuario));
    }

    public Usuario consultar(UsuarioLogado solicitante, String id) {
        verificarPermissao(solicitante, id);
        return buscarPorId(id);
    }

    public Usuario atualizar(UsuarioLogado solicitante, String id,
                             String novoNome, String novoEmail, String novaSenha) {
        verificarPermissao(solicitante, id);
        Usuario existente = buscarPorId(id);

        String emailFinal = (novoEmail != null && !novoEmail.isBlank()) ? normalizar(novoEmail) : existente.email();
        if (!emailFinal.equals(existente.email()) && usuarioRepository.existsByEmail(emailFinal)) {
            throw new RegraDeNegocioException("E-mail ja cadastrado.");
        }
        String nomeFinal = (novoNome != null && !novoNome.isBlank()) ? novoNome : existente.nome();
        String senhaFinal = (novaSenha != null && !novaSenha.isBlank())
                ? senhaCodificador.codificar(novaSenha) : existente.senhaHash();

        return usuarioRepository.save(new Usuario(id, nomeFinal, emailFinal, senhaFinal, existente.role()));
    }

    public void deletar(UsuarioLogado solicitante, String id) {
        verificarPermissao(solicitante, id);
        usuarioRepository.deleteById(buscarPorId(id).id());
    }

    private void verificarPermissao(UsuarioLogado solicitante, String alvoId) {
        if (solicitante == null || solicitante.id() == null || solicitante.id().isBlank()) {
            throw new AcessoNegadoException("Nao autenticado.");
        }
        if (!solicitante.admin() && !solicitante.id().equals(alvoId)) {
            throw new AcessoNegadoException("Sem permissao para acessar outro usuario.");
        }
    }
}
'@

Salvar 'domain\src\test\java\com\vinijoao\login\service\AuthServiceTest.java' @'
package com.vinijoao.login.service;

import com.vinijoao.login.exception.AcessoNegadoException;
import com.vinijoao.login.exception.CredenciaisInvalidasException;
import com.vinijoao.login.exception.RegraDeNegocioException;
import com.vinijoao.login.model.Token;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.UsuarioLogado;
import com.vinijoao.login.model.enumerable.Role;
import com.vinijoao.login.port.SenhaCodificador;
import com.vinijoao.login.port.TokenEmissor;
import com.vinijoao.login.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    static class UsuarioRepositoryFake implements UsuarioRepository {
        private final Map<String, Usuario> store = new HashMap<>();

        @Override
        public Usuario save(Usuario usuario) {
            String id = usuario.id() != null ? usuario.id() : UUID.randomUUID().toString();
            Usuario salvo = new Usuario(id, usuario.nome(), usuario.email(), usuario.senhaHash(), usuario.role());
            store.put(id, salvo);
            store.put("email:" + usuario.email(), salvo);
            return salvo;
        }

        @Override
        public Optional<Usuario> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<Usuario> findByEmail(String email) {
            return Optional.ofNullable(store.get("email:" + email));
        }

        @Override
        public boolean existsByEmail(String email) {
            return store.containsKey("email:" + email);
        }
        @Override
        public void deleteById(String id) {
            Usuario u = store.remove(id);
            if (u != null) {
                store.remove("email:" + u.email());
            }
        }

    }

    static class SenhaCodificadorFake implements SenhaCodificador {
        @Override
        public String codificar(String senha) { return "hash:" + senha; }

        @Override
        public boolean confere(String senha, String hash) { return hash.equals("hash:" + senha); }
    }

    static class TokenEmissorFake implements TokenEmissor {
        @Override
        public Token emitir(Usuario usuario) { return new Token("token-" + usuario.email()); }
    }

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                new UsuarioRepositoryFake(),
                new SenhaCodificadorFake(),
                new TokenEmissorFake(),
                "codigo-admin-secreto"
        );
    }

    @Test
    void cadastrarUsuarioSemRole() {
        Usuario usuario = service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        assertEquals(Role.USUARIO, usuario.role());
    }

    @Test
    void cadastrarAdminComCodigoCorreto() {
        Usuario admin = service.cadastrar("Admin", "admin@email.com", "senhaAdmin", Role.ADMIN, "codigo-admin-secreto");
        assertEquals(Role.ADMIN, admin.role());
    }

    @Test
    void cadastrarAdminSemCodigoDeve403() {
        assertThrows(AcessoNegadoException.class, () ->
                service.cadastrar("Hacker", "hacker@email.com", "senha", Role.ADMIN, null));
    }

    @Test
    void emailDuplicadoDeve409() {
        service.cadastrar("Joao", "joao@email.com", "senha", null, null);
        assertThrows(RegraDeNegocioException.class, () ->
                service.cadastrar("Outro Joao", "joao@email.com", "outrasenha", null, null));
    }

    @Test
    void autenticarComCredenciaisCorretas() {
        service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        Token token = service.autenticar("joao@email.com", "senha123").token();
        assertNotNull(token.value());
    }

    @Test
    void autenticarComSenhaErradaDeve401() {
        service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        assertThrows(CredenciaisInvalidasException.class, () ->
                service.autenticar("joao@email.com", "senha-errada"));
    }

    @Test
    void autenticarEmailInexistenteDeve401() {
        assertThrows(CredenciaisInvalidasException.class, () ->
                service.autenticar("naoexiste@email.com", "senha"));
    }

    @Test
    void usuarioComumNaoPodeConsultarOutroUsuario() {
        Usuario joao = service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        Usuario maria = service.cadastrar("Maria", "maria@email.com", "senha123", null, null);
        UsuarioLogado solicitante = new UsuarioLogado(joao.id(), joao.nome(), false);
        assertThrows(AcessoNegadoException.class, () -> service.consultar(solicitante, maria.id()));
    }

    @Test
    void usuarioComumPodeConsultarASiMesmo() {
        Usuario joao = service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        UsuarioLogado solicitante = new UsuarioLogado(joao.id(), joao.nome(), false);
        assertEquals(joao.id(), service.consultar(solicitante, joao.id()).id());
    }

    @Test
    void adminPodeConsultarQualquerUsuario() {
        Usuario joao = service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        UsuarioLogado admin = new UsuarioLogado("qualquer-id", "Admin", true);
        assertEquals(joao.id(), service.consultar(admin, joao.id()).id());
    }

    @Test
    void semIdentidadeDeveNegarAcesso() {
        Usuario joao = service.cadastrar("Joao", "joao@email.com", "senha123", null, null);
        UsuarioLogado anonimo = new UsuarioLogado(null, null, false);
        assertThrows(AcessoNegadoException.class, () -> service.consultar(anonimo, joao.id()));
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\config\WebConfig.java' @'
package com.vinijoao.login.config;

import com.vinijoao.login.controller.resolver.UsuarioLogadoResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new UsuarioLogadoResolver());
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\controller\AuthController.java' @'
package com.vinijoao.login.controller;

import com.vinijoao.login.controller.adapter.AuthControllerAdapter;
import com.vinijoao.login.controller.dto.request.CadastroRequest;
import com.vinijoao.login.controller.dto.request.LoginRequest;
import com.vinijoao.login.controller.dto.response.UsuarioResponse;
import com.vinijoao.login.model.Sessao;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.UsuarioLogado;
import com.vinijoao.login.security.TokenCookieFactory;
import com.vinijoao.login.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenCookieFactory cookieFactory;

    public AuthController(AuthService authService, TokenCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    // O token vai SO no cookie HttpOnly; o corpo devolve apenas os dados do usuario.
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@Valid @RequestBody CadastroRequest request) {
        Usuario usuario = authService.cadastrar(request.nome(), request.email(), request.senha(),
                AuthControllerAdapter.paraRole(request.role()), request.codigoAdmin());
        Sessao sessao = authService.iniciarSessao(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeader(sessao.token().value()))
                .body(AuthControllerAdapter.paraResposta(sessao.usuario()));
    }

    @PostMapping("/login")
    public ResponseEntity<UsuarioResponse> login(@Valid @RequestBody LoginRequest request) {
        Sessao sessao = authService.autenticar(request.email(), request.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeader(sessao.token().value()))
                .body(AuthControllerAdapter.paraResposta(sessao.usuario()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeaderLogout())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(UsuarioLogado usuarioLogado) {
        if (usuarioLogado.id() == null || usuarioLogado.id().isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = authService.buscarPorId(usuarioLogado.id());
        return ResponseEntity.ok(AuthControllerAdapter.paraResposta(usuario));
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\controller\UsuarioController.java' @'
package com.vinijoao.login.controller;

import com.vinijoao.login.controller.adapter.AuthControllerAdapter;
import com.vinijoao.login.controller.dto.request.AtualizarUsuarioRequest;
import com.vinijoao.login.controller.dto.response.UsuarioResponse;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.UsuarioLogado;
import com.vinijoao.login.security.TokenCookieFactory;
import com.vinijoao.login.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final AuthService authService;
    private final TokenCookieFactory cookieFactory;

    public UsuarioController(AuthService authService, TokenCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(UsuarioLogado usuarioLogado, @PathVariable String id) {
        Usuario usuario = authService.consultar(usuarioLogado, id);
        return ResponseEntity.ok(AuthControllerAdapter.paraResposta(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            UsuarioLogado usuarioLogado,
            @PathVariable String id,
            @Valid @RequestBody AtualizarUsuarioRequest request
    ) {
        Usuario atualizado = authService.atualizar(
                usuarioLogado, id, request.nome(), request.email(), request.senha());
        return ResponseEntity.ok(AuthControllerAdapter.paraResposta(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(UsuarioLogado usuarioLogado, @PathVariable String id) {
        authService.deletar(usuarioLogado, id);

        if (id.equals(usuarioLogado.id())) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeaderLogout())
                    .build();
        }
        return ResponseEntity.noContent().build();
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\controller\adapter\AuthControllerAdapter.java' @'
package com.vinijoao.login.controller.adapter;

import com.vinijoao.login.controller.dto.response.UsuarioResponse;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.enumerable.Role;

public class AuthControllerAdapter {

    private AuthControllerAdapter() {}

    public static Role paraRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return null;
        return Role.valueOf(roleStr.trim().toUpperCase());
    }

    public static UsuarioResponse paraResposta(Usuario usuario) {
        return new UsuarioResponse(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.role().name()
        );
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\controller\dto\request\CadastroRequest.java' @'
package com.vinijoao.login.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "Nome e obrigatorio")
        String nome,

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres")
        String senha,

        @Pattern(regexp = "(?i)ADMIN|USUARIO", message = "Role invalida")
        String role,
        String codigoAdmin
) {
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\controller\resolver\UsuarioLogadoResolver.java' @'
package com.vinijoao.login.controller.resolver;

import com.vinijoao.login.model.UsuarioLogado;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Monta o UsuarioLogado a partir dos headers que o Gateway escreve (X-Usuario-*).
 * Fica num so lugar: os controllers apenas declaram o parametro.
 */
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UsuarioLogado.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String id = webRequest.getHeader("X-Usuario-Id");
        String role = webRequest.getHeader("X-Usuario-Role");
        String nomeCodificado = webRequest.getHeader("X-Usuario-Nome");
        // O nome vem codificado em URL (header HTTP nao aceita acento)
        String nome = nomeCodificado == null ? null : URLDecoder.decode(nomeCodificado, StandardCharsets.UTF_8);
        return new UsuarioLogado(id, nome, "ADMIN".equals(role));
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\exception\GlobalExceptionHandler.java' @'
package com.vinijoao.login.exception;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<Map<String, String>> handleCredenciais(CredenciaisInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<Map<String, String>> handleAcesso(AcessoNegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, String>> handleRegra(RegraDeNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNaoEncontrado(RecursoNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidacao(MethodArgumentNotValidException ex) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", mensagem));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenerico(Exception ex) {
        if (ex instanceof ErrorResponse resposta) {
            return ResponseEntity.status(resposta.getStatusCode()).body(Map.of("erro", "Requisicao invalida."));
        }
        log.error("Erro interno", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("erro", "Erro interno no servidor."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleCorpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("erro", "Corpo da requisicao invalido."));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String, String>> handleDuplicado(DuplicateKeyException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("erro", "E-mail ja cadastrado."));
    }
}
'@

Salvar 'spring\src\main\java\com\vinijoao\login\security\JwtTokenEmissor.java' @'
package com.vinijoao.login.security;

import com.vinijoao.login.model.Token;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.port.TokenEmissor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenEmissor implements TokenEmissor {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtTokenEmissor(@Value("${JWT_SECRET}") String secret,
                           @Value("${JWT_EXPIRACAO_MINUTOS:60}") long expiracaoMinutos) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter no minimo 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMinutos = expiracaoMinutos;
    }

    @Override
    public Token emitir(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMinutos * 60 * 1000L);
        String jwt = Jwts.builder()
                .subject(usuario.email())
                .claim("uid", usuario.id())
                .claim("nome", usuario.nome())
                .claim("roles", List.of("ROLE_" + usuario.role().name()))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
        return new Token(jwt);
    }
}
'@

Salvar 'spring\src\main\resources\application.yaml' @'
server:
  port: 8090

spring:
  application:
    name: login-service
  # Spring Boot 4: a URI do Mongo fica em spring.mongodb.uri (so auto-index-creation continua em spring.data.mongodb)
  mongodb:
    uri: ${MONGO_URI:mongodb://localhost:27017/loginDb?replicaSet=rs0}
  data:
    mongodb:
      auto-index-creation: true
  config:
    import: "optional:file:.env[.properties],optional:file:../.env[.properties]"
'@

# O token nao vai mais no corpo da resposta: apaga a classe antiga
Remove-Item 'spring\src\main\java\com\vinijoao\login\controller\dto\response\AuthResponse.java' -ErrorAction SilentlyContinue
Write-Host 'AuthResponse.java removido'
Write-Host 'Fase 1 do Login aplicada. Agora rode: C:\maven\bin\mvn.cmd clean install' -ForegroundColor Green