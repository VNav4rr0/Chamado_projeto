package com.suporteti.atendimento.model;

import java.time.Instant;

public record ChamadoRecebido(
        String chamadoId,
        String protocolo,
        String categoria,
        String prioridade,
        String titulo,
        String solicitanteId,
        Instant criadoEm
) {
}
