package com.suporteti.chamado.service;

import com.suporteti.chamado.event.EventoSaida;
import com.suporteti.chamado.exception.AcessoNegadoException;
import com.suporteti.chamado.exception.RecursoNaoEncontradoException;
import com.suporteti.chamado.exception.RegraDeNegocioException;
import com.suporteti.chamado.model.*;
import com.suporteti.chamado.model.enumerable.Prioridade;
import com.suporteti.chamado.model.enumerable.StatusChamado;
import com.suporteti.chamado.port.GeradorProtocolo;
import com.suporteti.chamado.port.PublicadorEventos;
import com.suporteti.chamado.port.TransacaoExecutor;
import com.suporteti.chamado.repository.ChamadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ChamadoServiceTest {

    private ChamadoRepository chamadoRepository;
    private List<EventoSaida> eventosPublicados;
    private PublicadorEventos publicadorEventos;
    private TransacaoExecutor transacaoExecutor;
    private GeradorProtocolo geradorProtocolo;
    private Clock clock;
    private ChamadoService service;

    private final UsuarioLogado usuarioComum = new UsuarioLogado("usr-1", "Joao Solicitante", false);
    private final UsuarioLogado outroUsuario = new UsuarioLogado("usr-2", "Maria Outra", false);
    private final UsuarioLogado atendente = new UsuarioLogado("ate-1", "Carlos Tecnico", false);
    private final UsuarioLogado admin = new UsuarioLogado("adm-1", "Administrador", true);

    @BeforeEach
    void setup() {
        Map<String, Chamado> banco = new HashMap<>();
        AtomicInteger idSeq = new AtomicInteger(1);

        chamadoRepository = new ChamadoRepository() {
            @Override
            public Chamado save(Chamado chamado) {
                String id = chamado.id() != null ? chamado.id() : "ch-" + idSeq.getAndIncrement();
                Chamado salvo = new Chamado(
                        id,
                        chamado.protocolo(),
                        chamado.titulo(),
                        chamado.descricao(),
                        chamado.categoria(),
                        chamado.prioridade(),
                        chamado.status(),
                        chamado.solicitanteId(),
                        chamado.solicitanteNome(),
                        chamado.departamentoId(),
                        chamado.departamentoNome(),
                        chamado.atendenteId(),
                        chamado.atendenteNome(),
                        chamado.prazoSla(),
                        chamado.criadoEm(),
                        chamado.analisadoEm(),
                        chamado.atribuidoEm(),
                        chamado.resolvidoEm(),
                        chamado.fechadoEm(),
                        chamado.horasTrabalhadas(),
                        chamado.custo(),
                        chamado.motivo(),
                        chamado.historico(),
                        chamado.versao() == null ? 0L : chamado.versao() + 1
                );
                banco.put(id, salvo);
                return salvo;
            }

            @Override
            public Optional<Chamado> findById(String id) {
                return Optional.ofNullable(banco.get(id));
            }

            @Override
            public Pagina<Chamado> buscar(FiltroChamados filtro) {
                List<Chamado> lista = banco.values().stream()
                        .filter(c -> filtro.solicitanteId() == null || filtro.solicitanteId().equals(c.solicitanteId()))
                        .filter(c -> filtro.status() == null || filtro.status().isEmpty() || filtro.status().contains(c.status()))
                        .filter(c -> filtro.atendenteId() == null || filtro.atendenteId().equals(c.atendenteId()))
                        .toList();
                return new Pagina<>(lista, lista.size(), filtro.pagina(), filtro.tamanho());
            }
        };

        eventosPublicados = new ArrayList<>();
        publicadorEventos = eventosPublicados::add;

        transacaoExecutor = new TransacaoExecutor() {
            @Override
            public <T> T executar(Supplier<T> trabalho) {
                return trabalho.get();
            }

            @Override
            public void executar(Runnable trabalho) {
                trabalho.run();
            }
        };

        AtomicInteger protocoloSeq = new AtomicInteger(1);
        geradorProtocolo = agora -> "CH-2026-" + String.format("%06d", protocoloSeq.getAndIncrement());

        clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);

        service = new ChamadoService(
                chamadoRepository,
                publicadorEventos,
                transacaoExecutor,
                geradorProtocolo,
                clock
        );
    }

    @Test
    void deveAbrirChamadoComSucessoEPublicarEvento() {
        Chamado chamado = service.abrir(usuarioComum, "Erro no Teams", "Nao consigo conectar", "Software", Prioridade.ALTA);

        assertNotNull(chamado.id());
        assertEquals("CH-2026-000001", chamado.protocolo());
        assertEquals(StatusChamado.CRIADO, chamado.status());
        assertEquals(usuarioComum.id(), chamado.solicitanteId());
        assertEquals(usuarioComum.nome(), chamado.solicitanteNome());

        assertEquals(1, eventosPublicados.size());
        assertInstanceOf(EventoSaida.ChamadoCriado.class, eventosPublicados.get(0));
        EventoSaida.ChamadoCriado ev = (EventoSaida.ChamadoCriado) eventosPublicados.get(0);
        assertEquals(chamado.id(), ev.chamadoId());
        assertEquals("chamado.criado", ev.chaveRoteamento());
    }

    @Test
    void deveRejeitarAberturaComCamposObrigatoriosAusentes() {
        assertThrows(RegraDeNegocioException.class, () ->
                service.abrir(usuarioComum, "", "descricao", "Software", Prioridade.ALTA));
        assertThrows(RegraDeNegocioException.class, () ->
                service.abrir(usuarioComum, "titulo", " ", "Software", Prioridade.ALTA));
        assertThrows(RegraDeNegocioException.class, () ->
                service.abrir(usuarioComum, "titulo", "descricao", null, Prioridade.ALTA));
        assertThrows(RegraDeNegocioException.class, () ->
                service.abrir(usuarioComum, "titulo", "descricao", "Software", null));
        assertThrows(AcessoNegadoException.class, () ->
                service.abrir(null, "titulo", "descricao", "Software", Prioridade.ALTA));
    }

    @Test
    void deveListarApenasChamadosDoProprioUsuarioComum() {
        service.abrir(usuarioComum, "Chamado 1 do Joao", "desc", "Redes", Prioridade.MEDIA);
        service.abrir(outroUsuario, "Chamado 1 da Maria", "desc", "Hardware", Prioridade.BAIXA);

        Pagina<Chamado> listaJoao = service.listar(usuarioComum, new FiltroChamados(null, null, null, null, null, 0, 10));
        assertEquals(1, listaJoao.itens().size());
        assertEquals("Chamado 1 do Joao", listaJoao.itens().get(0).titulo());

        Pagina<Chamado> listaAdmin = service.listar(admin, new FiltroChamados(null, null, null, null, null, 0, 10));
        assertEquals(2, listaAdmin.itens().size());
    }

    @Test
    void deveConsultarChamadoApenasPorDonoAtendenteOuAdmin() {
        Chamado chamado = service.abrir(usuarioComum, "Chamado 1", "desc", "Redes", Prioridade.MEDIA);

        // Dono pode consultar
        assertNotNull(service.consultar(usuarioComum, chamado.id()));
        // Admin pode consultar
        assertNotNull(service.consultar(admin, chamado.id()));
        // Terceiro nao pode consultar
        assertThrows(AcessoNegadoException.class, () -> service.consultar(outroUsuario, chamado.id()));
    }

    @Test
    void devePermitirTriagemEAtribuicao() {
        Chamado chamado = service.abrir(usuarioComum, "Chamado 1", "desc", "Redes", Prioridade.MEDIA);

        // Aplica triagem aceita
        service.aplicarTriagem(new ResultadoTriagem(
                chamado.id(), "ACEITO", "dep-redes", "Redes", Instant.now().plusSeconds(3600), null
        ));

        Chamado triado = service.consultar(admin, chamado.id());
        assertEquals(StatusChamado.EM_ANALISE, triado.status());
        assertEquals("dep-redes", triado.departamentoId());

        // Aplica atribuicao
        service.aplicarAtribuicao(new Atribuicao(chamado.id(), atendente.id(), atendente.nome(), Instant.now()));

        Chamado atribuido = service.consultar(admin, chamado.id());
        assertEquals(StatusChamado.EM_ANDAMENTO, atribuido.status());
        assertEquals(atendente.id(), atribuido.atendenteId());
    }

    @Test
    void deveGarantirPermissoesDeTransicaoDeStatus() {
        Chamado chamado = service.abrir(usuarioComum, "Problema Rede", "Sem internet", "Redes", Prioridade.ALTA);
        service.aplicarTriagem(new ResultadoTriagem(chamado.id(), "ACEITO", "dep-redes", "Redes", Instant.now().plusSeconds(3600), null));
        service.aplicarAtribuicao(new Atribuicao(chamado.id(), atendente.id(), atendente.nome(), Instant.now()));

        // Outro usuario nao pode mover para AGUARDANDO_TERCEIRO, RESOLVIDO ou REJEITADO
        assertThrows(AcessoNegadoException.class, () ->
                service.alterarStatus(outroUsuario, chamado.id(), StatusChamado.AGUARDANDO_TERCEIRO, null, null, null));
        assertThrows(AcessoNegadoException.class, () ->
                service.alterarStatus(outroUsuario, chamado.id(), StatusChamado.RESOLVIDO, BigDecimal.ONE, BigDecimal.TEN, null));
        assertThrows(AcessoNegadoException.class, () ->
                service.alterarStatus(outroUsuario, chamado.id(), StatusChamado.REJEITADO, null, null, "Motivo"));

        // Atendente atribuido consegue colocar em AGUARDANDO_TERCEIRO
        Chamado aguardando = service.alterarStatus(atendente, chamado.id(), StatusChamado.AGUARDANDO_TERCEIRO, null, null, null);
        assertEquals(StatusChamado.AGUARDANDO_TERCEIRO, aguardando.status());

        // De AGUARDANDO_TERCEIRO de volta para EM_ANDAMENTO
        Chamado emAndamento = service.alterarStatus(atendente, chamado.id(), StatusChamado.EM_ANDAMENTO, null, null, null);
        assertEquals(StatusChamado.EM_ANDAMENTO, emAndamento.status());

        // Resolver exige horasTrabalhadas e custo
        assertThrows(RegraDeNegocioException.class, () ->
                service.alterarStatus(atendente, chamado.id(), StatusChamado.RESOLVIDO, null, BigDecimal.TEN, null));
        assertThrows(RegraDeNegocioException.class, () ->
                service.alterarStatus(atendente, chamado.id(), StatusChamado.RESOLVIDO, BigDecimal.ONE, null, null));

        // Resolver com campos validos
        Chamado resolvido = service.alterarStatus(
                atendente, chamado.id(), StatusChamado.RESOLVIDO, new BigDecimal("2.5"), new BigDecimal("150.00"), null
        );
        assertEquals(StatusChamado.RESOLVIDO, resolvido.status());
        assertEquals(new BigDecimal("2.5"), resolvido.horasTrabalhadas());
        assertEquals(new BigDecimal("150.00"), resolvido.custo());

        // Evento chamado.resolvido foi publicado
        assertTrue(eventosPublicados.stream().anyMatch(e -> e.chaveRoteamento().equals("chamado.resolvido")));

        // Atendente nao pode fechar; apenas solicitante ou admin pode
        assertThrows(AcessoNegadoException.class, () ->
                service.alterarStatus(atendente, chamado.id(), StatusChamado.FECHADO, null, null, null));

        // Solicitante pode fechar
        Chamado fechado = service.alterarStatus(usuarioComum, chamado.id(), StatusChamado.FECHADO, null, null, null);
        assertEquals(StatusChamado.FECHADO, fechado.status());
    }

    @Test
    void devePermitirAdminFecharChamadoResolvido() {
        Chamado chamado = service.abrir(usuarioComum, "Problema 2", "desc", "Redes", Prioridade.ALTA);
        service.aplicarTriagem(new ResultadoTriagem(chamado.id(), "ACEITO", "dep-redes", "Redes", Instant.now().plusSeconds(3600), null));
        service.aplicarAtribuicao(new Atribuicao(chamado.id(), atendente.id(), atendente.nome(), Instant.now()));
        service.alterarStatus(atendente, chamado.id(), StatusChamado.RESOLVIDO, BigDecimal.ONE, BigDecimal.TEN, null);

        // ADMIN pode fechar chamado resolvido
        Chamado fechado = service.alterarStatus(admin, chamado.id(), StatusChamado.FECHADO, null, null, null);
        assertEquals(StatusChamado.FECHADO, fechado.status());
    }

    @Test
    void deveExigirMotivoAoRejeitar() {
        Chamado chamado = service.abrir(usuarioComum, "Problema", "desc", "Redes", Prioridade.ALTA);
        service.aplicarTriagem(new ResultadoTriagem(chamado.id(), "ACEITO", "dep-redes", "Redes", Instant.now().plusSeconds(3600), null));
        service.aplicarAtribuicao(new Atribuicao(chamado.id(), atendente.id(), atendente.nome(), Instant.now()));

        // Rejeitar sem motivo lanca RegraDeNegocioException
        assertThrows(RegraDeNegocioException.class, () ->
                service.alterarStatus(atendente, chamado.id(), StatusChamado.REJEITADO, null, null, "   "));

        // Rejeitar com motivo tem sucesso e publica chamado.rejeitado
        Chamado rejeitado = service.alterarStatus(atendente, chamado.id(), StatusChamado.REJEITADO, null, null, "Equipamento fora do escopo");
        assertEquals(StatusChamado.REJEITADO, rejeitado.status());
        assertEquals("Equipamento fora do escopo", rejeitado.motivo());

        assertTrue(eventosPublicados.stream().anyMatch(e -> e.chaveRoteamento().equals("chamado.rejeitado")));
    }
}
