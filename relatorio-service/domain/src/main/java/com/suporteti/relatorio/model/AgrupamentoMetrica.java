package com.suporteti.relatorio.model;

import java.math.BigDecimal;

public record AgrupamentoMetrica(
        String chave,
        long quantidade,
        BigDecimal horas,
        BigDecimal custo
) {}
