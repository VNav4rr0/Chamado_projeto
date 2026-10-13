package com.suporteti.atendimento.model;

import com.suporteti.atendimento.model.enumerable.StatusItemFila;

import java.time.Instant;

public record ItemFila(
        String id,
        String chamadoId,
        String protocolo,
        String categoria,
        String prioridade,
        String departamentoId,
        StatusItemFila status,
        String atendenteId,
        Instant prazoSla,
        Instant criadoEm,
        Instant atribuidoEm,
        Instant concluidoEm,
        String motivo,
        Long versao
) {
    public ItemFila comAtribuicao(String novoAtendenteId, Instant instante) {
        return new ItemFila(
                id, chamadoId, protocolo, categoria, prioridade, departamentoId,
                StatusItemFila.ATRIBUIDO, novoAtendenteId, prazoSla, criadoEm,
                instante, concluidoEm, motivo, versao
        );
    }

    public ItemFila comConclusao(Instant instante) {
        return new ItemFila(
                id, chamadoId, protocolo, categoria, prioridade, departamentoId,
                StatusItemFila.CONCLUIDO, atendenteId, prazoSla, criadoEm,
                atribuidoEm, instante, motivo, versao
        );
    }
}
