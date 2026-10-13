package com.suporteti.atendimento.model;

import java.util.List;

public record Departamento(
        String id,
        String nome,
        List<String> categorias,
        boolean ativo
) {
    public Departamento {
        categorias = categorias != null ? List.copyOf(categorias) : List.of();
    }
}
