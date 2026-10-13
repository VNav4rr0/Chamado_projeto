package com.suporteti.atendimento.messaging;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "mensagensProcessadas")
public record MensagemProcessadaOrm(
        @Id String id,
        Instant processadoEm
) {}
