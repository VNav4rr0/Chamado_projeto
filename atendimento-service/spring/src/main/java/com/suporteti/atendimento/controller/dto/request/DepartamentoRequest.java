package com.suporteti.atendimento.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record DepartamentoRequest(
        @NotBlank(message = "Nome do departamento e obrigatorio")
        String nome,
        List<String> categorias,
        Boolean ativo
) {
}
