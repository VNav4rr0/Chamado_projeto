package com.suporteti.chamado.repository.mongo;

import com.suporteti.chamado.repository.orm.ChamadoOrmMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChamadoMongoRepository extends MongoRepository<ChamadoOrmMongo, String> {
}
