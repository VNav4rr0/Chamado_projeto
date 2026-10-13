package com.suporteti.chamado.model;

import java.util.List;

public record Pagina<T>(
        List<T> itens,
        long total,
        int pagina,
        int tamanho
) {
    public Pagina {
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
