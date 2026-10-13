package com.suporteti.chamado.controller.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ChamadoResponse(
        String id,
        String protocolo,
        String titulo,
        String descricao,
        String categoria,
        String prioridade,
        String status,
        String solicitanteId,
        String solicitanteNome,
        String departamentoId,
        String departamentoNome,
        String atendenteId,
        String atendenteNome,
        Instant prazoSla,
        Instant criadoEm,
        Instant analisadoEm,
        Instant atribuidoEm,
        Instant resolvidoEm,
        Instant fechadoEm,
        BigDecimal horasTrabalhadas,
        BigDecimal custo,
        String motivo,
        List<MudancaStatusResponse> historico
) {}
