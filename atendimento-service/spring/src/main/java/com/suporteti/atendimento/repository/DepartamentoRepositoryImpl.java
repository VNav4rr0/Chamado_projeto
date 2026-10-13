package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.repository.adapter.AtendimentoRepositoryAdapter;
import com.suporteti.atendimento.repository.mongo.DepartamentoMongoRepository;
import com.suporteti.atendimento.repository.orm.DepartamentoOrm;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class DepartamentoRepositoryImpl implements DepartamentoRepository {

    private final DepartamentoMongoRepository mongoRepository;

    public DepartamentoRepositoryImpl(DepartamentoMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Departamento save(Departamento departamento) {
        DepartamentoOrm orm = AtendimentoRepositoryAdapter.paraOrm(departamento);
        return AtendimentoRepositoryAdapter.paraDominio(mongoRepository.save(orm));
    }

    @Override
    public Optional<Departamento> findById(String id) {
        return mongoRepository.findById(id).map(AtendimentoRepositoryAdapter::paraDominio);
    }

    @Override
    public List<Departamento> listar() {
        return mongoRepository.findAll().stream()
                .map(AtendimentoRepositoryAdapter::paraDominio)
                .toList();
    }

    @Override
    public Optional<Departamento> buscarPorCategoria(String categoria) {
        return mongoRepository.findByCategoriasContaining(categoria)
                .map(AtendimentoRepositoryAdapter::paraDominio);
    }
}
