package com.suporteti.relatorio.service;

import com.suporteti.relatorio.exception.AcessoNegadoException;
import com.suporteti.relatorio.exception.RegraDeNegocioException;
import com.suporteti.relatorio.model.AgrupamentoMetrica;
import com.suporteti.relatorio.model.ChamadoConcluido;
import com.suporteti.relatorio.model.RelatorioMensal;
import com.suporteti.relatorio.port.ChamadosClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioServiceTest {

    private List<ChamadoConcluido> dadosFalsos;
    private ChamadosClient chamadosClient;
    private RelatorioService service;
    private final ZoneId zoneId = ZoneId.of("America/Sao_Paulo");

    @BeforeEach
    void setup() {
        dadosFalsos = new ArrayList<>();
        chamadosClient = periodo -> dadosFalsos;
        service = new RelatorioService(chamadosClient, zoneId);
    }

    @Test
    void deveExigirPerfilAdminParaGerarRelatorio() {
        assertThrows(AcessoNegadoException.class, () ->
                service.gerar(false, YearMonth.of(2026, 10)));
    }

    @Test
    void deveRejeitarPeriodoNulo() {
        assertThrows(RegraDeNegocioException.class, () ->
                service.gerar(true, null));
    }

    @Test
    void deveRetornarRelatorioZeradoQuandoNaoHouverChamados() {
        RelatorioMensal relatorio = service.gerar(true, YearMonth.of(2026, 10));

        assertEquals(2026, relatorio.ano());
        assertEquals(10, relatorio.mes());
        assertEquals(0, relatorio.totalConcluidos());
        assertEquals(new BigDecimal("0.00"), relatorio.horasTotais());
        assertEquals(new BigDecimal("0.00"), relatorio.custoTotal());
        assertEquals(new BigDecimal("0.00"), relatorio.tempoMedioAtendimentoHoras());
        assertEquals(new BigDecimal("0.00"), relatorio.tempoMedioPrimeiraRespostaHoras());
        assertEquals(new BigDecimal("0.00"), relatorio.percentualSlaCumprido());
        assertTrue(relatorio.porCategoria().isEmpty());
    }

    @Test
    void deveCalcularMetricasCorretamenteComDadosFixos() {
        // Chamado 1: criado as 10:00, atribuido as 11:00 (1h), resolvido as 14:00 (4h), prazo SLA as 15:00 (cumprido)
        dadosFalsos.add(new ChamadoConcluido(
                "CH-01", "Software", "ALTA", "Sistemas", "ate-1", "Carlos Tecnico",
                Instant.parse("2026-10-10T13:00:00Z"), // 10:00 SP
                Instant.parse("2026-10-10T14:00:00Z"), // 11:00 SP (1h resposta)
                Instant.parse("2026-10-10T17:00:00Z"), // 14:00 SP (4h atendimento)
                Instant.parse("2026-10-10T18:00:00Z"), // 15:00 SP (SLA ok)
                new BigDecimal("4.00"), new BigDecimal("200.00")
        ));

        // Chamado 2: criado as 08:00, atribuido as 10:00 (2h), resolvido as 16:00 (8h), prazo SLA as 14:00 (estourado)
        dadosFalsos.add(new ChamadoConcluido(
                "CH-02", "Redes", "CRITICA", "Infra", "ate-2", "Ana Especialista",
                Instant.parse("2026-10-15T11:00:00Z"), // 08:00 SP
                Instant.parse("2026-10-15T13:00:00Z"), // 10:00 SP (2h resposta)
                Instant.parse("2026-10-15T19:00:00Z"), // 16:00 SP (8h atendimento)
                Instant.parse("2026-10-15T17:00:00Z"), // 14:00 SP (SLA estourado)
                new BigDecimal("6.00"), new BigDecimal("350.00")
        ));

        RelatorioMensal relatorio = service.gerar(true, YearMonth.of(2026, 10));

        assertEquals(2, relatorio.totalConcluidos());
        assertEquals(new BigDecimal("10.00"), relatorio.horasTotais());
        assertEquals(new BigDecimal("550.00"), relatorio.custoTotal());

        // Tempo medio atendimento: (4h + 8h) / 2 = 6.00h
        assertEquals(new BigDecimal("6.00"), relatorio.tempoMedioAtendimentoHoras());

        // Tempo medio resposta: (1h + 2h) / 2 = 1.50h
        assertEquals(new BigDecimal("1.50"), relatorio.tempoMedioPrimeiraRespostaHoras());

        // SLA: 1 de 2 cumprido = 50.00%
        assertEquals(new BigDecimal("50.00"), relatorio.percentualSlaCumprido());

        // Agrupamentos
        assertEquals(2, relatorio.porCategoria().size());
        assertEquals(2, relatorio.porAtendente().size());

        AgrupamentoMetrica grupoRedes = relatorio.porCategoria().stream()
                .filter(g -> g.chave().equals("Redes")).findFirst().orElseThrow();
        assertEquals(1, grupoRedes.quantidade());
        assertEquals(new BigDecimal("6.00"), grupoRedes.horas());
        assertEquals(new BigDecimal("350.00"), grupoRedes.custo());
    }

    @Test
    void deveRespeitarFronteiraDeMesNoFusoDeSaoPaulo() {
        // Chamado A: resolvido em 2026-09-30 23:59:00 SP (UTC: 2026-10-01T02:59:00Z) -> Deve pertencer a SETEMBRO!
        dadosFalsos.add(new ChamadoConcluido(
                "CH-SET", "Geral", "BAIXA", "TI", "ate-1", "Carlos Tecnico",
                Instant.parse("2026-10-01T00:00:00Z"),
                Instant.parse("2026-10-01T01:00:00Z"),
                Instant.parse("2026-10-01T02:59:00Z"), // 23:59 em Sao Paulo no dia 30/09
                Instant.parse("2026-10-02T00:00:00Z"),
                BigDecimal.ONE, BigDecimal.TEN
        ));

        // Chamado B: resolvido em 2026-10-01 00:01:00 SP (UTC: 2026-10-01T03:01:00Z) -> Deve pertencer a OUTUBRO!
        dadosFalsos.add(new ChamadoConcluido(
                "CH-OUT", "Geral", "BAIXA", "TI", "ate-1", "Carlos Tecnico",
                Instant.parse("2026-10-01T01:00:00Z"),
                Instant.parse("2026-10-01T02:00:00Z"),
                Instant.parse("2026-10-01T03:01:00Z"), // 00:01 em Sao Paulo no dia 01/10
                Instant.parse("2026-10-02T00:00:00Z"),
                BigDecimal.ONE, BigDecimal.TEN
        ));

        // Relatório de Outubro
        RelatorioMensal relOutubro = service.gerar(true, YearMonth.of(2026, 10));
        assertEquals(1, relOutubro.totalConcluidos(), "Apenas o chamado resolvido em outubro (horário de SP) deve entrar");

        // Relatório de Setembro
        RelatorioMensal relSetembro = service.gerar(true, YearMonth.of(2026, 9));
        assertEquals(1, relSetembro.totalConcluidos(), "O chamado resolvido no final de setembro (horário de SP) deve entrar em setembro");
    }
}
