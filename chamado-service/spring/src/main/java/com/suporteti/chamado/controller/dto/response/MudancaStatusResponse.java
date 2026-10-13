package com.suporteti.chamado.controller.dto.response;

import java.time.Instant;

public record MudancaStatusResponse(
        String de,
        String para,
        String porUsuarioId,
        Instant em,
        String observacao
) {}
