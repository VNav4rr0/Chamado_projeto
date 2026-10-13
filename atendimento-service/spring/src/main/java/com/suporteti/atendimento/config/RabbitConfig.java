package com.suporteti.atendimento.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String CHAMADOS_EXCHANGE = "chamadosExchange";
    public static final String ATENDIMENTO_EXCHANGE = "atendimentoExchange";

    public static final String ATENDIMENTO_QUEUE = "atendimentoEventosChamadoQueue";
    public static final String ATENDIMENTO_DLQ = "atendimentoEventosChamadoDlq";

    @Bean
    public TopicExchange chamadosExchange() {
        return new TopicExchange(CHAMADOS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange atendimentoExchange() {
        return new TopicExchange(ATENDIMENTO_EXCHANGE, true, false);
    }

    @Bean
    public Queue atendimentoQueue() {
        return QueueBuilder.durable(ATENDIMENTO_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", CHAMADOS_EXCHANGE,
                        "x-dead-letter-routing-key", ATENDIMENTO_DLQ
                ))
                .build();
    }

    @Bean
    public Queue atendimentoDlq() {
        return QueueBuilder.durable(ATENDIMENTO_DLQ).build();
    }

    @Bean
    public Binding bindingChamadoCriado(Queue atendimentoQueue, TopicExchange chamadosExchange) {
        return BindingBuilder.bind(atendimentoQueue).to(chamadosExchange).with("chamado.criado");
    }

    @Bean
    public Binding bindingChamadoResolvido(Queue atendimentoQueue, TopicExchange chamadosExchange) {
        return BindingBuilder.bind(atendimentoQueue).to(chamadosExchange).with("chamado.resolvido");
    }

    @Bean
    public Binding bindingChamadoRejeitado(Queue atendimentoQueue, TopicExchange chamadosExchange) {
        return BindingBuilder.bind(atendimentoQueue).to(chamadosExchange).with("chamado.rejeitado");
    }

    @Bean
    public Binding bindingAtendimentoDlq(Queue atendimentoDlq, TopicExchange chamadosExchange) {
        return BindingBuilder.bind(atendimentoDlq).to(chamadosExchange).with(ATENDIMENTO_DLQ);
    }
}
