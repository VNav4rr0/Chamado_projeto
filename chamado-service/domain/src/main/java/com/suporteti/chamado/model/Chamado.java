package com.suporteti.chamado.model;

import com.suporteti.chamado.model.enumerable.Prioridade;
import com.suporteti.chamado.model.enumerable.StatusChamado;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record Chamado(
        String id,
        String protocolo,
        String titulo,
        String descricao,
        String categoria,
        Prioridade prioridade,
        StatusChamado status,
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
        List<MudancaStatus> historico,
        Long versao
) {
    public Chamado {
        historico = historico == null ? List.of() : List.copyOf(historico);
    }

    public Chamado comStatus(
            StatusChamado novoStatus,
            String usuarioId,
            Instant em,
            BigDecimal novasHoras,
            BigDecimal novoCusto,
            String novoMotivo,
            String observacao
    ) {
        List<MudancaStatus> novoHistorico = new ArrayList<>(this.historico);
        novoHistorico.add(new MudancaStatus(this.status, novoStatus, usuarioId, em, observacao));

        Instant novoResolvidoEm = novoStatus == StatusChamado.RESOLVIDO ? em : this.resolvidoEm;
        Instant novoFechadoEm = novoStatus == StatusChamado.FECHADO ? em : this.fechadoEm;
        BigDecimal hTrabalhadas = novasHoras != null ? novasHoras : this.horasTrabalhadas;
        BigDecimal cFinal = novoCusto != null ? novoCusto : this.custo;
        String mFinal = novoMotivo != null ? novoMotivo : this.motivo;

        return new Chamado(
                this.id,
                this.protocolo,
                this.titulo,
                this.descricao,
                this.categoria,
                this.prioridade,
                novoStatus,
                this.solicitanteId,
                this.solicitanteNome,
                this.departamentoId,
                this.departamentoNome,
                this.atendenteId,
                this.atendenteNome,
                this.prazoSla,
                this.criadoEm,
                this.analisadoEm,
                this.atribuidoEm,
                novoResolvidoEm,
                novoFechadoEm,
                hTrabalhadas,
                cFinal,
                mFinal,
                novoHistorico,
                this.versao
        );
    }

    public Chamado comTriagem(ResultadoTriagem triagem, Instant em) {
        boolean aceito = "ACEITO".equalsIgnoreCase(triagem.resultado());
        StatusChamado novoStatus = aceito ? StatusChamado.EM_ANALISE : StatusChamado.REJEITADO;
        String obs = aceito ? "Triagem aceita: " + triagem.departamentoNome() : "Triagem rejeitada: " + triagem.motivo();

        List<MudancaStatus> novoHistorico = new ArrayList<>(this.historico);
        novoHistorico.add(new MudancaStatus(this.status, novoStatus, "SISTEMA", em, obs));

        return new Chamado(
                this.id,
                this.protocolo,
                this.titulo,
                this.descricao,
                this.categoria,
                this.prioridade,
                novoStatus,
                this.solicitanteId,
                this.solicitanteNome,
                triagem.departamentoId(),
                triagem.departamentoNome(),
                this.atendenteId,
                this.atendenteNome,
                triagem.prazoSla(),
                this.criadoEm,
                em,
                this.atribuidoEm,
                this.resolvidoEm,
                this.fechadoEm,
                this.horasTrabalhadas,
                this.custo,
                aceito ? this.motivo : triagem.motivo(),
                novoHistorico,
                this.versao
        );
    }

    public Chamado comAtribuicao(Atribuicao atribuicao, Instant em) {
        List<MudancaStatus> novoHistorico = new ArrayList<>(this.historico);
        novoHistorico.add(new MudancaStatus(
                this.status,
                StatusChamado.EM_ANDAMENTO,
                atribuicao.atendenteId(),
                em,
                "Atribuido para " + atribuicao.atendenteNome()
        ));

        Instant dataAtribuicao = atribuicao.atribuidoEm() != null ? atribuicao.atribuidoEm() : em;

        return new Chamado(
                this.id,
                this.protocolo,
                this.titulo,
                this.descricao,
                this.categoria,
                this.prioridade,
                StatusChamado.EM_ANDAMENTO,
                this.solicitanteId,
                this.solicitanteNome,
                this.departamentoId,
                this.departamentoNome,
                atribuicao.atendenteId(),
                atribuicao.atendenteNome(),
                this.prazoSla,
                this.criadoEm,
                this.analisadoEm,
                dataAtribuicao,
                this.resolvidoEm,
                this.fechadoEm,
                this.horasTrabalhadas,
                this.custo,
                this.motivo,
                novoHistorico,
                this.versao
        );
    }
}
