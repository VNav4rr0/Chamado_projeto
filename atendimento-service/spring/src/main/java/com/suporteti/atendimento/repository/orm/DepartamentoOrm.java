package com.suporteti.atendimento.repository.orm;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "departamentos")
public record DepartamentoOrm(
        @Id String id,
        String nome,
        List<String> categorias,
        boolean ativo
) {
}
