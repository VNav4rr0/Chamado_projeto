package com.suporteti.chamado.repository.orm;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Document(collection = "chamados")
public record ChamadoOrmMongo(
        @Id String id,
        String protocolo,
        String titulo,
        String descricao,
        String categoria,
        String prioridade,
        String status,
        String solicitanteId,
        String solicitanteNome,
        String departamentoId,
        String departamentoNome,
        String atendenteId,
        String atendenteNome,
        Instant prazoSla,
        Instant criadoEm,
        Instant analisadoEm,
        Instant atribuidoEm,
        Instant resolvidoEm,
        Instant fechadoEm,
        BigDecimal horasTrabalhadas,
        BigDecimal custo,
        String motivo,
        List<MudancaStatusOrmMongo> historico,
        @Version Long versao
) {
    public record MudancaStatusOrmMongo(
            String de,
            String para,
            String porUsuarioId,
            Instant em,
            String observacao
    ) {}
}
