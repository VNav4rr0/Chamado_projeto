package com.suporteti.atendimento.repository.orm;

import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "filaAtendimento")
public record ItemFilaOrm(
        @Id String id,
        @Indexed(unique = true) String chamadoId,
        String protocolo,
        String categoria,
        String prioridade,
        String departamentoId,
        StatusItemFila status,
        String atendenteId,
        Instant prazoSla,
        Instant criadoEm,
        Instant atribuidoEm,
        Instant concluidoEm,
        String motivo,
        @Version Long versao
) {
}
