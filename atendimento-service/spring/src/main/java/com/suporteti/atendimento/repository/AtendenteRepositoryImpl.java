package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.repository.adapter.AtendimentoRepositoryAdapter;
import com.suporteti.atendimento.repository.mongo.AtendenteMongoRepository;
import com.suporteti.atendimento.repository.orm.AtendenteOrm;
import org.bson.Document;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AtendenteRepositoryImpl implements AtendenteRepository {

    private final AtendenteMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    public AtendenteRepositoryImpl(AtendenteMongoRepository mongoRepository, MongoTemplate mongoTemplate) {
        this.mongoRepository = mongoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Atendente save(Atendente atendente) {
        AtendenteOrm orm = AtendimentoRepositoryAdapter.paraOrm(atendente);
        return AtendimentoRepositoryAdapter.paraDominio(mongoRepository.save(orm));
    }

    @Override
    public Optional<Atendente> findById(String id) {
        return mongoRepository.findById(id).map(AtendimentoRepositoryAdapter::paraDominio);
    }

    @Override
    public List<Atendente> listar(String departamentoId) {
        if (departamentoId != null && !departamentoId.isBlank()) {
            return mongoRepository.findByDepartamentoId(departamentoId).stream()
                    .map(AtendimentoRepositoryAdapter::paraDominio)
                    .toList();
        }
        return mongoRepository.findAll().stream()
                .map(AtendimentoRepositoryAdapter::paraDominio)
                .toList();
    }

    @Override
    public boolean reservarCapacidade(String atendenteId) {
        Query query = Query.query(
                Criteria.where("_id").is(atendenteId)
                        .and("ativo").is(true)
                        .and("$expr").is(new Document("$lt", List.of("$chamadosAtivos", "$capacidade")))
        );
        Update update = new Update().inc("chamadosAtivos", 1);

        AtendenteOrm modificado = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), AtendenteOrm.class
        );
        return modificado != null;
    }

    @Override
    public void liberarCapacidade(String atendenteId) {
        Query query = Query.query(Criteria.where("_id").is(atendenteId).and("chamadosAtivos").gt(0));
        Update update = new Update().inc("chamadosAtivos", -1);
        mongoTemplate.updateFirst(query, update, AtendenteOrm.class);
    }
}
