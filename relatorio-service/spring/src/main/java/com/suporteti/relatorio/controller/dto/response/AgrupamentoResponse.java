package com.suporteti.relatorio.controller.dto.response;

import java.math.BigDecimal;

public record AgrupamentoResponse(
        String chave,
        long quantidade,
        BigDecimal horas,
        BigDecimal custo
) {}
