package com.suporteti.chamado.config;

import com.suporteti.chamado.repository.orm.ChamadoOrmMongo;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@Configuration
public class MongoConfig {

    private final MongoTemplate mongoTemplate;

    public MongoConfig(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
        return new MongoTransactionManager(dbFactory);
    }

    @PostConstruct
    public void criarIndices() {
        mongoTemplate.indexOps(ChamadoOrmMongo.class)
                .createIndex(new Index().on("solicitanteId", Sort.Direction.ASC));
        mongoTemplate.indexOps(ChamadoOrmMongo.class)
                .createIndex(new Index().on("status", Sort.Direction.ASC));
        mongoTemplate.indexOps(ChamadoOrmMongo.class)
                .createIndex(new Index().on("resolvidoEm", Sort.Direction.ASC));
    }
}
