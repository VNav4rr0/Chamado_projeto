package com.suporteti.relatorio.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ChamadoConcluido(
        String protocolo,
        String categoria,
        String prioridade,
        String departamentoNome,
        String atendenteId,
        String atendenteNome,
        Instant criadoEm,
        Instant atribuidoEm,
        Instant resolvidoEm,
        Instant prazoSla,
        BigDecimal horasTrabalhadas,
        BigDecimal custo
) {}
