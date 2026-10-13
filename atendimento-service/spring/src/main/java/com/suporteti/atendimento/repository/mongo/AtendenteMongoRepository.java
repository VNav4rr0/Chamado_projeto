package com.suporteti.atendimento.repository.mongo;

import com.suporteti.atendimento.repository.orm.AtendenteOrm;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AtendenteMongoRepository extends MongoRepository<AtendenteOrm, String> {
    List<AtendenteOrm> findByDepartamentoId(String departamentoId);
}
