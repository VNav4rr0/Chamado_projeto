package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import com.suporteti.atendimento.repository.adapter.AtendimentoRepositoryAdapter;
import com.suporteti.atendimento.repository.mongo.ItemFilaMongoRepository;
import com.suporteti.atendimento.repository.orm.ItemFilaOrm;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class FilaRepositoryImpl implements FilaRepository {

    private final ItemFilaMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    public FilaRepositoryImpl(ItemFilaMongoRepository mongoRepository, MongoTemplate mongoTemplate) {
        this.mongoRepository = mongoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ItemFila save(ItemFila item) {
        ItemFilaOrm orm = AtendimentoRepositoryAdapter.paraOrm(item);
        return AtendimentoRepositoryAdapter.paraDominio(mongoRepository.save(orm));
    }

    @Override
    public Optional<ItemFila> buscarPorChamadoId(String chamadoId) {
        return mongoRepository.findByChamadoId(chamadoId)
                .map(AtendimentoRepositoryAdapter::paraDominio);
    }

    @Override
    public List<ItemFila> listar(String departamentoId, StatusItemFila status) {
        if (departamentoId != null && !departamentoId.isBlank() && status != null) {
            return mongoRepository.findByDepartamentoIdAndStatus(departamentoId, status).stream()
                    .map(AtendimentoRepositoryAdapter::paraDominio)
                    .toList();
        }
        if (departamentoId != null && !departamentoId.isBlank()) {
            return mongoRepository.findByDepartamentoId(departamentoId).stream()
                    .map(AtendimentoRepositoryAdapter::paraDominio)
                    .toList();
        }
        if (status != null) {
            return mongoRepository.findByStatus(status).stream()
                    .map(AtendimentoRepositoryAdapter::paraDominio)
                    .toList();
        }
        return mongoRepository.findAll().stream()
                .map(AtendimentoRepositoryAdapter::paraDominio)
                .toList();
    }

    @Override
    public Optional<ItemFila> reivindicar(String chamadoId, String departamentoId, String atendenteId, Instant instante) {
        Query query = Query.query(
                Criteria.where("chamadoId").is(chamadoId)
                        .and("departamentoId").is(departamentoId)
                        .and("status").is(StatusItemFila.NA_FILA)
        );
        Update update = new Update()
                .set("status", StatusItemFila.ATRIBUIDO)
                .set("atendenteId", atendenteId)
                .set("atribuidoEm", instante);

        ItemFilaOrm modificado = mongoTemplate.findAndModify(
                query, update, FindAndModifyOptions.options().returnNew(true), ItemFilaOrm.class
        );
        return Optional.ofNullable(modificado).map(AtendimentoRepositoryAdapter::paraDominio);
    }

    @Override
    public void concluir(String chamadoId, Instant instante) {
        Query query = Query.query(Criteria.where("chamadoId").is(chamadoId));
        Update update = new Update()
                .set("status", StatusItemFila.CONCLUIDO)
                .set("concluidoEm", instante);
        mongoTemplate.updateFirst(query, update, ItemFilaOrm.class);
    }
}
