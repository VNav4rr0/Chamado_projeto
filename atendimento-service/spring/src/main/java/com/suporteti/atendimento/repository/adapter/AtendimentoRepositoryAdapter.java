package com.suporteti.atendimento.repository.adapter;

import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.repository.orm.AtendenteOrm;
import com.suporteti.atendimento.repository.orm.DepartamentoOrm;
import com.suporteti.atendimento.repository.orm.ItemFilaOrm;
import com.suporteti.atendimento.repository.orm.SlaOrm;

public class AtendimentoRepositoryAdapter {

    private AtendimentoRepositoryAdapter() {}

    public static Departamento paraDominio(DepartamentoOrm orm) {
        if (orm == null) return null;
        return new Departamento(orm.id(), orm.nome(), orm.categorias(), orm.ativo());
    }

    public static DepartamentoOrm paraOrm(Departamento d) {
        if (d == null) return null;
        return new DepartamentoOrm(d.id(), d.nome(), d.categorias(), d.ativo());
    }

    public static Atendente paraDominio(AtendenteOrm orm) {
        if (orm == null) return null;
        return new Atendente(
                orm.id(), orm.nome(), orm.departamentoId(),
                orm.capacidade(), orm.chamadosAtivos(), orm.ativo()
        );
    }

    public static AtendenteOrm paraOrm(Atendente a) {
        if (a == null) return null;
        return new AtendenteOrm(
                a.id(), a.nome(), a.departamentoId(),
                a.capacidade(), a.chamadosAtivos(), a.ativo(), null
        );
    }

    public static ItemFila paraDominio(ItemFilaOrm orm) {
        if (orm == null) return null;
        return new ItemFila(
                orm.id(), orm.chamadoId(), orm.protocolo(), orm.categoria(),
                orm.prioridade(), orm.departamentoId(), orm.status(),
                orm.atendenteId(), orm.prazoSla(), orm.criadoEm(),
                orm.atribuidoEm(), orm.concluidoEm(), orm.motivo(), orm.versao()
        );
    }

    public static ItemFilaOrm paraOrm(ItemFila i) {
        if (i == null) return null;
        return new ItemFilaOrm(
                i.id(), i.chamadoId(), i.protocolo(), i.categoria(),
                i.prioridade(), i.departamentoId(), i.status(),
                i.atendenteId(), i.prazoSla(), i.criadoEm(),
                i.atribuidoEm(), i.concluidoEm(), i.motivo(), i.versao()
        );
    }

    public static Sla paraDominio(SlaOrm orm) {
        if (orm == null) return null;
        return new Sla(orm.prioridade(), orm.horas());
    }

    public static SlaOrm paraOrm(Sla s) {
        if (s == null) return null;
        return new SlaOrm(s.prioridade(), s.horas());
    }
}
