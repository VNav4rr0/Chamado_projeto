package com.suporteti.relatorio.client;

import java.util.List;

public record PaginaDto(
        List<ChamadoConcluidoDto> itens,
        long total,
        int pagina,
        int tamanho
) {}
