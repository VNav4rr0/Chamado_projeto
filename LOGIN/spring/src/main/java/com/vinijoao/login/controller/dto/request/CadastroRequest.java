package com.vinijoao.login.controller.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CadastroRequest(
        @NotBlank(message = "Nome e obrigatorio")
        String nome,

        @NotBlank(message = "E-mail e obrigatorio")
        @Email(message = "E-mail invalido")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 6, message = "Senha deve ter ao menos 6 caracteres")
        String senha,

        @Pattern(regexp = "(?i)ADMIN|COMUM", message = "Role invalida")
        String role,
        String codigoAdmin
) {
}