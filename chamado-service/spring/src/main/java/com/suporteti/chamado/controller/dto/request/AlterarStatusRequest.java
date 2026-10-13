package com.suporteti.chamado.controller.dto.request;

import com.suporteti.chamado.model.enumerable.StatusChamado;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AlterarStatusRequest(
        @NotNull(message = "Status e obrigatorio.")
        StatusChamado status,

        BigDecimal horasTrabalhadas,
        BigDecimal custo,
        String motivo
) {}
