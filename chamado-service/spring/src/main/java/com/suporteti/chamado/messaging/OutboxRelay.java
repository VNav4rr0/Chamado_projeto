package com.suporteti.chamado.messaging;

import com.suporteti.chamado.config.RabbitConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final MongoTemplate mongoTemplate;
    private final RabbitTemplate rabbitTemplate;

    public OutboxRelay(MongoTemplate mongoTemplate, RabbitTemplate rabbitTemplate) {
        this.mongoTemplate = mongoTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 1000)
    public void processarMensagensPendentes() {
        try {
            Query query = new Query(Criteria.where("status").is("PENDENTE"))
                    .with(Sort.by(Sort.Direction.ASC, "criadoEm"))
                    .limit(50);

            List<MensagemOutboxOrm> pendentes = mongoTemplate.find(query, MensagemOutboxOrm.class);
            if (pendentes.isEmpty()) {
                return;
            }

            for (MensagemOutboxOrm msg : pendentes) {
                try {
                    MessageProperties props = new MessageProperties();
                    props.setMessageId(msg.id());
                    props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    if (msg.criadoEm() != null) {
                        props.setHeader("timestamp", msg.criadoEm().toString());
                    }

                    Message amqpMessage = new Message(msg.payload().getBytes(StandardCharsets.UTF_8), props);
                    CorrelationData correlationData = new CorrelationData(msg.id());

                    rabbitTemplate.send(RabbitConfig.CHAMADOS_EXCHANGE, msg.chaveRoteamento(), amqpMessage, correlationData);

                    CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);
                    if (confirm != null && confirm.ack()) {
                        mongoTemplate.updateFirst(
                                Query.query(Criteria.where("_id").is(msg.id())),
                                Update.update("status", "ENVIADA"),
                                MensagemOutboxOrm.class
                        );
                        log.debug("Mensagem outbox {} enviada com sucesso", msg.id());
                    } else {
                        log.warn("Nao houve confirm de envio para mensagem outbox {}", msg.id());
                    }
                } catch (Exception ex) {
                    log.error("Erro ao enviar mensagem outbox {}: {}", msg.id(), ex.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Falha ao consultar outbox no MongoDB: {}", e.getMessage());
        }
    }
}
