package com.suporteti.chamado.model;

import java.time.Instant;

public record ResultadoTriagem(
        String chamadoId,
        String resultado,
        String departamentoId,
        String departamentoNome,
        Instant prazoSla,
        String motivo
) {}
