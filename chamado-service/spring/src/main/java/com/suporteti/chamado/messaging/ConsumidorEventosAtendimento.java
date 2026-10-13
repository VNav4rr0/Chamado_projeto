package com.suporteti.chamado.messaging;

import com.suporteti.chamado.config.RabbitConfig;
import com.suporteti.chamado.model.Atribuicao;
import com.suporteti.chamado.model.ResultadoTriagem;
import com.suporteti.chamado.port.TransacaoExecutor;
import com.suporteti.chamado.service.ChamadoService;
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
public class ConsumidorEventosAtendimento {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventosAtendimento.class);

    private final ChamadoService chamadoService;
    private final RegistroMensagensProcessadas registroMensagensProcessadas;
    private final TransacaoExecutor transacaoExecutor;
    private final ObjectMapper objectMapper;

    public ConsumidorEventosAtendimento(
            ChamadoService chamadoService,
            RegistroMensagensProcessadas registroMensagensProcessadas,
            TransacaoExecutor transacaoExecutor,
            ObjectMapper objectMapper
    ) {
        this.chamadoService = chamadoService;
        this.registroMensagensProcessadas = registroMensagensProcessadas;
        this.transacaoExecutor = transacaoExecutor;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = RabbitConfig.CHAMADO_QUEUE)
    public void consumir(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        String bodyJson = new String(message.getBody(), StandardCharsets.UTF_8);

        log.info("Consumindo evento do atendimento: routingKey={}, messageId={}", routingKey, messageId);

        try {
            transacaoExecutor.executar(() -> {
                registroMensagensProcessadas.registrar(messageId);
                processarMensagem(routingKey, bodyJson);
            });
        } catch (DuplicateKeyException e) {
            log.info("Mensagem {} ja processada anteriormente no chamado-service. Ignorando duplicata.", messageId);
        }
    }

    private void processarMensagem(String routingKey, String bodyJson) {
        try {
            JsonNode node = objectMapper.readTree(bodyJson);
            switch (routingKey) {
                case "chamado.triado" -> {
                    String chamadoId = node.get("chamadoId").asText();
                    String resultado = node.get("resultado").asText();
                    String departamentoId = node.hasNonNull("departamentoId") ? node.get("departamentoId").asText() : null;
                    String departamentoNome = node.hasNonNull("departamentoNome") ? node.get("departamentoNome").asText() : null;
                    Instant prazoSla = node.hasNonNull("prazoSla") ? Instant.parse(node.get("prazoSla").asText()) : null;
                    String motivo = node.hasNonNull("motivo") ? node.get("motivo").asText() : null;

                    chamadoService.aplicarTriagem(new ResultadoTriagem(
                            chamadoId, resultado, departamentoId, departamentoNome, prazoSla, motivo
                    ));
                    log.info("Triagem aplicada ao chamado {} com resultado {}", chamadoId, resultado);
                }
                case "chamado.atribuido" -> {
                    String chamadoId = node.get("chamadoId").asText();
                    String atendenteId = node.get("atendenteId").asText();
                    String atendenteNome = node.get("atendenteNome").asText();
                    Instant atribuidoEm = node.hasNonNull("atribuidoEm") ? Instant.parse(node.get("atribuidoEm").asText()) : null;

                    chamadoService.aplicarAtribuicao(new Atribuicao(
                            chamadoId, atendenteId, atendenteNome, atribuidoEm
                    ));
                    log.info("Atribuicao aplicada ao chamado {} para o atendente {}", chamadoId, atendenteNome);
                }
                default -> log.warn("Routing key desconhecida no chamado-service: {}", routingKey);
            }
        } catch (Exception ex) {
            log.error("Erro ao processar payload do evento {}: {}", routingKey, ex.getMessage());
            throw new RuntimeException("Falha no processamento da mensagem AMQP", ex);
        }
    }
}
