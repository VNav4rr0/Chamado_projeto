package com.suporteti.relatorio.client;

import java.math.BigDecimal;
import java.time.Instant;

public record ChamadoConcluidoDto(
        String id,
        String protocolo,
        String titulo,
        String categoria,
        String prioridade,
        String status,
        String departamentoNome,
        String atendenteId,
        String atendenteNome,
        Instant prazoSla,
        Instant criadoEm,
        Instant atribuidoEm,
        Instant resolvidoEm,
        BigDecimal horasTrabalhadas,
        BigDecimal custo
) {}
