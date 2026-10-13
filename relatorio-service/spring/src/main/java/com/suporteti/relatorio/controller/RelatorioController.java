package com.suporteti.relatorio.controller;

import com.suporteti.relatorio.controller.adapter.RelatorioControllerAdapter;
import com.suporteti.relatorio.controller.dto.response.RelatorioMensalResponse;
import com.suporteti.relatorio.exception.AcessoNegadoException;
import com.suporteti.relatorio.model.RelatorioMensal;
import com.suporteti.relatorio.service.RelatorioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/relatorio")
public class RelatorioController {

    private final RelatorioService relatorioService;
    private final ZoneId zoneId;

    public RelatorioController(RelatorioService relatorioService, ZoneId zoneId) {
        this.relatorioService = relatorioService;
        this.zoneId = zoneId != null ? zoneId : ZoneId.of("America/Sao_Paulo");
    }

    @GetMapping("/mensal")
    public ResponseEntity<RelatorioMensalResponse> obterRelatorioMensal(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes,
            @RequestHeader(value = "X-Usuario-Role", required = false) String role
    ) {
        boolean ehAdmin = role != null && role.toUpperCase().contains("ADMIN");
        if (!ehAdmin) {
            throw new AcessoNegadoException("Apenas administradores podem acessar o relatorio.");
        }

        YearMonth periodo;
        if (ano != null && mes != null) {
            periodo = YearMonth.of(ano, mes);
        } else {
            periodo = YearMonth.now(zoneId);
        }

        RelatorioMensal relatorio = relatorioService.gerar(true, periodo);
        return ResponseEntity.ok(RelatorioControllerAdapter.paraResponse(relatorio));
    }
}
