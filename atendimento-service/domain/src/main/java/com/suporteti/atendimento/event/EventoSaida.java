package com.suporteti.atendimento.event;

import java.time.Instant;

public sealed interface EventoSaida permits EventoSaida.ChamadoTriado, EventoSaida.ChamadoAtribuido {

    String chaveRoteamento();

    record ChamadoTriado(
            String chamadoId,
            String resultado,
            String departamentoId,
            String departamentoNome,
            Instant prazoSla,
            String motivo
    ) implements EventoSaida {
        @Override
        public String chaveRoteamento() {
            return "chamado.triado";
        }
    }

    record ChamadoAtribuido(
            String chamadoId,
            String atendenteId,
            String atendenteNome,
            Instant atribuidoEm
    ) implements EventoSaida {
        @Override
        public String chaveRoteamento() {
            return "chamado.atribuido";
        }
    }
}
