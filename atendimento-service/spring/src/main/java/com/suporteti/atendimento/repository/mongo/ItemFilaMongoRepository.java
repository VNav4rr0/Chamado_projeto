package com.suporteti.atendimento.repository.mongo;

import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import com.suporteti.atendimento.repository.orm.ItemFilaOrm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ItemFilaMongoRepository extends MongoRepository<ItemFilaOrm, String> {
    Optional<ItemFilaOrm> findByChamadoId(String chamadoId);
    List<ItemFilaOrm> findByDepartamentoIdAndStatus(String departamentoId, StatusItemFila status);
    List<ItemFilaOrm> findByDepartamentoId(String departamentoId);
    List<ItemFilaOrm> findByStatus(StatusItemFila status);
}
