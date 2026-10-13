package com.vinijoao.login.controller.dto.response;

public record UsuarioResponse(
        String id,
        String nome,
        String email,
        String role
) {
}