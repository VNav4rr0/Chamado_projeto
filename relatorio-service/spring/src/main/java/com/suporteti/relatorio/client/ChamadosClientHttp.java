package com.suporteti.relatorio.client;

import com.suporteti.relatorio.exception.ServicoIndisponivelException;
import com.suporteti.relatorio.model.ChamadoConcluido;
import com.suporteti.relatorio.port.ChamadosClient;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChamadosClientHttp implements ChamadosClient {

    private static final Logger log = LoggerFactory.getLogger(ChamadosClientHttp.class);

    private final RestClient restClient;
    private final ZoneId zoneId;

    public ChamadosClientHttp(RestClient restClient, ZoneId zoneId) {
        this.restClient = restClient;
        this.zoneId = zoneId != null ? zoneId : ZoneId.of("America/Sao_Paulo");
    }

    @Override
    public List<ChamadoConcluido> listarConcluidos(YearMonth periodo) {
        List<ChamadoConcluido> resultado = new ArrayList<>();

        ZonedDateTime inicioMes = periodo.atDay(1).atStartOfDay(zoneId);
        ZonedDateTime fimMes = periodo.atEndOfMonth().atTime(23, 59, 59, 999_999_999).atZone(zoneId);
        Instant resolvidoDe = inicioMes.toInstant();
        Instant resolvidoAte = fimMes.toInstant();

        String cookieHeader = extrairHeaderDaRequisicao("Cookie");
        String authHeader = extrairHeaderDaRequisicao("Authorization");

        int pagina = 0;
        int tamanho = 200;

        try {
            while (true) {
                final int paginaAtual = pagina;
                PaginaDto resposta = restClient.get()
                        .uri(uriBuilder -> uriBuilder.path("/api/chamados")
                                .queryParam("status", "RESOLVIDO,FECHADO")
                                .queryParam("resolvidoDe", resolvidoDe.toString())
                                .queryParam("resolvidoAte", resolvidoAte.toString())
                                .queryParam("pagina", paginaAtual)
                                .queryParam("tamanho", tamanho)
                                .build())
                        .headers(headers -> {
                            if (cookieHeader != null) {
                                headers.set("Cookie", cookieHeader);
                            }
                            if (authHeader != null) {
                                headers.set("Authorization", authHeader);
                            }
                        })
                        .retrieve()
                        .body(PaginaDto.class);

                if (resposta == null || resposta.itens() == null || resposta.itens().isEmpty()) {
                    break;
                }

                for (ChamadoConcluidoDto dto : resposta.itens()) {
                    resultado.add(new ChamadoConcluido(
                            dto.protocolo(),
                            dto.categoria(),
                            dto.prioridade(),
                            dto.departamentoNome(),
                            dto.atendenteId(),
                            dto.atendenteNome(),
                            dto.criadoEm(),
                            dto.atribuidoEm(),
                            dto.resolvidoEm(),
                            dto.prazoSla(),
                            dto.horasTrabalhadas(),
                            dto.custo()
                    ));
                }

                if (resultado.size() >= resposta.total() || resposta.itens().size() < tamanho) {
                    break;
                }
                pagina++;
            }
        } catch (Exception e) {
            log.error("Erro ao consultar chamados no Gateway: {}", e.getMessage());
            throw new ServicoIndisponivelException("Servico de chamados ou Gateway indisponivel.", e);
        }

        return resultado;
    }

    private String extrairHeaderDaRequisicao(String nomeHeader) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            if (request != null) {
                return request.getHeader(nomeHeader);
            }
        }
        return null;
    }
}
