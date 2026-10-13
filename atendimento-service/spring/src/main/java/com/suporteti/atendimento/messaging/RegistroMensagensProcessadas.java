package com.suporteti.atendimento.messaging;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RegistroMensagensProcessadas {

    private final MongoTemplate mongoTemplate;

    public RegistroMensagensProcessadas(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public void registrar(String messageId) {
        if (messageId == null || messageId.isBlank()) {
            return;
        }
        mongoTemplate.insert(new MensagemProcessadaOrm(messageId, Instant.now()));
    }
}
