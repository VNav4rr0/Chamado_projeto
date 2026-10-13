package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.repository.adapter.AtendimentoRepositoryAdapter;
import com.suporteti.atendimento.repository.mongo.SlaMongoRepository;
import com.suporteti.atendimento.repository.orm.SlaOrm;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SlaRepositoryImpl implements SlaRepository {

    private final SlaMongoRepository mongoRepository;

    public SlaRepositoryImpl(SlaMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Sla save(Sla sla) {
        SlaOrm orm = AtendimentoRepositoryAdapter.paraOrm(sla);
        return AtendimentoRepositoryAdapter.paraDominio(mongoRepository.save(orm));
    }

    @Override
    public Optional<Sla> findByPrioridade(String prioridade) {
        return mongoRepository.findById(prioridade.toUpperCase())
                .map(AtendimentoRepositoryAdapter::paraDominio);
    }

    @Override
    public List<Sla> listar() {
        return mongoRepository.findAll().stream()
                .map(AtendimentoRepositoryAdapter::paraDominio)
                .toList();
    }
}
