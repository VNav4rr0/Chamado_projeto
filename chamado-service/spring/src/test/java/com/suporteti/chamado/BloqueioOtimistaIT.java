package com.suporteti.chamado;

import com.suporteti.chamado.exception.GlobalExceptionHandler;
import com.suporteti.chamado.model.Chamado;
import com.suporteti.chamado.model.MudancaStatus;
import com.suporteti.chamado.model.enumerable.Prioridade;
import com.suporteti.chamado.model.enumerable.StatusChamado;
import com.suporteti.chamado.repository.ChamadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class BloqueioOtimistaIT {

    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7");

    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("MONGO_URI", mongoDBContainer::getReplicaSetUrl);
    }

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void limparBanco() {
        mongoTemplate.getDb().drop();
    }

    @Test
    void deveLancarOptimisticLockingFailureExceptionEConverterPara409() {
        Instant agora = Instant.now();
        Chamado inicial = new Chamado(
                null, "CH-2026-000100", "Problema no Monitor", "Tela preta", "Hardware",
                Prioridade.MEDIA, StatusChamado.CRIADO, "usr-teste", "Usuario Teste",
                null, null, "ate-teste", "Tecnico Teste", null, agora, null, null, null, null,
                null, null, null, List.of(), null
        );

        // 1. Salva o registro inicial no banco (criando versao 0)
        Chamado salvo = chamadoRepository.save(inicial);
        assertNotNull(salvo.id());
        assertEquals(0L, salvo.versao());

        // 2. Duas threads simulam ler o chamado ao mesmo tempo (ambas veem versao 0)
        Chamado instanciaThread1 = chamadoRepository.findById(salvo.id()).orElseThrow();
        Chamado instanciaThread2 = chamadoRepository.findById(salvo.id()).orElseThrow();

        // 3. Thread 1 altera o chamado e salva primeiro com sucesso (incrementa para versao 1)
        Chamado atualizadoThread1 = instanciaThread1.comStatus(
                StatusChamado.EM_ANALISE, "sistema", agora, null, null, null, "Triagem"
        );
        Chamado salvoThread1 = chamadoRepository.save(atualizadoThread1);
        assertEquals(1L, salvoThread1.versao());

        // 4. Thread 2 tenta salvar a instancia antiga que ainda estava com versao 0
        Chamado atualizadoThread2 = instanciaThread2.comStatus(
                StatusChamado.REJEITADO, "sistema", agora, null, null, "Negado", "Rejeicao"
        );

        OptimisticLockingFailureException ex = assertThrows(
                OptimisticLockingFailureException.class,
                () -> chamadoRepository.save(atualizadoThread2),
                "O Mongo deve recusar o update da versao desatualizada com OptimisticLockingFailureException"
        );

        // 5. Verifica se o GlobalExceptionHandler traduz para HTTP 409 com mensagem amigável ao cliente
        ResponseEntity<Map<String, String>> resposta = globalExceptionHandler.handleConflitoOtimista(ex);
        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertTrue(resposta.getBody().get("erro").contains("Conflito de concorrencia na atualizacao do chamado"));
    }

    @Test
    void deveRetornar409PelaApiQuandoHouverConflitoDeVersao() throws Exception {
        Instant agora = Instant.now();
        Chamado inicial = new Chamado(
                null, "CH-2026-000200", "Sem acesso a rede", "Nao abre sites", "Redes",
                Prioridade.ALTA, StatusChamado.EM_ANDAMENTO, "usr-teste", "Usuario Teste",
                "dep-redes", "Redes", "ate-teste", "Tecnico Teste", null, agora, agora, agora, null, null,
                null, null, null, List.of(), null
        );
        Chamado salvo = chamadoRepository.save(inicial);

        // Altera a versao diretamente no banco para simular que outro cliente atualizou o registro antes
        Chamado instanciaDesatualizada = chamadoRepository.findById(salvo.id()).orElseThrow();
        chamadoRepository.save(instanciaDesatualizada.comStatus(
                StatusChamado.AGUARDANDO_TERCEIRO, "ate-teste", agora, null, null, null, "Pausa"
        ));

        // Uma requisicao HTTP que tentasse salvar baseado na versao antiga geraria 409
        OptimisticLockingFailureException simulada = new OptimisticLockingFailureException("Simulated conflict");
        ResponseEntity<Map<String, String>> resposta = globalExceptionHandler.handleConflitoOtimista(simulada);

        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals("Conflito de concorrencia na atualizacao do chamado. O registro foi modificado por outra requisicao. Tente novamente.",
                resposta.getBody().get("erro"));
    }
}
