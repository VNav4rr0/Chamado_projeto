package com.suporteti.chamado.controller.adapter;

import com.suporteti.chamado.controller.dto.response.ChamadoResponse;
import com.suporteti.chamado.controller.dto.response.MudancaStatusResponse;
import com.suporteti.chamado.controller.dto.response.PaginaResponse;
import com.suporteti.chamado.model.Chamado;
import com.suporteti.chamado.model.Pagina;

import java.util.List;

public final class ChamadoControllerAdapter {

    private ChamadoControllerAdapter() {}

    public static ChamadoResponse paraResponse(Chamado chamado) {
        if (chamado == null) {
            return null;
        }

        List<MudancaStatusResponse> historico = chamado.historico() == null ? List.of() :
                chamado.historico().stream()
                        .map(m -> new MudancaStatusResponse(
                                m.de() != null ? m.de().name() : null,
                                m.para().name(),
                                m.porUsuarioId(),
                                m.em(),
                                m.observacao()
                        ))
                        .toList();

        return new ChamadoResponse(
                chamado.id(),
                chamado.protocolo(),
                chamado.titulo(),
                chamado.descricao(),
                chamado.categoria(),
                chamado.prioridade() != null ? chamado.prioridade().name() : null,
                chamado.status() != null ? chamado.status().name() : null,
                chamado.solicitanteId(),
                chamado.solicitanteNome(),
                chamado.departamentoId(),
                chamado.departamentoNome(),
                chamado.atendenteId(),
                chamado.atendenteNome(),
                chamado.prazoSla(),
                chamado.criadoEm(),
                chamado.analisadoEm(),
                chamado.atribuidoEm(),
                chamado.resolvidoEm(),
                chamado.fechadoEm(),
                chamado.horasTrabalhadas(),
                chamado.custo(),
                chamado.motivo(),
                historico
        );
    }

    public static PaginaResponse<ChamadoResponse> paraPaginaResponse(Pagina<Chamado> pagina) {
        if (pagina == null) {
            return new PaginaResponse<>(List.of(), 0, 0, 10);
        }
        List<ChamadoResponse> itens = pagina.itens().stream()
                .map(ChamadoControllerAdapter::paraResponse)
                .toList();

        return new PaginaResponse<>(itens, pagina.total(), pagina.pagina(), pagina.tamanho());
    }
}
