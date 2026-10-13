package com.vinijoao.login.controller;

import com.vinijoao.login.controller.adapter.AuthControllerAdapter;
import com.vinijoao.login.controller.dto.request.AtualizarUsuarioRequest;
import com.vinijoao.login.controller.dto.response.UsuarioResponse;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.enumerable.Role;
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

    private static boolean ehAdmin(String role) {
        return Role.ADMIN.name().equals(role);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscar(
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioLogadoId,
            @RequestHeader(value = "X-Usuario-Role", required = false) String usuarioLogadoRole,
            @PathVariable String id
    ) {
        Usuario usuario = authService.consultar(usuarioLogadoId, ehAdmin(usuarioLogadoRole), id);
        return ResponseEntity.ok(AuthControllerAdapter.paraResposta(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioLogadoId,
            @RequestHeader(value = "X-Usuario-Role", required = false) String usuarioLogadoRole,
            @PathVariable String id,
            @Valid @RequestBody AtualizarUsuarioRequest request
    ) {
        Usuario atualizado = authService.atualizar(
                usuarioLogadoId, ehAdmin(usuarioLogadoRole), id,
                request.nome(), request.email(), request.senha());
        return ResponseEntity.ok(AuthControllerAdapter.paraResposta(atualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(
            @RequestHeader(value = "X-Usuario-Id", required = false) String usuarioLogadoId,
            @RequestHeader(value = "X-Usuario-Role", required = false) String usuarioLogadoRole,
            @PathVariable String id
    ) {
        authService.deletar(usuarioLogadoId, ehAdmin(usuarioLogadoRole), id);

        if (id.equals(usuarioLogadoId)) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.SET_COOKIE, cookieFactory.cookieHeaderLogout())
                    .build();
        }
        return ResponseEntity.noContent().build();
    }
}