package com.vinijoao.login.repository.mongo;

import com.vinijoao.login.repository.orm.UsuarioOrmMongo;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UsuarioMongoRepository extends MongoRepository<UsuarioOrmMongo, String> {
    Optional<UsuarioOrmMongo> findByEmail(String email);
    boolean existsByEmail(String email);
}