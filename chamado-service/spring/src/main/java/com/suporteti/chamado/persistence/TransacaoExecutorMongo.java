package com.suporteti.chamado.persistence;

import com.mongodb.MongoException;
import com.suporteti.chamado.exception.RegraDeNegocioException;
import com.suporteti.chamado.port.TransacaoExecutor;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

@Component
public class TransacaoExecutorMongo implements TransacaoExecutor {

    private final TransactionTemplate transactionTemplate;

    public TransacaoExecutorMongo(MongoTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T executar(Supplier<T> trabalho) {
        int tentativas = 0;
        int maxTentativas = 3;
        while (true) {
            tentativas++;
            try {
                return transactionTemplate.execute(status -> trabalho.get());
            } catch (Exception ex) {
                if (isTransientTransactionError(ex)) {
                    if (tentativas < maxTentativas) {
                        try {
                            int jitter = ThreadLocalRandom.current().nextInt(20, 80);
                            Thread.sleep((50L * tentativas) + jitter);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw ex;
                        }
                        continue;
                    }
                    throw new RegraDeNegocioException("Conflito concorrente na operacao. Tente novamente.");
                }
                throw ex;
            }
        }
    }

    @Override
    public void executar(Runnable trabalho) {
        executar(() -> {
            trabalho.run();
            return null;
        });
    }

    private boolean isTransientTransactionError(Throwable ex) {
        Throwable atual = ex;
        while (atual != null) {
            if (atual instanceof MongoException mongoEx) {
                if (mongoEx.hasErrorLabel(MongoException.TRANSIENT_TRANSACTION_ERROR_LABEL)) {
                    return true;
                }
            }
            atual = atual.getCause();
        }
        return false;
    }
}
