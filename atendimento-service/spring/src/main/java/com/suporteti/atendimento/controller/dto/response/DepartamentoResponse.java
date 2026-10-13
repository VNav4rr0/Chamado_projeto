package com.suporteti.atendimento.controller.dto.response;

import java.util.List;

public record DepartamentoResponse(
        String id,
        String nome,
        List<String> categorias,
        boolean ativo
) {
}
