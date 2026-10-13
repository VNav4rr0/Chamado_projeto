package com.suporteti.atendimento.messaging;

import com.suporteti.atendimento.event.EventoSaida;
import com.suporteti.atendimento.port.PublicadorEventos;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Component
public class PublicadorEventosOutbox implements PublicadorEventos {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    public PublicadorEventosOutbox(MongoTemplate mongoTemplate, ObjectMapper objectMapper) {
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publicar(EventoSaida evento) {
        try {
            String payloadJson = objectMapper.writeValueAsString(evento);
            MensagemOutboxOrm mensagem = new MensagemOutboxOrm(
                    UUID.randomUUID().toString(),
                    evento.chaveRoteamento(),
                    payloadJson,
                    "PENDENTE",
                    Instant.now()
            );
            mongoTemplate.save(mensagem);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar evento outbox para JSON.", e);
        }
    }
}
