package com.suporteti.chamado.model;

import java.time.Instant;

public record Atribuicao(
        String chamadoId,
        String atendenteId,
        String atendenteNome,
        Instant atribuidoEm
) {}
