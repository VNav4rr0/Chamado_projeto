package com.suporteti.chamado.event;

import java.time.Instant;

public sealed interface EventoSaida permits
        EventoSaida.ChamadoCriado,
        EventoSaida.ChamadoResolvido,
        EventoSaida.ChamadoRejeitado {

    String chaveRoteamento();

    record ChamadoCriado(
            String chamadoId,
            String protocolo,
            String categoria,
            String prioridade,
            String titulo,
            String solicitanteId,
            Instant criadoEm
    ) implements EventoSaida {
        @Override
        public String chaveRoteamento() {
            return "chamado.criado";
        }
    }

    record ChamadoResolvido(
            String chamadoId,
            String atendenteId,
            Instant resolvidoEm
    ) implements EventoSaida {
        @Override
        public String chaveRoteamento() {
            return "chamado.resolvido";
        }
    }

    record ChamadoRejeitado(
            String chamadoId,
            String atendenteId,
            String motivo
    ) implements EventoSaida {
        @Override
        public String chaveRoteamento() {
            return "chamado.rejeitado";
        }
    }
}
