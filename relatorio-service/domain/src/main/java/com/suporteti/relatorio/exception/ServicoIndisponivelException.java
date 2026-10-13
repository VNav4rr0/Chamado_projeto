package com.suporteti.relatorio.exception;

public class ServicoIndisponivelException extends RuntimeException {
    public ServicoIndisponivelException(String message) {
        super(message);
    }

    public ServicoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
