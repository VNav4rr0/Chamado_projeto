package com.suporteti.atendimento.port;

import com.suporteti.atendimento.event.EventoSaida;

public interface PublicadorEventos {
    void publicar(EventoSaida evento);
}
