package com.vinijoao.login.controller.dto.response;

public record AuthResponse(
        String token,
        UsuarioResponse usuario
) {
}