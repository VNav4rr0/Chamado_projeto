package com.suporteti.atendimento.messaging;

import com.suporteti.atendimento.config.RabbitConfig;
import com.suporteti.atendimento.model.ChamadoRecebido;
import com.suporteti.atendimento.port.TransacaoExecutor;
import com.suporteti.atendimento.service.FilaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class ConsumidorEventosChamado {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventosChamado.class);

    private final FilaService filaService;
    private final RegistroMensagensProcessadas registroMensagensProcessadas;
    private final TransacaoExecutor transacaoExecutor;
    private final ObjectMapper objectMapper;

    public ConsumidorEventosChamado(
            FilaService filaService,
            RegistroMensagensProcessadas registroMensagensProcessadas,
            TransacaoExecutor transacaoExecutor,
            ObjectMapper objectMapper
    ) {
        this.filaService = filaService;
        this.registroMensagensProcessadas = registroMensagensProcessadas;
        this.transacaoExecutor = transacaoExecutor;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.ATENDIMENTO_QUEUE)
    public void consumir(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String bodyJson = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("Consumindo evento do chamado: routingKey={}, messageId={}", routingKey, messageId);

        try {
            transacaoExecutor.executar(() -> {
                registroMensagensProcessadas.registrar(messageId);
                processarMensagem(routingKey, bodyJson);
            });
        } catch (DuplicateKeyException e) {
            log.info("Mensagem {} ja processada anteriormente no atendimento-service. Ignorando duplicata.", messageId);
        }
    }

    private void processarMensagem(String routingKey, String bodyJson) {
        try {
            JsonNode node = objectMapper.readTree(bodyJson);
            switch (routingKey) {
                case "chamado.criado" -> {
                    String chamadoId = node.get("chamadoId").asText();
                    String protocolo = node.get("protocolo").asText();
                    String categoria = node.get("categoria").asText();
                    String prioridade = node.get("prioridade").asText();
                    String titulo = node.get("titulo").asText();
                    String solicitanteId = node.get("solicitanteId").asText();
                    Instant criadoEm = Instant.parse(node.get("criadoEm").asText());

                    filaService.triar(new ChamadoRecebido(
                            chamadoId, protocolo, categoria, prioridade, titulo, solicitanteId, criadoEm
                    ));
                    log.info("Chamado {} triado com sucesso pelo atendimento-service", chamadoId);
                }
                case "chamado.resolvido", "chamado.rejeitado" -> {
                    String chamadoId = node.get("chamadoId").asText();
                    String atendenteId = node.hasNonNull("atendenteId") ? node.get("atendenteId").asText() : null;
                    filaService.concluir(chamadoId, atendenteId);
                    log.info("Item da fila do chamado {} concluido apos {}", chamadoId, routingKey);
                }
                default -> log.warn("Routing key desconhecida no atendimento-service: {}", routingKey);
            }
        } catch (Exception ex) {
            log.error("Erro ao processar payload do evento {}: {}", routingKey, ex.getMessage());
            throw new RuntimeException("Falha no processamento da mensagem AMQP", ex);
        }
    }
}
