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