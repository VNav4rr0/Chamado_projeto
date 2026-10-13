package com.suporteti.chamado.controller;

import com.suporteti.chamado.controller.adapter.ChamadoControllerAdapter;
import com.suporteti.chamado.controller.dto.request.AbrirChamadoRequest;
import com.suporteti.chamado.controller.dto.request.AlterarStatusRequest;
import com.suporteti.chamado.controller.dto.response.ChamadoResponse;
import com.suporteti.chamado.controller.dto.response.PaginaResponse;
import com.suporteti.chamado.model.Chamado;
import com.suporteti.chamado.model.FiltroChamados;
import com.suporteti.chamado.model.Pagina;
import com.suporteti.chamado.model.UsuarioLogado;
import com.suporteti.chamado.model.enumerable.StatusChamado;
import com.suporteti.chamado.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;

    public ChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(
            @Valid @RequestBody AbrirChamadoRequest request,
            UsuarioLogado usuario
    ) {
        Chamado criado = chamadoService.abrir(
                usuario,
                request.titulo(),
                request.descricao(),
                request.categoria(),
                request.prioridade()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ChamadoControllerAdapter.paraResponse(criado));
    }

    @GetMapping
    public ResponseEntity<PaginaResponse<ChamadoResponse>> listar(
            @RequestParam(required = false) List<StatusChamado> status,
            @RequestParam(required = false) String atendenteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant resolvidoDe,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant resolvidoAte,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho,
            UsuarioLogado usuario
    ) {
        FiltroChamados filtro = new FiltroChamados(null, status, atendenteId, resolvidoDe, resolvidoAte, pagina, tamanho);
        Pagina<Chamado> resultado = chamadoService.listar(usuario, filtro);
        return ResponseEntity.ok(ChamadoControllerAdapter.paraPaginaResponse(resultado));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoResponse> consultar(
            @PathVariable String id,
            UsuarioLogado usuario
    ) {
        Chamado chamado = chamadoService.consultar(usuario, id);
        return ResponseEntity.ok(ChamadoControllerAdapter.paraResponse(chamado));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ChamadoResponse> alterarStatus(
            @PathVariable String id,
            @Valid @RequestBody AlterarStatusRequest request,
            UsuarioLogado usuario
    ) {
        Chamado atualizado = chamadoService.alterarStatus(
                usuario,
                id,
                request.status(),
                request.horasTrabalhadas(),
                request.custo(),
                request.motivo()
        );
        return ResponseEntity.ok(ChamadoControllerAdapter.paraResponse(atualizado));
    }
}
