package com.vinijoao.login.controller;

import com.vinijoao.login.controller.adapter.AuthControllerAdapter;
import com.vinijoao.login.controller.dto.request.CadastroRequest;
import com.vinijoao.login.controller.dto.request.LoginRequest;
import com.vinijoao.login.controller.dto.response.AuthResponse;
import com.vinijoao.login.controller.dto.response.UsuarioResponse;
import com.vinijoao.login.model.Sessao;
import com.vinijoao.login.model.Token;
import com.vinijoao.login.model.Usuario;
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

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody CadastroRequest request) {
        Usuario usuario = authService.cadastrar(request.nome(), request.email(), request.senha(),
                AuthControllerAdapter.paraRole(request.role()), request.codigoAdmin());
        Sessao sessao = authService.iniciarSessao(usuario);
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeader(sessao.token().value()))
                .body(AuthControllerAdapter.paraAuthResponse(sessao));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        Sessao sessao = authService.autenticar(request.email(), request.senha());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeader(sessao.token().value()))
                .body(AuthControllerAdapter.paraAuthResponse(sessao));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeaderLogout())
                .build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioId
    ) {
        if (usuarioId == null || usuarioId.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Usuario usuario = authService.buscarPorId(usuarioId);
        return ResponseEntity.ok(AuthControllerAdapter.paraResposta(usuario));
    }
}