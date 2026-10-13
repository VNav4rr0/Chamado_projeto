package com.suporteti.relatorio.config;

import com.suporteti.relatorio.port.ChamadosClient;
import com.suporteti.relatorio.service.RelatorioService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class ServiceConfig {

    @Bean
    public ZoneId zoneId() {
        return ZoneId.of("America/Sao_Paulo");
    }

    @Bean
    public RelatorioService relatorioService(ChamadosClient chamadosClient, ZoneId zoneId) {
        return new RelatorioService(chamadosClient, zoneId);
    }
}
