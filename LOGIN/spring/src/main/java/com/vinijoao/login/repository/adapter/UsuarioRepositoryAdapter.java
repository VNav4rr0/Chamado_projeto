package com.vinijoao.login.repository.adapter;

import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.model.enumerable.Role;
import com.vinijoao.login.repository.orm.UsuarioOrmMongo;

public class UsuarioRepositoryAdapter {

    private UsuarioRepositoryAdapter() {}

    public static Usuario paraDominio(UsuarioOrmMongo orm) {
        return new Usuario(
                orm.id(),
                orm.nome(),
                orm.email(),
                orm.senhaHash(),
                Role.valueOf(orm.role())
        );
    }

    public static UsuarioOrmMongo paraOrm(Usuario usuario) {
        return new UsuarioOrmMongo(
                usuario.id(),
                usuario.nome(),
                usuario.email(),
                usuario.senhaHash(),
                usuario.role().name()
        );
    }
}