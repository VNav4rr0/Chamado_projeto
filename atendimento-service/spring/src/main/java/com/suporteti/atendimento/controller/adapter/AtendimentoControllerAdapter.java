package com.suporteti.atendimento.controller.adapter;

import com.suporteti.atendimento.controller.dto.response.AtendenteResponse;
import com.suporteti.atendimento.controller.dto.response.DepartamentoResponse;
import com.suporteti.atendimento.controller.dto.response.ItemFilaResponse;
import com.suporteti.atendimento.controller.dto.response.SlaResponse;
import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.Sla;

public class AtendimentoControllerAdapter {

    private AtendimentoControllerAdapter() {}

    public static ItemFilaResponse paraResposta(ItemFila item) {
        if (item == null) return null;
        return new ItemFilaResponse(
                item.id(),
                item.chamadoId(),
                item.protocolo(),
                item.categoria(),
                item.prioridade(),
                item.departamentoId(),
                item.status() != null ? item.status().name() : null,
                item.atendenteId(),
                item.prazoSla(),
                item.criadoEm(),
                item.atribuidoEm(),
                item.concluidoEm(),
                item.motivo()
        );
    }

    public static DepartamentoResponse paraResposta(Departamento dep) {
        if (dep == null) return null;
        return new DepartamentoResponse(
                dep.id(),
                dep.nome(),
                dep.categorias(),
                dep.ativo()
        );
    }

    public static AtendenteResponse paraResposta(Atendente at) {
        if (at == null) return null;
        return new AtendenteResponse(
                at.id(),
                at.nome(),
                at.departamentoId(),
                at.capacidade(),
                at.chamadosAtivos(),
                at.ativo()
        );
    }

    public static SlaResponse paraResposta(Sla sla) {
        if (sla == null) return null;
        return new SlaResponse(sla.prioridade(), sla.horas());
    }
}
