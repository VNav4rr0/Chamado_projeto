package com.suporteti.chamado.port;

import java.time.Instant;

public interface GeradorProtocolo {
    String proximo(Instant agora);
}
