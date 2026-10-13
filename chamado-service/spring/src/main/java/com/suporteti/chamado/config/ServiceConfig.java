package com.suporteti.chamado.config;

import com.suporteti.chamado.port.GeradorProtocolo;
import com.suporteti.chamado.port.PublicadorEventos;
import com.suporteti.chamado.port.TransacaoExecutor;
import com.suporteti.chamado.repository.ChamadoRepository;
import com.suporteti.chamado.service.ChamadoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ServiceConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }

    @Bean
    public ChamadoService chamadoService(
            ChamadoRepository chamadoRepository,
            PublicadorEventos publicadorEventos,
            TransacaoExecutor transacaoExecutor,
            GeradorProtocolo geradorProtocolo,
            Clock clock
    ) {
        return new ChamadoService(
                chamadoRepository,
                publicadorEventos,
                transacaoExecutor,
                geradorProtocolo,
                clock
        );
    }
}
