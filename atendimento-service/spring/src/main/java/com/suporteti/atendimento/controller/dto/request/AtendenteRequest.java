package com.suporteti.atendimento.controller.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AtendenteRequest(
        String id,
        @NotBlank(message = "Nome do atendente e obrigatorio")
        String nome,
        @NotBlank(message = "Departamento e obrigatorio")
        String departamentoId,
        Integer capacidade,
        Boolean ativo
) {
}
