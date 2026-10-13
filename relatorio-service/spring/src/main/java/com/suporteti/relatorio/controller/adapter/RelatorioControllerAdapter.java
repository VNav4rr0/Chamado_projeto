package com.suporteti.relatorio.controller.adapter;

import com.suporteti.relatorio.controller.dto.response.AgrupamentoResponse;
import com.suporteti.relatorio.controller.dto.response.RelatorioMensalResponse;
import com.suporteti.relatorio.model.RelatorioMensal;

import java.util.List;

public final class RelatorioControllerAdapter {

    private RelatorioControllerAdapter() {}

    public static RelatorioMensalResponse paraResponse(RelatorioMensal domain) {
        if (domain == null) {
            return null;
        }

        List<AgrupamentoResponse> categorias = domain.porCategoria().stream()
                .map(a -> new AgrupamentoResponse(a.chave(), a.quantidade(), a.horas(), a.custo()))
                .toList();

        List<AgrupamentoResponse> prioridades = domain.porPrioridade().stream()
                .map(a -> new AgrupamentoResponse(a.chave(), a.quantidade(), a.horas(), a.custo()))
                .toList();

        List<AgrupamentoResponse> atendentes = domain.porAtendente().stream()
                .map(a -> new AgrupamentoResponse(a.chave(), a.quantidade(), a.horas(), a.custo()))
                .toList();

        return new RelatorioMensalResponse(
                domain.ano(),
                domain.mes(),
                domain.totalConcluidos(),
                domain.tempoMedioAtendimentoHoras(),
                domain.tempoMedioPrimeiraRespostaHoras(),
                domain.percentualSlaCumprido(),
                domain.horasTotais(),
                domain.custoTotal(),
                categorias,
                prioridades,
                atendentes
        );
    }
}
