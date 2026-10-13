package com.vinijoao.login.repository;

import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.repository.adapter.UsuarioRepositoryAdapter;
import com.vinijoao.login.repository.mongo.UsuarioMongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private final UsuarioMongoRepository mongo;

    public UsuarioRepositoryImpl(UsuarioMongoRepository mongo) {
        this.mongo = mongo;
    }

    @Override
    public Usuario save(Usuario usuario) {
        return UsuarioRepositoryAdapter.paraDominio(mongo.save(UsuarioRepositoryAdapter.paraOrm(usuario)));
    }

    @Override
    public Optional<Usuario> findById(String id) {
        return mongo.findById(id).map(UsuarioRepositoryAdapter::paraDominio);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        return mongo.findByEmail(email).map(UsuarioRepositoryAdapter::paraDominio);
    }

    @Override
    public boolean existsByEmail(String email) {
        return mongo.existsByEmail(email);
    }

    @Override
    public void deleteById(String id) {
        mongo.deleteById(id);
    }
}
