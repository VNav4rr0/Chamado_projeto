package com.suporteti.atendimento.config;

import com.suporteti.atendimento.port.PublicadorEventos;
import com.suporteti.atendimento.port.TransacaoExecutor;
import com.suporteti.atendimento.repository.AtendenteRepository;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.FilaRepository;
import com.suporteti.atendimento.repository.SlaRepository;
import com.suporteti.atendimento.service.CadastroService;
import com.suporteti.atendimento.service.FilaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public CadastroService cadastroService(
            DepartamentoRepository departamentoRepository,
            AtendenteRepository atendenteRepository,
            SlaRepository slaRepository
    ) {
        return new CadastroService(departamentoRepository, atendenteRepository, slaRepository);
    }

    @Bean
    public FilaService filaService(
            FilaRepository filaRepository,
            AtendenteRepository atendenteRepository,
            DepartamentoRepository departamentoRepository,
            SlaRepository slaRepository,
            PublicadorEventos publicadorEventos,
            TransacaoExecutor transacaoExecutor
    ) {
        return new FilaService(
                filaRepository,
                atendenteRepository,
                departamentoRepository,
                slaRepository,
                publicadorEventos,
                transacaoExecutor
        );
    }
}
