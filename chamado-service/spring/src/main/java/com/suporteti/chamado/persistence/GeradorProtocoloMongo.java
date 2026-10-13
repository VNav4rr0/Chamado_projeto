package com.suporteti.chamado.persistence;

import com.suporteti.chamado.port.GeradorProtocolo;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;

@Component
public class GeradorProtocoloMongo implements GeradorProtocolo {

    private final MongoTemplate mongoTemplate;

    public GeradorProtocoloMongo(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String proximo(Instant agora) {
        int ano = agora != null
                ? agora.atZone(ZoneId.of("America/Sao_Paulo")).getYear()
                : 2026;

        String contadorId = "protocolo" + ano;
        Query query = new Query(Criteria.where("_id").is(contadorId));
        Update update = new Update().inc("valor", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        ContadorOrm contador = mongoTemplate.findAndModify(query, update, options, ContadorOrm.class);
        long valor = contador != null ? contador.getValor() : 1L;

        return String.format("CH-%d-%06d", ano, valor);
    }
}
