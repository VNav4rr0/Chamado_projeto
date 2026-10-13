package com.suporteti.relatorio.model;

import java.math.BigDecimal;
import java.util.List;

public record RelatorioMensal(
        int ano,
        int mes,
        long totalConcluidos,
        BigDecimal tempoMedioAtendimentoHoras,
        BigDecimal tempoMedioPrimeiraRespostaHoras,
        BigDecimal percentualSlaCumprido,
        BigDecimal horasTotais,
        BigDecimal custoTotal,
        List<AgrupamentoMetrica> porCategoria,
        List<AgrupamentoMetrica> porPrioridade,
        List<AgrupamentoMetrica> porAtendente
) {
    public RelatorioMensal {
        porCategoria = porCategoria == null ? List.of() : List.copyOf(porCategoria);
        porPrioridade = porPrioridade == null ? List.of() : List.copyOf(porPrioridade);
        porAtendente = porAtendente == null ? List.of() : List.copyOf(porAtendente);
    }
}
