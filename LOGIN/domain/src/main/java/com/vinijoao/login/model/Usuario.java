package com.vinijoao.login.model;

import com.vinijoao.login.model.enumerable.Role;

public record Usuario(
        String id,
        String nome,
        String email,
        String senhaHash,
        Role role
) {
}