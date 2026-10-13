package com.suporteti.atendimento.controller.dto.request;

import jakarta.validation.constraints.Min;

public record SlaRequest(
        @Min(value = 1, message = "Horas de SLA devem ser no minimo 1")
        int horas
) {
}
