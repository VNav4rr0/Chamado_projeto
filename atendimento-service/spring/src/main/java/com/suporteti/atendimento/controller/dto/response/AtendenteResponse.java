package com.suporteti.atendimento.controller.dto.response;

public record AtendenteResponse(
        String id,
        String nome,
        String departamentoId,
        int capacidade,
        int chamadosAtivos,
        boolean ativo
) {
}
