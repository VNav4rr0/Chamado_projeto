package com.suporteti.atendimento.controller.dto.response;

import java.time.Instant;

public record ItemFilaResponse(
        String id,
        String chamadoId,
        String protocolo,
        String categoria,
        String prioridade,
        String departamentoId,
        String status,
        String atendenteId,
        Instant prazoSla,
        Instant criadoEm,
        Instant atribuidoEm,
        Instant concluidoEm,
        String motivo
) {
}
