package com.suporteti.chamado.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String CHAMADOS_EXCHANGE = "chamadosExchange";
    public static final String ATENDIMENTO_EXCHANGE = "atendimentoExchange";

    public static final String CHAMADO_QUEUE = "chamadoEventosAtendimentoQueue";
    public static final String CHAMADO_DLQ = "chamadoEventosAtendimentoDlq";

    @Bean
    public TopicExchange chamadosExchange() {
        return new TopicExchange(CHAMADOS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange atendimentoExchange() {
        return new TopicExchange(ATENDIMENTO_EXCHANGE, true, false);
    }

    @Bean
    public Queue chamadoQueue() {
        return QueueBuilder.durable(CHAMADO_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", ATENDIMENTO_EXCHANGE,
                        "x-dead-letter-routing-key", CHAMADO_DLQ
                ))
                .build();
    }

    @Bean
    public Queue chamadoDlq() {
        return QueueBuilder.durable(CHAMADO_DLQ).build();
    }

    @Bean
    public Binding bindingChamadoTriado(Queue chamadoQueue, TopicExchange atendimentoExchange) {
        return BindingBuilder.bind(chamadoQueue).to(atendimentoExchange).with("chamado.triado");
    }

    @Bean
    public Binding bindingChamadoAtribuido(Queue chamadoQueue, TopicExchange atendimentoExchange) {
        return BindingBuilder.bind(chamadoQueue).to(atendimentoExchange).with("chamado.atribuido");
    }

    @Bean
    public Binding bindingChamadoDlq(Queue chamadoDlq, TopicExchange atendimentoExchange) {
        return BindingBuilder.bind(chamadoDlq).to(atendimentoExchange).with(CHAMADO_DLQ);
    }
}
