package com.suporteti.atendimento.model;

public record UsuarioLogado(
        String id,
        String nome,
        boolean admin
) {
}
