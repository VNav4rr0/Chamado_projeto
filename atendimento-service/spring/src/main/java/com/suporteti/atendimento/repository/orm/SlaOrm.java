package com.suporteti.atendimento.repository.orm;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "slas")
public record SlaOrm(
        @Id String prioridade,
        int horas
) {
}
