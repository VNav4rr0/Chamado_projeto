package com.suporteti.atendimento.service;

import com.suporteti.atendimento.event.EventoSaida;
import com.suporteti.atendimento.exception.AcessoNegadoException;
import com.suporteti.atendimento.exception.RecursoNaoEncontradoException;
import com.suporteti.atendimento.exception.RegraDeNegocioException;
import com.suporteti.atendimento.model.Atendente;
import com.suporteti.atendimento.model.ChamadoRecebido;
import com.suporteti.atendimento.model.Departamento;
import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.Sla;
import com.suporteti.atendimento.model.UsuarioLogado;
import com.suporteti.atendimento.model.enumerable.StatusItemFila;
import com.suporteti.atendimento.port.PublicadorEventos;
import com.suporteti.atendimento.port.TransacaoExecutor;
import com.suporteti.atendimento.repository.AtendenteRepository;
import com.suporteti.atendimento.repository.DepartamentoRepository;
import com.suporteti.atendimento.repository.FilaRepository;
import com.suporteti.atendimento.repository.SlaRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class FilaService {

    private final FilaRepository filaRepository;
    private final AtendenteRepository atendenteRepository;
    private final DepartamentoRepository departamentoRepository;
    private final SlaRepository slaRepository;
    private final PublicadorEventos publicadorEventos;
    private final TransacaoExecutor transacaoExecutor;

    public FilaService(
            FilaRepository filaRepository,
            AtendenteRepository atendenteRepository,
            DepartamentoRepository departamentoRepository,
            SlaRepository slaRepository,
            PublicadorEventos publicadorEventos,
            TransacaoExecutor transacaoExecutor
    ) {
        this.filaRepository = filaRepository;
        this.atendenteRepository = atendenteRepository;
        this.departamentoRepository = departamentoRepository;
        this.slaRepository = slaRepository;
        this.publicadorEventos = publicadorEventos;
        this.transacaoExecutor = transacaoExecutor;
    }

    public ItemFila triar(ChamadoRecebido chamado) {
        if (chamado == null) {
            throw new RegraDeNegocioException("Dados do chamado recebido sao obrigatorios.");
        }

        return transacaoExecutor.executar(() -> {
            Optional<Departamento> depOpt = departamentoRepository.buscarPorCategoria(chamado.categoria());
            if (depOpt.isEmpty() || !depOpt.get().ativo()) {
                String motivo = "Categoria inexistente ou inativa: " + chamado.categoria();
                ItemFila rejeitado = new ItemFila(
                        null, chamado.chamadoId(), chamado.protocolo(), chamado.categoria(),
                        chamado.prioridade(), null, StatusItemFila.REJEITADO, null,
                        null, chamado.criadoEm(), null, null, motivo, null
                );
                ItemFila salvo = filaRepository.save(rejeitado);
                publicadorEventos.publicar(new EventoSaida.ChamadoTriado(
                        chamado.chamadoId(), "REJEITADO", null, null, null, motivo
                ));
                return salvo;
            }

            Departamento departamento = depOpt.get();
            Optional<Sla> slaOpt = slaRepository.findByPrioridade(chamado.prioridade());
            if (slaOpt.isEmpty()) {
                String motivo = "Prioridade sem SLA configurado: " + chamado.prioridade();
                ItemFila rejeitado = new ItemFila(
                        null, chamado.chamadoId(), chamado.protocolo(), chamado.categoria(),
                        chamado.prioridade(), departamento.id(), StatusItemFila.REJEITADO, null,
                        null, chamado.criadoEm(), null, null, motivo, null
                );
                ItemFila salvo = filaRepository.save(rejeitado);
                publicadorEventos.publicar(new EventoSaida.ChamadoTriado(
                        chamado.chamadoId(), "REJEITADO", departamento.id(), departamento.nome(), null, motivo
                ));
                return salvo;
            }

            Sla sla = slaOpt.get();
            Instant prazoSla = chamado.criadoEm().plus(Duration.ofHours(sla.horas()));

            ItemFila naFila = new ItemFila(
                    null, chamado.chamadoId(), chamado.protocolo(), chamado.categoria(),
                    chamado.prioridade(), departamento.id(), StatusItemFila.NA_FILA, null,
                    prazoSla, chamado.criadoEm(), null, null, null, null
            );
            ItemFila salvo = filaRepository.save(naFila);
            publicadorEventos.publicar(new EventoSaida.ChamadoTriado(
                    chamado.chamadoId(), "ACEITO", departamento.id(), departamento.nome(), prazoSla, null
            ));
            return salvo;
        });
    }

    public ItemFila assumir(UsuarioLogado usuario, String chamadoId) {
        if (usuario == null || usuario.id() == null || usuario.id().isBlank()) {
            throw new AcessoNegadoException("Nao autenticado.");
        }

        ItemFila item = buscarPorChamadoId(chamadoId);
        if (item.status() != StatusItemFila.NA_FILA) {
            throw new RegraDeNegocioException("Chamado nao esta disponivel na fila de atendimento.");
        }

        Atendente atendente = atendenteRepository.findById(usuario.id())
                .orElseThrow(() -> new RegraDeNegocioException("Usuario nao possui perfil de atendente cadastrado."));

        if (!atendente.ativo()) {
            throw new RegraDeNegocioException("Perfil de atendente inativo.");
        }

        if (!atendente.departamentoId().equals(item.departamentoId())) {
            throw new RegraDeNegocioException("Atendente pertence a outro departamento.");
        }

        return transacaoExecutor.executar(() -> {
            boolean reservou = atendenteRepository.reservarCapacidade(atendente.id());
            if (!reservou) {
                throw new RegraDeNegocioException("Capacidade do atendente esgotada.");
            }

            Instant agora = Instant.now();
            Optional<ItemFila> reivindicado = filaRepository.reivindicar(
                    chamadoId, item.departamentoId(), atendente.id(), agora
            );

            if (reivindicado.isEmpty()) {
                throw new RegraDeNegocioException("Chamado ja assumido ou indisponivel.");
            }

            publicadorEventos.publicar(new EventoSaida.ChamadoAtribuido(
                    chamadoId, atendente.id(), atendente.nome(), agora
            ));

            return reivindicado.get();
        });
    }

    public void concluir(String chamadoId, String atendenteId) {
        transacaoExecutor.executar(() -> {
            Instant agora = Instant.now();
            filaRepository.concluir(chamadoId, agora);
            if (atendenteId != null && !atendenteId.isBlank()) {
                atendenteRepository.liberarCapacidade(atendenteId);
            }
        });
    }

    public List<ItemFila> listar(String departamentoId, StatusItemFila status) {
        return filaRepository.listar(departamentoId, status);
    }

    public ItemFila buscarPorChamadoId(String chamadoId) {
        return filaRepository.buscarPorChamadoId(chamadoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item da fila nao encontrado para o chamado: " + chamadoId));
    }
}
