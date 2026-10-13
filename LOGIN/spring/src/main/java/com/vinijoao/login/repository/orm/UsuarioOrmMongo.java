package com.vinijoao.login.repository.orm;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "usuarios")
public record UsuarioOrmMongo(
        @Id String id,
        String nome,
        @Indexed(unique = true) String email,
        String senhaHash,
        String role
) {
}