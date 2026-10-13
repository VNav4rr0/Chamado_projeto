package com.vinijoao.login.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record AtualizarUsuarioRequest(
        String nome,

        @Email(message = "E-mail invalido")
        String email,

        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres")
        String senha
) {
}
