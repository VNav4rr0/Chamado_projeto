package com.suporteti.chamado.messaging;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "mensagensOutbox")
public record MensagemOutboxOrm(
        @Id String id,
        String chaveRoteamento,
        String payload,
        String status,
        Instant criadoEm
) {}
