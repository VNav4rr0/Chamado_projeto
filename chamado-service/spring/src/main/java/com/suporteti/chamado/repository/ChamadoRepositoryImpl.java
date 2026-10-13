package com.suporteti.chamado.repository;

import com.suporteti.chamado.model.Chamado;
import com.suporteti.chamado.model.FiltroChamados;
import com.suporteti.chamado.model.Pagina;
import com.suporteti.chamado.repository.adapter.ChamadoRepositoryAdapter;
import com.suporteti.chamado.repository.mongo.ChamadoMongoRepository;
import com.suporteti.chamado.repository.orm.ChamadoOrmMongo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ChamadoRepositoryImpl implements ChamadoRepository {

    private final ChamadoMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    public ChamadoRepositoryImpl(ChamadoMongoRepository mongoRepository, MongoTemplate mongoTemplate) {
        this.mongoRepository = mongoRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Chamado save(Chamado chamado) {
        ChamadoOrmMongo orm = ChamadoRepositoryAdapter.paraOrm(chamado);
        ChamadoOrmMongo salvo = mongoRepository.save(orm);
        return ChamadoRepositoryAdapter.paraDominio(salvo);
    }

    @Override
    public Optional<Chamado> findById(String id) {
        return mongoRepository.findById(id).map(ChamadoRepositoryAdapter::paraDominio);
    }

    @Override
    public Pagina<Chamado> buscar(FiltroChamados filtro) {
        List<Criteria> criterias = new ArrayList<>();

        if (filtro.solicitanteId() != null && !filtro.solicitanteId().isBlank()) {
            criterias.add(Criteria.where("solicitanteId").is(filtro.solicitanteId()));
        }
        if (filtro.status() != null && !filtro.status().isEmpty()) {
            List<String> statusNames = filtro.status().stream().map(Enum::name).toList();
            criterias.add(Criteria.where("status").in(statusNames));
        }
        if (filtro.atendenteId() != null && !filtro.atendenteId().isBlank()) {
            criterias.add(Criteria.where("atendenteId").is(filtro.atendenteId()));
        }
        if (filtro.resolvidoDe() != null && filtro.resolvidoAte() != null) {
            criterias.add(Criteria.where("resolvidoEm").gte(filtro.resolvidoDe()).lte(filtro.resolvidoAte()));
        } else if (filtro.resolvidoDe() != null) {
            criterias.add(Criteria.where("resolvidoEm").gte(filtro.resolvidoDe()));
        } else if (filtro.resolvidoAte() != null) {
            criterias.add(Criteria.where("resolvidoEm").lte(filtro.resolvidoAte()));
        }

        Query query = new Query();
        if (!criterias.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criterias.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, ChamadoOrmMongo.class);

        Pageable pageable = PageRequest.of(
                filtro.pagina(),
                filtro.tamanho(),
                Sort.by(Sort.Direction.DESC, "criadoEm")
        );

        List<ChamadoOrmMongo> documentos = mongoTemplate.find(query.with(pageable), ChamadoOrmMongo.class);
        List<Chamado> itens = documentos.stream().map(ChamadoRepositoryAdapter::paraDominio).toList();

        return new Pagina<>(itens, total, filtro.pagina(), filtro.tamanho());
    }
}
