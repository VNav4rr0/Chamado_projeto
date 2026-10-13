package com.suporteti.atendimento.service;

import com.suporteti.atendimento.exception.RecursoNaoEncontradoException;
import com.suporteti.atendimento.exception.RegraDeNegocioException;
import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.repository.AtendenteRepository;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.SlaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CadastroServiceTest {

    static class DepartamentoRepositoryFake implements DepartamentoRepository {
        private final Map<String, Departamento> store = new HashMap<>();

        @Override
        public Departamento save(Departamento departamento) {
            String id = departamento.id() != null ? departamento.id() : UUID.randomUUID().toString();
            Departamento salvo = new Departamento(id, departamento.nome(), departamento.categorias(), departamento.ativo());
            store.put(id, salvo);
            return salvo;
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
                    .filter(d -> d.categorias().contains(categoria))
                    .findFirst();
        }
    }

    static class AtendenteRepositoryFake implements AtendenteRepository {
        private final Map<String, Atendente> store = new HashMap<>();

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
            if (departamentoId == null || departamentoId.isBlank()) {
                return new ArrayList<>(store.values());
            }
            return store.values().stream()
                    .filter(a -> departamentoId.equals(a.departamentoId()))
                    .toList();
        }

        @Override
        public boolean reservarCapacidade(String atendenteId) {
            Atendente a = store.get(atendenteId);
            if (a != null && a.podeAtender()) {
                store.put(atendenteId, a.comIncrementoAtivos());
                return true;
            }
            return false;
        }

        @Override
        public void liberarCapacidade(String atendenteId) {
            Atendente a = store.get(atendenteId);
            if (a != null) {
                store.put(atendenteId, a.comDecrementoAtivos());
            }
        }
    }

    static class SlaRepositoryFake implements SlaRepository {
        private final Map<String, Sla> store = new HashMap<>();

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

    private CadastroService cadastroService;
    private DepartamentoRepositoryFake depRepo;

    @BeforeEach
    void setUp() {
        depRepo = new DepartamentoRepositoryFake();
        AtendenteRepositoryFake atendRepo = new AtendenteRepositoryFake();
        SlaRepositoryFake slaRepo = new SlaRepositoryFake();
        cadastroService = new CadastroService(depRepo, atendRepo, slaRepo);
    }

    @Test
    void deveCriarEBuscarDepartamento() {
        Departamento dep = cadastroService.criarDepartamento("Redes", List.of("Switches", "Roteadores"));
        assertNotNull(dep.id());
        assertEquals("Redes", dep.nome());
        assertTrue(dep.ativo());

        Departamento buscado = cadastroService.buscarDepartamentoPorId(dep.id());
        assertEquals(dep.id(), buscado.id());
    }

    @Test
    void deveListarCategoriasOrdenadasEDistintas() {
        cadastroService.criarDepartamento("Redes", List.of("Wifi", "Roteadores"));
        cadastroService.criarDepartamento("Software", List.of("Email", "Wifi"));

        List<String> categorias = cadastroService.listarCategorias();
        assertEquals(3, categorias.size());
        assertEquals(List.of("Email", "Roteadores", "Wifi"), categorias);
    }

    @Test
    void deveCriarAtendenteComSucesso() {
        Departamento dep = cadastroService.criarDepartamento("Suporte", List.of("Hardware"));
        Atendente atendente = cadastroService.criarAtendente("user-1", "Carlos", dep.id(), 5);

        assertEquals("user-1", atendente.id());
        assertEquals(5, atendente.capacidade());
        assertEquals(0, atendente.chamadosAtivos());
        assertTrue(atendente.ativo());
    }

    @Test
    void naoDeveCriarAtendenteComCapacidadeZeroOuNegativa() {
        Departamento dep = cadastroService.criarDepartamento("Suporte", List.of("Hardware"));
        assertThrows(RegraDeNegocioException.class, () ->
                cadastroService.criarAtendente("user-1", "Carlos", dep.id(), 0));
    }

    @Test
    void deveSalvarEBuscarSla() {
        cadastroService.salvarSla("ALTA", 8);
        Sla sla = cadastroService.buscarSlaPorPrioridade("alta");
        assertEquals("ALTA", sla.prioridade());
        assertEquals(8, sla.horas());
    }

    @Test
    void buscarSlaInexistenteDeveLancarExcecao() {
        assertThrows(RecursoNaoEncontradoException.class, () ->
                cadastroService.buscarSlaPorPrioridade("CRITICA"));
    }
}
