package com.suporteti.atendimento.repository.orm;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "atendentes")
public record AtendenteOrm(
        @Id String id,
        String nome,
        String departamentoId,
        int capacidade,
        int chamadosAtivos,
        boolean ativo,
        @Version Long versao
) {
}
