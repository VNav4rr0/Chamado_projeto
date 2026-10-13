package com.suporteti.atendimento.service;

import com.suporteti.atendimento.exception.RecursoNaoEncontradoException;
import com.suporteti.atendimento.exception.RegraDeNegocioException;
import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.repository.AtendenteRepository;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.SlaRepository;

import java.util.List;

public class CadastroService {

    private final DepartamentoRepository departamentoRepository;
    private final AtendenteRepository atendenteRepository;
    private final SlaRepository slaRepository;

    public CadastroService(
            DepartamentoRepository departamentoRepository,
            AtendenteRepository atendenteRepository,
            SlaRepository slaRepository
    ) {
        this.departamentoRepository = departamentoRepository;
        this.atendenteRepository = atendenteRepository;
        this.slaRepository = slaRepository;
    }

    // --- Departamentos ---

    public Departamento criarDepartamento(String nome, List<String> categorias) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("Nome do departamento e obrigatorio.");
        }
        Departamento departamento = new Departamento(null, nome.trim(), categorias, true);
        return departamentoRepository.save(departamento);
    }

    public Departamento atualizarDepartamento(String id, String nome, List<String> categorias, Boolean ativo) {
        Departamento existente = buscarDepartamentoPorId(id);
        String nomeFinal = (nome != null && !nome.isBlank()) ? nome.trim() : existente.nome();
        List<String> categoriasFinal = categorias != null ? categorias : existente.categorias();
        boolean ativoFinal = ativo != null ? ativo : existente.ativo();

        Departamento atualizado = new Departamento(id, nomeFinal, categoriasFinal, ativoFinal);
        return departamentoRepository.save(atualizado);
    }

    public List<Departamento> listarDepartamentos() {
        return departamentoRepository.listar();
    }

    public Departamento buscarDepartamentoPorId(String id) {
        return departamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Departamento nao encontrado."));
    }

    public List<String> listarCategorias() {
        return departamentoRepository.listar().stream()
                .filter(Departamento::ativo)
                .flatMap(d -> d.categorias().stream())
                .distinct()
                .sorted()
                .toList();
    }

    // --- Atendentes ---

    public Atendente criarAtendente(String id, String nome, String departamentoId, int capacidade) {
        if (id == null || id.isBlank()) {
            throw new RegraDeNegocioException("Id do atendente e obrigatorio.");
        }
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("Nome do atendente e obrigatorio.");
        }
        buscarDepartamentoPorId(departamentoId);

        if (capacidade <= 0) {
            throw new RegraDeNegocioException("Capacidade do atendente deve ser maior que zero.");
        }

        Atendente atendente = new Atendente(id, nome.trim(), departamentoId, capacidade, 0, true);
        return atendenteRepository.save(atendente);
    }

    public Atendente atualizarAtendente(String id, String nome, String departamentoId, Integer capacidade, Boolean ativo) {
        Atendente existente = buscarAtendentePorId(id);

        String nomeFinal = (nome != null && !nome.isBlank()) ? nome.trim() : existente.nome();
        String depFinal = existente.departamentoId();
        if (departamentoId != null && !departamentoId.isBlank()) {
            buscarDepartamentoPorId(departamentoId);
            depFinal = departamentoId;
        }
        int capFinal = existente.capacidade();
        if (capacidade != null) {
            if (capacidade < existente.chamadosAtivos()) {
                throw new RegraDeNegocioException("Capacidade nao pode ser menor que o numero de chamados ativos.");
            }
            capFinal = capacidade;
        }
        boolean ativoFinal = ativo != null ? ativo : existente.ativo();

        Atendente atualizado = new Atendente(id, nomeFinal, depFinal, capFinal, existente.chamadosAtivos(), ativoFinal);
        return atendenteRepository.save(atualizado);
    }

    public List<Atendente> listarAtendentes(String departamentoId) {
        return atendenteRepository.listar(departamentoId);
    }

    public Atendente buscarAtendentePorId(String id) {
        return atendenteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendente nao encontrado."));
    }

    // --- SLAs ---

    public Sla salvarSla(String prioridade, int horas) {
        if (prioridade == null || prioridade.isBlank()) {
            throw new RegraDeNegocioException("Prioridade e obrigatoria.");
        }
        if (horas <= 0) {
            throw new RegraDeNegocioException("Horas de SLA devem ser maiores que zero.");
        }
        Sla sla = new Sla(prioridade.trim().toUpperCase(), horas);
        return slaRepository.save(sla);
    }

    public List<Sla> listarSlas() {
        return slaRepository.listar();
    }

    public Sla buscarSlaPorPrioridade(String prioridade) {
        if (prioridade == null || prioridade.isBlank()) {
            throw new RegraDeNegocioException("Prioridade e obrigatoria.");
        }
        return slaRepository.findByPrioridade(prioridade.trim().toUpperCase())
                .orElseThrow(() -> new RecursoNaoEncontradoException("SLA nao encontrado para prioridade: " + prioridade));
    }
}
