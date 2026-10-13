package com.suporteti.relatorio.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioMensalResponse(
        int ano,
        int mes,
        long totalConcluidos,
        BigDecimal tempoMedioAtendimentoHoras,
        BigDecimal tempoMedioPrimeiraRespostaHoras,
        BigDecimal percentualSlaCumprido,
        BigDecimal horasTotais,
        BigDecimal custoTotal,
        List<AgrupamentoResponse> porCategoria,
        List<AgrupamentoResponse> porPrioridade,
        List<AgrupamentoResponse> porAtendente
) {}
