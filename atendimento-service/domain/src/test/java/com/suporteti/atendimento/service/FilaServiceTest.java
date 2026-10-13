package com.suporteti.atendimento.service;

import com.suporteti.atendimento.event.EventoSaida;
import com.suporteti.atendimento.exception.AcessoNegadoException;
import com.suporteti.atendimento.exception.RegraDeNegocioException;
import com.suporteti.atendimento.model.*;
import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import com.suporteti.atendimento.port.PublicadorEventos;
import com.suporteti.atendimento.port.TransacaoExecutor;
import com.suporteti.atendimento.repository.AtendenteRepository;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.FilaRepository;
import com.suporteti.atendimento.repository.SlaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class FilaServiceTest {

    static class TransacaoExecutorFake implements TransacaoExecutor {
        @Override
        public <T> T executar(Supplier<T> trabalho) {
            return trabalho.get();
        }

        @Override
        public void executar(Runnable trabalho) {
            trabalho.run();
        }
    }

    static class PublicadorEventosFake implements PublicadorEventos {
        final List<EventoSaida> eventos = new ArrayList<>();

        @Override
        public void publicar(EventoSaida evento) {
            eventos.add(evento);
        }
    }

    static class FilaRepositoryFake implements FilaRepository {
        final Map<String, ItemFila> porChamadoId = new HashMap<>();

        @Override
        public ItemFila save(ItemFila item) {
            String id = item.id() != null ? item.id() : UUID.randomUUID().toString();
            ItemFila salvo = new ItemFila(
                    id, item.chamadoId(), item.protocolo(), item.categoria(),
                    item.prioridade(), item.departamentoId(), item.status(),
                    item.atendenteId(), item.prazoSla(), item.criadoEm(),
                    item.atribuidoEm(), item.concluidoEm(), item.motivo(), item.versao()
            );
            porChamadoId.put(item.chamadoId(), salvo);
            return salvo;
        }

        @Override
        public Optional<ItemFila> buscarPorChamadoId(String chamadoId) {
            return Optional.ofNullable(porChamadoId.get(chamadoId));
        }

        @Override
        public List<ItemFila> listar(String departamentoId, StatusItemFila status) {
            return porChamadoId.values().stream()
                    .filter(i -> departamentoId == null || departamentoId.equals(i.departamentoId()))
                    .filter(i -> status == null || status == i.status())
                    .toList();
        }

        @Override
        public synchronized Optional<ItemFila> reivindicar(String chamadoId, String departamentoId, String atendenteId, Instant instante) {
            ItemFila item = porChamadoId.get(chamadoId);
            if (item != null && item.status() == StatusItemFila.NA_FILA && departamentoId.equals(item.departamentoId())) {
                ItemFila atribuido = item.comAtribuicao(atendenteId, instante);
                porChamadoId.put(chamadoId, atribuido);
                return Optional.of(atribuido);
            }
            return Optional.empty();
        }

        @Override
        public void concluir(String chamadoId, Instant instante) {
            ItemFila item = porChamadoId.get(chamadoId);
            if (item != null) {
                porChamadoId.put(chamadoId, item.comConclusao(instante));
            }
        }
    }

    static class AtendenteRepositoryFake implements AtendenteRepository {
        final Map<String, Atendente> store = new HashMap<>();

        @Override
        public Atendente save(Atendente atendente) {
            store.put(atendente.id(), atendente);
            return atendente;
        }

        @Override
        public Optional<Atendente> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Atendente> listar(String departamentoId) {
            return new ArrayList<>(store.values());
        }

        @Override
        public synchronized boolean reservarCapacidade(String atendenteId) {
            Atendente a = store.get(atendenteId);
            if (a != null && a.podeAtender()) {
                store.put(atendenteId, a.comIncrementoAtivos());
                return true;
            }
            return false;
        }

        @Override
        public synchronized void liberarCapacidade(String atendenteId) {
            Atendente a = store.get(atendenteId);
            if (a != null) {
                store.put(atendenteId, a.comDecrementoAtivos());
            }
        }
    }

    static class DepartamentoRepositoryFake implements DepartamentoRepository {
        final Map<String, Departamento> store = new HashMap<>();

        @Override
        public Departamento save(Departamento departamento) {
            store.put(departamento.id(), departamento);
            return departamento;
        }

        @Override
        public Optional<Departamento> findById(String id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Departamento> listar() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Optional<Departamento> buscarPorCategoria(String categoria) {
            return store.values().stream()
                    .filter(d -> d.ativo() && d.categorias().contains(categoria))
                    .findFirst();
        }
    }

    static class SlaRepositoryFake implements SlaRepository {
        final Map<String, Sla> store = new HashMap<>();

        @Override
        public Sla save(Sla sla) {
            store.put(sla.prioridade().toUpperCase(), sla);
            return sla;
        }

        @Override
        public Optional<Sla> findByPrioridade(String prioridade) {
            return Optional.ofNullable(store.get(prioridade.toUpperCase()));
        }

        @Override
        public List<Sla> listar() {
            return new ArrayList<>(store.values());
        }
    }

    private FilaService filaService;
    private FilaRepositoryFake filaRepo;
    private AtendenteRepositoryFake atendRepo;
    private DepartamentoRepositoryFake depRepo;
    private SlaRepositoryFake slaRepo;
    private PublicadorEventosFake publicador;

    @BeforeEach
    void setUp() {
        filaRepo = new FilaRepositoryFake();
        atendRepo = new AtendenteRepositoryFake();
        depRepo = new DepartamentoRepositoryFake();
        slaRepo = new SlaRepositoryFake();
        publicador = new PublicadorEventosFake();

        filaService = new FilaService(
                filaRepo, atendRepo, depRepo, slaRepo,
                publicador, new TransacaoExecutorFake()
        );

        // Seed básico
        depRepo.save(new Departamento("dep-redes", "Redes", List.of("Wifi", "Roteadores"), true));
        slaRepo.save(new Sla("ALTA", 8));
        slaRepo.save(new Sla("MEDIA", 24));
    }

    @Test
    void triagemComSucessoDeveColocarNaFilaECriarEventoAceito() {
        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-1", "CH-2026-001", "Wifi", "ALTA", "Sem sinal", "user-solicitante", Instant.now()
        );

        ItemFila item = filaService.triar(chamado);

        assertEquals(StatusItemFila.NA_FILA, item.status());
        assertEquals("dep-redes", item.departamentoId());
        assertNotNull(item.prazoSla());
        assertEquals(1, publicador.eventos.size());

        EventoSaida.ChamadoTriado evento = (EventoSaida.ChamadoTriado) publicador.eventos.get(0);
        assertEquals("ACEITO", evento.resultado());
        assertEquals("dep-redes", evento.departamentoId());
    }

    @Test
    void triagemComCategoriaInvalidaDeveRejeitar() {
        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-2", "CH-2026-002", "CategoriaInexistente", "ALTA", "Problema", "user-solicitante", Instant.now()
        );

        ItemFila item = filaService.triar(chamado);

        assertEquals(StatusItemFila.REJEITADO, item.status());
        assertNotNull(item.motivo());
        assertEquals(1, publicador.eventos.size());

        EventoSaida.ChamadoTriado evento = (EventoSaida.ChamadoTriado) publicador.eventos.get(0);
        assertEquals("REJEITADO", evento.resultado());
    }

    @Test
    void triagemComPrioridadeSemSlaDeveRejeitar() {
        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-3", "CH-2026-003", "Wifi", "CRITICA", "Caiu tudo", "user-solicitante", Instant.now()
        );

        ItemFila item = filaService.triar(chamado);

        assertEquals(StatusItemFila.REJEITADO, item.status());
        assertTrue(item.motivo().contains("SLA"));
        assertEquals(1, publicador.eventos.size());
    }

    @Test
    void assumirChamadoComSucesso() {
        // Prepara item na fila
        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-10", "CH-2026-010", "Wifi", "ALTA", "Problema", "user-solicitante", Instant.now()
        );
        filaService.triar(chamado);

        // Prepara atendente
        atendRepo.save(new Atendente("tec-1", "Roberto", "dep-redes", 3, 0, true));

        UsuarioLogado usuario = new UsuarioLogado("tec-1", "Roberto", true);
        ItemFila assumido = filaService.assumir(usuario, "ch-10");

        assertEquals(StatusItemFila.ATRIBUIDO, assumido.status());
        assertEquals("tec-1", assumido.atendenteId());
        assertEquals(1, atendRepo.findById("tec-1").get().chamadosAtivos());

        // Evento gerado
        boolean temEventoAtribuido = publicador.eventos.stream()
                .anyMatch(e -> e instanceof EventoSaida.ChamadoAtribuido ca && ca.chamadoId().equals("ch-10"));
        assertTrue(temEventoAtribuido);
    }

    @Test
    void assumirChamadoSemAutenticacaoDeveLancarAcessoNegado() {
        assertThrows(AcessoNegadoException.class, () ->
                filaService.assumir(null, "ch-10"));
    }

    @Test
    void assumirChamadoComAtendenteDeOutroDepartamentoDeveLancarErro() {
        depRepo.save(new Departamento("dep-soft", "Software", List.of("Excel"), true));
        atendRepo.save(new Atendente("tec-2", "Lucas", "dep-soft", 3, 0, true));

        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-20", "CH-2026-020", "Wifi", "ALTA", "Sem rede", "user", Instant.now()
        );
        filaService.triar(chamado);

        UsuarioLogado usuario = new UsuarioLogado("tec-2", "Lucas", true);
        assertThrows(RegraDeNegocioException.class, () ->
                filaService.assumir(usuario, "ch-20"));
    }

    @Test
    void assumirChamadoComCapacidadeEsgotadaDeveLancarErro() {
        atendRepo.save(new Atendente("tec-1", "Roberto", "dep-redes", 1, 1, true));

        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-30", "CH-2026-030", "Wifi", "ALTA", "Sem rede", "user", Instant.now()
        );
        filaService.triar(chamado);

        UsuarioLogado usuario = new UsuarioLogado("tec-1", "Roberto", true);
        assertThrows(RegraDeNegocioException.class, () ->
                filaService.assumir(usuario, "ch-30"));
    }

    @Test
    void concluirChamadoDeveLiberarCapacidade() {
        atendRepo.save(new Atendente("tec-1", "Roberto", "dep-redes", 3, 2, true));
        ChamadoRecebido chamado = new ChamadoRecebido(
                "ch-40", "CH-2026-040", "Wifi", "ALTA", "Sem rede", "user", Instant.now()
        );
        filaService.triar(chamado);

        filaService.concluir("ch-40", "tec-1");

        assertEquals(1, atendRepo.findById("tec-1").get().chamadosAtivos());
        ItemFila item = filaRepo.buscarPorChamadoId("ch-40").get();
        assertEquals(StatusItemFila.CONCLUIDO, item.status());
    }
}
