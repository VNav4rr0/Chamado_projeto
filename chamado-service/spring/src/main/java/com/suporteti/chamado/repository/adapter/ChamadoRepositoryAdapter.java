package com.suporteti.chamado.repository.adapter;

import com.suporteti.chamado.model.Chamado;
import com.suporteti.chamado.model.MudancaStatus;
import com.suporteti.chamado.model.enumerable.Prioridade;
import com.suporteti.chamado.model.enumerable.StatusChamado;
import com.suporteti.chamado.repository.orm.ChamadoOrmMongo;

import java.util.List;

public final class ChamadoRepositoryAdapter {

    private ChamadoRepositoryAdapter() {}

    public static Chamado paraDominio(ChamadoOrmMongo orm) {
        if (orm == null) {
            return null;
        }

        List<MudancaStatus> historico = orm.historico() == null ? List.of() :
                orm.historico().stream()
                        .map(m -> new MudancaStatus(
                                m.de() != null ? StatusChamado.valueOf(m.de()) : null,
                                StatusChamado.valueOf(m.para()),
                                m.porUsuarioId(),
                                m.em(),
                                m.observacao()
                        ))
                        .toList();

        return new Chamado(
                orm.id(),
                orm.protocolo(),
                orm.titulo(),
                orm.descricao(),
                orm.categoria(),
                orm.prioridade() != null ? Prioridade.valueOf(orm.prioridade()) : null,
                orm.status() != null ? StatusChamado.valueOf(orm.status()) : null,
                orm.solicitanteId(),
                orm.solicitanteNome(),
                orm.departamentoId(),
                orm.departamentoNome(),
                orm.atendenteId(),
                orm.atendenteNome(),
                orm.prazoSla(),
                orm.criadoEm(),
                orm.analisadoEm(),
                orm.atribuidoEm(),
                orm.resolvidoEm(),
                orm.fechadoEm(),
                orm.horasTrabalhadas(),
                orm.custo(),
                orm.motivo(),
                historico,
                orm.versao()
        );
    }

    public static ChamadoOrmMongo paraOrm(Chamado domain) {
        if (domain == null) {
            return null;
        }

        List<ChamadoOrmMongo.MudancaStatusOrmMongo> historicoOrm = domain.historico() == null ? List.of() :
                domain.historico().stream()
                        .map(m -> new ChamadoOrmMongo.MudancaStatusOrmMongo(
                                m.de() != null ? m.de().name() : null,
                                m.para() != null ? m.para().name() : null,
                                m.porUsuarioId(),
                                m.em(),
                                m.observacao()
                        ))
                        .toList();

        return new ChamadoOrmMongo(
                domain.id(),
                domain.protocolo(),
                domain.titulo(),
                domain.descricao(),
                domain.categoria(),
                domain.prioridade() != null ? domain.prioridade().name() : null,
                domain.status() != null ? domain.status().name() : null,
                domain.solicitanteId(),
                domain.solicitanteNome(),
                domain.departamentoId(),
                domain.departamentoNome(),
                domain.atendenteId(),
                domain.atendenteNome(),
                domain.prazoSla(),
                domain.criadoEm(),
                domain.analisadoEm(),
                domain.atribuidoEm(),
                domain.resolvidoEm(),
                domain.fechadoEm(),
                domain.horasTrabalhadas(),
                domain.custo(),
                domain.motivo(),
                historicoOrm,
                domain.versao()
        );
    }
}
