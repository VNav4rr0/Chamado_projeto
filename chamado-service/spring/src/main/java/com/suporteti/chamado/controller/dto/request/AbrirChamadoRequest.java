package com.suporteti.chamado.controller.dto.request;

import com.suporteti.chamado.model.enumerable.Prioridade;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AbrirChamadoRequest(
        @NotBlank(message = "Titulo e obrigatorio.")
        String titulo,

        @NotBlank(message = "Descricao e obrigatoria.")
        String descricao,

        @NotBlank(message = "Categoria e obrigatoria.")
        String categoria,

        @NotNull(message = "Prioridade e obrigatoria.")
        Prioridade prioridade
) {}
