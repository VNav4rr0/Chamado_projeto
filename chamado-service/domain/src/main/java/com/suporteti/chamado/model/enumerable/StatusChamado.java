package com.suporteti.chamado.model.enumerable;

import java.util.Set;

public enum StatusChamado {
    CRIADO,
    EM_ANALISE,
    EM_ANDAMENTO,
    AGUARDANDO_TERCEIRO,
    RESOLVIDO,
    FECHADO,
    REJEITADO;

    public boolean podeIrPara(StatusChamado novo) {
        if (novo == null) {
            return false;
        }
        return switch (this) {
            case CRIADO -> novo == EM_ANALISE || novo == REJEITADO;
            case EM_ANALISE -> novo == EM_ANDAMENTO;
            case EM_ANDAMENTO -> novo == AGUARDANDO_TERCEIRO || novo == RESOLVIDO || novo == REJEITADO;
            case AGUARDANDO_TERCEIRO -> novo == EM_ANDAMENTO || novo == RESOLVIDO;
            case RESOLVIDO -> novo == FECHADO;
            case FECHADO, REJEITADO -> false;
        };
    }
}
