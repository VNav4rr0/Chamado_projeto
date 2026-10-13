package com.suporteti.chamado.model;

import com.suporteti.chamado.model.enumerable.StatusChamado;

import java.time.Instant;
import java.util.List;

public record FiltroChamados(
        String solicitanteId,
        List<StatusChamado> status,
        String atendenteId,
        Instant resolvidoDe,
        Instant resolvidoAte,
        int pagina,
        int tamanho
) {
    public FiltroChamados {
        if (pagina < 0) {
            pagina = 0;
        }
        if (tamanho <= 0) {
            tamanho = 10;
        }
    }

    public FiltroChamados comSolicitanteId(String novoSolicitanteId) {
        return new FiltroChamados(
                novoSolicitanteId,
                status,
                atendenteId,
                resolvidoDe,
                resolvidoAte,
                pagina,
                tamanho
        );
    }
}
