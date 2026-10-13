package com.suporteti.atendimento.config;

import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.SlaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DadosIniciais implements CommandLineRunner {

    private final DepartamentoRepository departamentoRepository;
    private final SlaRepository slaRepository;

    public DadosIniciais(DepartamentoRepository departamentoRepository, SlaRepository slaRepository) {
        this.departamentoRepository = departamentoRepository;
        this.slaRepository = slaRepository;
    }

    @Override
    public void run(String... args) {
        carregarDepartamentos();
        carregarSlas();
    }

    private void carregarDepartamentos() {
        if (departamentoRepository.listar().isEmpty()) {
            departamentoRepository.save(new Departamento(null, "Infraestrutura", List.of("Infraestrutura"), true));
            departamentoRepository.save(new Departamento(null, "Redes", List.of("Redes"), true));
            departamentoRepository.save(new Departamento(null, "Software", List.of("Software"), true));
            departamentoRepository.save(new Departamento(null, "Acesso e Contas", List.of("AcessoContas"), true));
            departamentoRepository.save(new Departamento(null, "Hardware", List.of("Hardware"), true));
        }
    }

    private void carregarSlas() {
        if (slaRepository.listar().isEmpty()) {
            slaRepository.save(new Sla("BAIXA", 72));
            slaRepository.save(new Sla("MEDIA", 24));
            slaRepository.save(new Sla("ALTA", 8));
            slaRepository.save(new Sla("CRITICA", 2));
        }
    }
}
