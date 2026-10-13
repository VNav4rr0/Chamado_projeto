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