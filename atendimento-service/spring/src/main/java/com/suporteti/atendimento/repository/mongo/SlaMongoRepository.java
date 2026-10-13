package com.suporteti.atendimento.repository.mongo;

import com.suporteti.atendimento.repository.orm.SlaOrm;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SlaMongoRepository extends MongoRepository<SlaOrm, String> {
}
