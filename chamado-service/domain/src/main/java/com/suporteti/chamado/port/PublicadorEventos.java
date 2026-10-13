package com.suporteti.chamado.port;

import com.suporteti.chamado.event.EventoSaida;

public interface PublicadorEventos {
    void publicar(EventoSaida evento);
}
