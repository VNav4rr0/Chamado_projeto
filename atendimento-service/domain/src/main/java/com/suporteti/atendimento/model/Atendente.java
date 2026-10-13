package com.suporteti.atendimento.model;

public record Atendente(
        String id,
        String nome,
        String departamentoId,
        int capacidade,
        int chamadosAtivos,
        boolean ativo
) {
    public boolean podeAtender() {
        return ativo && chamadosAtivos < capacidade;
    }

    public Atendente comIncrementoAtivos() {
        return new Atendente(id, nome, departamentoId, capacidade, chamadosAtivos + 1, ativo);
    }

    public Atendente comDecrementoAtivos() {
        int novosAtivos = Math.max(0, chamadosAtivos - 1);
        return new Atendente(id, nome, departamentoId, capacidade, novosAtivos, ativo);
    }
}
