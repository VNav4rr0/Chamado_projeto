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