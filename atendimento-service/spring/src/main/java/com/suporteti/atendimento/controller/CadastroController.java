package com.suporteti.atendimento.controller;

import com.suporteti.atendimento.controller.adapter.AtendimentoControllerAdapter;
import com.suporteti.atendimento.controller.dto.request.AtendenteRequest;
import com.suporteti.atendimento.controller.dto.request.DepartamentoRequest;
import com.suporteti.atendimento.controller.dto.request.SlaRequest;
import com.suporteti.atendimento.controller.dto.response.AtendenteResponse;
import com.suporteti.atendimento.controller.dto.response.DepartamentoResponse;
import com.suporteti.atendimento.controller.dto.response.SlaResponse;
import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.model.UsuarioLogado;
import com.suporteti.atendimento.service.CadastroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/atendimento")
public class CadastroController {

    private final CadastroService cadastroService;

    public CadastroController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    // --- Categorias ---

    @GetMapping("/categorias")
    public ResponseEntity<List<String>> listarCategorias() {
        return ResponseEntity.ok(cadastroService.listarCategorias());
    }

    // --- Departamentos ---

    @GetMapping("/departamentos")
    public ResponseEntity<List<DepartamentoResponse>> listarDepartamentos() {
        List<DepartamentoResponse> resposta = cadastroService.listarDepartamentos().stream()
                .map(AtendimentoControllerAdapter::paraResposta)
                .toList();
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/departamentos")
    public ResponseEntity<DepartamentoResponse> criarDepartamento(@Valid @RequestBody DepartamentoRequest request) {
        Departamento criado = cadastroService.criarDepartamento(request.nome(), request.categorias());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AtendimentoControllerAdapter.paraResposta(criado));
    }

    @PutMapping("/departamentos/{id}")
    public ResponseEntity<DepartamentoResponse> atualizarDepartamento(
            @PathVariable String id,
            @RequestBody DepartamentoRequest request
    ) {
        Departamento atualizado = cadastroService.atualizarDepartamento(id, request.nome(), request.categorias(), request.ativo());
        return ResponseEntity.ok(AtendimentoControllerAdapter.paraResposta(atualizado));
    }

    // --- Atendentes ---

    @GetMapping("/atendentes")
    public ResponseEntity<List<AtendenteResponse>> listarAtendentes(
            @RequestParam(required = false) String departamentoId
    ) {
        List<AtendenteResponse> resposta = cadastroService.listarAtendentes(departamentoId).stream()
                .map(AtendimentoControllerAdapter::paraResposta)
                .toList();
        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/atendentes")
    public ResponseEntity<AtendenteResponse> criarAtendente(
            UsuarioLogado usuarioLogado,
            @Valid @RequestBody AtendenteRequest request
    ) {
        String idFinal = (request.id() != null && !request.id().isBlank())
                ? request.id()
                : (usuarioLogado != null ? usuarioLogado.id() : null);

        int capacidade = request.capacidade() != null ? request.capacidade() : 5;

        Atendente criado = cadastroService.criarAtendente(idFinal, request.nome(), request.departamentoId(), capacidade);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AtendimentoControllerAdapter.paraResposta(criado));
    }

    @PutMapping("/atendentes/{id}")
    public ResponseEntity<AtendenteResponse> atualizarAtendente(
            @PathVariable String id,
            @RequestBody AtendenteRequest request
    ) {
        Atendente atualizado = cadastroService.atualizarAtendente(
                id, request.nome(), request.departamentoId(), request.capacidade(), request.ativo()
        );
        return ResponseEntity.ok(AtendimentoControllerAdapter.paraResposta(atualizado));
    }

    // --- SLAs ---

    @GetMapping("/slas")
    public ResponseEntity<List<SlaResponse>> listarSlas() {
        List<SlaResponse> resposta = cadastroService.listarSlas().stream()
                .map(AtendimentoControllerAdapter::paraResposta)
                .toList();
        return ResponseEntity.ok(resposta);
    }

    @PutMapping("/slas/{prioridade}")
    public ResponseEntity<SlaResponse> atualizarSla(
            @PathVariable String prioridade,
            @Valid @RequestBody SlaRequest request
    ) {
        Sla salvo = cadastroService.salvarSla(prioridade, request.horas());
        return ResponseEntity.ok(AtendimentoControllerAdapter.paraResposta(salvo));
    }
}
