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