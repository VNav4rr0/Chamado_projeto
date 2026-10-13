package com.suporteti.relatorio.service;

import com.suporteti.relatorio.exception.AcessoNegadoException;
import com.suporteti.relatorio.exception.RegraDeNegocioException;
import com.suporteti.relatorio.model.AgrupamentoMetrica;
import com.suporteti.relatorio.model.ChamadoConcluido;
import com.suporteti.relatorio.model.RelatorioMensal;
import com.suporteti.relatorio.port.ChamadosClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class RelatorioService {

    private final ChamadosClient chamadosClient;
    private final ZoneId zoneId;

    public RelatorioService(ChamadosClient chamadosClient, ZoneId zoneId) {
        this.chamadosClient = chamadosClient;
        this.zoneId = zoneId != null ? zoneId : ZoneId.of("America/Sao_Paulo");
    }

    public RelatorioMensal gerar(boolean admin, YearMonth periodo) {
        if (!admin) {
            throw new AcessoNegadoException("Acesso negado. Apenas administradores podem gerar relatorios.");
        }
        if (periodo == null) {
            throw new RegraDeNegocioException("Periodo e obrigatorio.");
        }

        List<ChamadoConcluido> concluidos = chamadosClient.listarConcluidos(periodo);

        List<ChamadoConcluido> filtrados = concluidos.stream()
                .filter(c -> c.resolvidoEm() != null)
                .filter(c -> YearMonth.from(c.resolvidoEm().atZone(zoneId)).equals(periodo))
                .toList();

        if (filtrados.isEmpty()) {
            return new RelatorioMensal(
                    periodo.getYear(),
                    periodo.getMonthValue(),
                    0L,
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    List.of(),
                    List.of(),
                    List.of()
            );
        }

        long totalConcluidos = filtrados.size();

        BigDecimal horasTotais = filtrados.stream()
                .map(c -> c.horasTrabalhadas() != null ? c.horasTrabalhadas() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal custoTotal = filtrados.stream()
                .map(c -> c.custo() != null ? c.custo() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        double somaHorasAtendimento = filtrados.stream()
                .mapToDouble(c -> (double) Duration.between(c.criadoEm(), c.resolvidoEm()).toSeconds() / 3600.0)
                .sum();
        BigDecimal tempoMedioAtendimento = BigDecimal.valueOf(somaHorasAtendimento / totalConcluidos)
                .setScale(2, RoundingMode.HALF_UP);

        List<ChamadoConcluido> comAtribuicao = filtrados.stream()
                .filter(c -> c.atribuidoEm() != null)
                .toList();
        BigDecimal tempoMedioPrimeiraResposta = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        if (!comAtribuicao.isEmpty()) {
            double somaHorasResposta = comAtribuicao.stream()
                    .mapToDouble(c -> (double) Duration.between(c.criadoEm(), c.atribuidoEm()).toSeconds() / 3600.0)
                    .sum();
            tempoMedioPrimeiraResposta = BigDecimal.valueOf(somaHorasResposta / comAtribuicao.size())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        long dentroDoSla = filtrados.stream()
                .filter(c -> c.prazoSla() != null && !c.resolvidoEm().isAfter(c.prazoSla()))
                .count();
        BigDecimal percentualSlaCumprido = BigDecimal.valueOf((double) dentroDoSla / totalConcluidos * 100.0)
                .setScale(2, RoundingMode.HALF_UP);

        List<AgrupamentoMetrica> porCategoria = agruparPor(filtrados, c -> c.categoria() != null ? c.categoria() : "Indefinida");
        List<AgrupamentoMetrica> porPrioridade = agruparPor(filtrados, c -> c.prioridade() != null ? c.prioridade() : "Indefinida");
        List<AgrupamentoMetrica> porAtendente = agruparPor(filtrados, c -> c.atendenteNome() != null ? c.atendenteNome() : "Nao atribuido");

        return new RelatorioMensal(
                periodo.getYear(),
                periodo.getMonthValue(),
                totalConcluidos,
                tempoMedioAtendimento,
                tempoMedioPrimeiraResposta,
                percentualSlaCumprido,
                horasTotais,
                custoTotal,
                porCategoria,
                porPrioridade,
                porAtendente
        );
    }

    private List<AgrupamentoMetrica> agruparPor(
            List<ChamadoConcluido> lista,
            java.util.function.Function<ChamadoConcluido, String> extratorChave
    ) {
        Map<String, List<ChamadoConcluido>> mapa = lista.stream()
                .collect(Collectors.groupingBy(extratorChave));

        return mapa.entrySet().stream()
                .map(e -> {
                    String chave = e.getKey();
                    long qtd = e.getValue().size();
                    BigDecimal horas = e.getValue().stream()
                            .map(c -> c.horasTrabalhadas() != null ? c.horasTrabalhadas() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal custo = e.getValue().stream()
                            .map(c -> c.custo() != null ? c.custo() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .setScale(2, RoundingMode.HALF_UP);
                    return new AgrupamentoMetrica(chave, qtd, horas, custo);
                })
                .sorted(Comparator.comparing(AgrupamentoMetrica::quantidade).reversed())
                .toList();
    }
}
