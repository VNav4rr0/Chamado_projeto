package com.suporteti.atendimento;

import com.suporteti.atendimento.exception.RegraDeNegocioException;
import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.ChamadoRecebido;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.model.UsuarioLogado;
import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import com.suporteti.atendimento.repository.AtendenteRepository;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.FilaRepository;
import com.suporteti.atendimento.repository.SlaRepository;
import com.suporteti.atendimento.service.FilaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.rabbitmq.listener.simple.auto-startup=false"
})
class AtribuicaoConcorrenteIT {

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
    private FilaService filaService;

    @Autowired
    private FilaRepository filaRepository;

    @Autowired
    private AtendenteRepository atendenteRepository;

    @Autowired
    private DepartamentoRepository departamentoRepository;

    @Autowired
    private SlaRepository slaRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void limparBanco() {
        mongoTemplate.getDb().drop();
        departamentoRepository.save(new Departamento("dep-concorrencia", "TI", List.of("Geral"), true));
        slaRepository.save(new Sla("ALTA", 8));
    }

    @Test
    void cenarioA_DezAtendentesDisputandoMesmoChamadoAoMesmoTempo() throws InterruptedException {
        String chamadoId = "chamado-disputa-a";
        filaService.triar(new ChamadoRecebido(
                chamadoId, "CH-CONC-01", "Geral", "ALTA", "Disputa de chamado", "usuario", Instant.now()
        ));

        int totalAtendentes = 10;
        for (int i = 1; i <= totalAtendentes; i++) {
            atendenteRepository.save(new Atendente(
                    "atendente-a-" + i, "Atendente " + i, "dep-concorrencia", 5, 0, true
            ));
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalAtendentes);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalAtendentes);

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhasConflito = new AtomicInteger();
        AtomicInteger outrosErros = new AtomicInteger();

        for (int i = 1; i <= totalAtendentes; i++) {
            final String atendenteId = "atendente-a-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    filaService.assumir(new UsuarioLogado(atendenteId, "Atendente", true), chamadoId);
                    sucessos.incrementAndGet();
                } catch (RegraDeNegocioException e) {
                    falhasConflito.incrementAndGet();
                } catch (Throwable e) {
                    outrosErros.incrementAndGet();
                    System.err.println("Erro inesperado no cenario A: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finalizou = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finalizou, "Todas as threads devem finalizar no tempo limite");
        assertEquals(0, outrosErros.get(), "Nenhum erro inesperado deve ocorrer");
        assertEquals(1, sucessos.get(), "Exatamente 1 atendente deve conseguir assumir o chamado");
        assertEquals(9, falhasConflito.get(), "Exatamente 9 atendentes devem receber RegraDeNegocioException");

        ItemFila item = filaRepository.buscarPorChamadoId(chamadoId).orElseThrow();
        assertEquals(StatusItemFila.ATRIBUIDO, item.status());
        assertNotNull(item.atendenteId());

        int somaAtivos = 0;
        for (int i = 1; i <= totalAtendentes; i++) {
            somaAtivos += atendenteRepository.findById("atendente-a-" + i).orElseThrow().chamadosAtivos();
        }
        assertEquals(1, somaAtivos, "A soma de chamados ativos entre os 10 atendentes deve ser exatamente 1");
    }

    @Test
    void cenarioB_UmAtendenteComCapacidadeTresTentandoAssumirDezChamados() throws InterruptedException {
        String atendenteId = "atendente-cap-3";
        atendenteRepository.save(new Atendente(
                atendenteId, "Atendente Limitado", "dep-concorrencia", 3, 0, true
        ));

        int totalChamados = 10;
        for (int i = 1; i <= totalChamados; i++) {
            filaService.triar(new ChamadoRecebido(
                    "chamado-b-" + i, "CH-B-" + i, "Geral", "ALTA", "Chamado " + i, "usuario", Instant.now()
            ));
        }

        ExecutorService executor = Executors.newFixedThreadPool(totalChamados);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(totalChamados);

        AtomicInteger sucessos = new AtomicInteger();
        AtomicInteger falhasRegra = new AtomicInteger();
        AtomicInteger outrosErros = new AtomicInteger();

        for (int i = 1; i <= totalChamados; i++) {
            final String chamadoId = "chamado-b-" + i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    filaService.assumir(new UsuarioLogado(atendenteId, "Atendente Limitado", true), chamadoId);
                    sucessos.incrementAndGet();
                } catch (RegraDeNegocioException e) {
                    falhasRegra.incrementAndGet();
                } catch (Throwable e) {
                    outrosErros.incrementAndGet();
                    System.err.println("Erro inesperado no cenario B: " + e.getClass().getName() + " - " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finalizou = doneLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finalizou, "Todas as threads devem finalizar no tempo limite");
        assertEquals(0, outrosErros.get(), "Nenhum erro inesperado deve ocorrer");
        assertTrue(sucessos.get() <= 3, "No maximo 3 chamados podem ser assumidos pelo atendente com capacidade 3");
        assertTrue(sucessos.get() > 0, "Pelo menos 1 chamado deve ser assumido");
        assertEquals(totalChamados, sucessos.get() + falhasRegra.get(), "Todas as 10 tentativas devem resultar em sucesso ou RegraDeNegocioException");

        Atendente atendente = atendenteRepository.findById(atendenteId).orElseThrow();
        assertEquals(sucessos.get(), atendente.chamadosAtivos(), "Chamados ativos deve ser igual a quantidade de chamados assumidos");
        assertTrue(atendente.chamadosAtivos() <= 3, "Chamados ativos não podem ultrapassar a capacidade (3)");
    }
}
