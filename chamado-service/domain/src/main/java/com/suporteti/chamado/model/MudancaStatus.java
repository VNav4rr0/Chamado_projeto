package com.suporteti.chamado.model;

import com.suporteti.chamado.model.enumerable.StatusChamado;

import java.time.Instant;

public record MudancaStatus(
        StatusChamado de,
        StatusChamado para,
        String porUsuarioId,
        Instant em,
        String observacao
) {}
