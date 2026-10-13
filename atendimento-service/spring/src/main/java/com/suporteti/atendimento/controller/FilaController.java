package com.suporteti.atendimento.controller;

import com.suporteti.atendimento.controller.adapter.AtendimentoControllerAdapter;
import com.suporteti.atendimento.controller.dto.response.ItemFilaResponse;
import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.UsuarioLogado;
import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import com.suporteti.atendimento.service.FilaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atendimento/fila")
public class FilaController {

    private final FilaService filaService;

    public FilaController(FilaService filaService) {
        this.filaService = filaService;
    }

    @GetMapping
    public ResponseEntity<List<ItemFilaResponse>> listar(
            @RequestParam(required = false) String departamentoId,
            @RequestParam(required = false) StatusItemFila status
    ) {
        List<ItemFila> itens = filaService.listar(departamentoId, status);
        List<ItemFilaResponse> resposta = itens.stream()
                .map(AtendimentoControllerAdapter::paraResposta)
                .toList();
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/{chamadoId}/assumir")
    public ResponseEntity<ItemFilaResponse> assumir(
            UsuarioLogado usuarioLogado,
            @PathVariable String chamadoId
    ) {
        ItemFila assumido = filaService.assumir(usuarioLogado, chamadoId);
        return ResponseEntity.ok(AtendimentoControllerAdapter.paraResposta(assumido));
    }
}
