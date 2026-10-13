package com.vinijoao.login.controller.adapter;

import com.vinijoao.login.controller.dto.response.AuthResponse;
import com.vinijoao.login.controller.dto.response.UsuarioResponse;
import com.vinijoao.login.model.Sessao;
import com.vinijoao.login.model.Token;
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

    public static AuthResponse paraAuthResponse(Sessao sessao) {
        return new AuthResponse(sessao.token().value(), paraResposta(sessao.usuario()));
    }
}