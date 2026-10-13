package com.vinijoao.login.repository;

import com.vinijoao.login.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository {
    Usuario save(Usuario usuario);
    Optional<Usuario> findById(String id);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    void deleteById(String id);
}
