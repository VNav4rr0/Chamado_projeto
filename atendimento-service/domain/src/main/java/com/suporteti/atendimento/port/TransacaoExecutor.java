package com.suporteti.atendimento.port;

import java.util.function.Supplier;

public interface TransacaoExecutor {
    <T> T executar(Supplier<T> trabalho);
    void executar(Runnable trabalho);
}
