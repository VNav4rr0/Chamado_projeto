package com.suporteti.atendimento.repository.mongo;

import com.suporteti.atendimento.repository.orm.DepartamentoOrm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface DepartamentoMongoRepository extends MongoRepository<DepartamentoOrm, String> {
    Optional<DepartamentoOrm> findByCategoriasContaining(String categoria);
}
