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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class ChamadoService {

    private final ChamadoRepository chamadoRepository;
    private final PublicadorEventos publicadorEventos;
    private final TransacaoExecutor transacaoExecutor;
    private final GeradorProtocolo geradorProtocolo;
    private final Clock clock;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            PublicadorEventos publicadorEventos,
            TransacaoExecutor transacaoExecutor,
            GeradorProtocolo geradorProtocolo,
            Clock clock
    ) {
        this.chamadoRepository = chamadoRepository;
        this.publicadorEventos = publicadorEventos;
        this.transacaoExecutor = transacaoExecutor;
        this.geradorProtocolo = geradorProtocolo;
        this.clock = clock;
    }

    public Chamado abrir(
            UsuarioLogado usuario,
            String titulo,
            String descricao,
            String categoria,
            Prioridade prioridade
    ) {
        if (usuario == null) {
            throw new AcessoNegadoException("Usuario nao autenticado.");
        }
        if (titulo == null || titulo.isBlank()) {
            throw new RegraDeNegocioException("Titulo e obrigatorio.");
        }
        if (descricao == null || descricao.isBlank()) {
            throw new RegraDeNegocioException("Descricao e obrigatoria.");
        }
        if (categoria == null || categoria.isBlank()) {
            throw new RegraDeNegocioException("Categoria e obrigatoria.");
        }
        if (prioridade == null) {
            throw new RegraDeNegocioException("Prioridade e obrigatoria.");
        }

        Instant agora = clock.instant();
        String protocolo = geradorProtocolo.proximo(agora);

        return transacaoExecutor.executar(() -> {
            Chamado novo = new Chamado(
                    null,
                    protocolo,
                    titulo.trim(),
                    descricao.trim(),
                    categoria.trim(),
                    prioridade,
                    StatusChamado.CRIADO,
                    usuario.id(),
                    usuario.nome(),
                    null,
                    null,
                    null,
                    null,
                    null,
                    agora,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    List.of(new MudancaStatus(null, StatusChamado.CRIADO, usuario.id(), agora, "Abertura de chamado")),
                    null
            );

            Chamado salvo = chamadoRepository.save(novo);

            publicadorEventos.publicar(new EventoSaida.ChamadoCriado(
                    salvo.id(),
                    salvo.protocolo(),
                    salvo.categoria(),
                    salvo.prioridade().name(),
                    salvo.titulo(),
                    salvo.solicitanteId(),
                    salvo.criadoEm()
            ));

            return salvo;
        });
    }

    public Pagina<Chamado> listar(UsuarioLogado usuario, FiltroChamados filtro) {
        if (usuario == null) {
            throw new AcessoNegadoException("Usuario nao autenticado.");
        }
        FiltroChamados filtroAplicado = filtro != null ? filtro : new FiltroChamados(null, null, null, null, null, 0, 10);
        if (!usuario.admin()) {
            filtroAplicado = filtroAplicado.comSolicitanteId(usuario.id());
        }
        return chamadoRepository.buscar(filtroAplicado);
    }

    public Chamado consultar(UsuarioLogado usuario, String id) {
        if (usuario == null) {
            throw new AcessoNegadoException("Usuario nao autenticado.");
        }
        Chamado chamado = chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado nao encontrado."));

        boolean solicitante = usuario.id().equals(chamado.solicitanteId());
        boolean atendente = usuario.id().equals(chamado.atendenteId());

        if (!usuario.admin() && !solicitante && !atendente) {
            throw new AcessoNegadoException("Acesso negado ao chamado.");
        }
        return chamado;
    }

    public Chamado alterarStatus(
            UsuarioLogado usuario,
            String id,
            StatusChamado novoStatus,
            BigDecimal horasTrabalhadas,
            BigDecimal custo,
            String motivo
    ) {
        if (usuario == null) {
            throw new AcessoNegadoException("Usuario nao autenticado.");
        }
        if (novoStatus == null) {
            throw new RegraDeNegocioException("Novo status e obrigatorio.");
        }

        return transacaoExecutor.executar(() -> {
            Chamado chamado = chamadoRepository.findById(id)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado nao encontrado."));

            if (!chamado.status().podeIrPara(novoStatus)) {
                throw new RegraDeNegocioException("Transicao de " + chamado.status() + " para " + novoStatus + " nao permitida.");
            }

            if (novoStatus == StatusChamado.FECHADO) {
                boolean solicitante = usuario.id().equals(chamado.solicitanteId());
                if (!usuario.admin() && !solicitante) {
                    throw new AcessoNegadoException("Somente o solicitante ou administrador pode fechar o chamado.");
                }
            } else if (novoStatus == StatusChamado.AGUARDANDO_TERCEIRO
                    || novoStatus == StatusChamado.EM_ANDAMENTO
                    || novoStatus == StatusChamado.RESOLVIDO
                    || novoStatus == StatusChamado.REJEITADO) {

                boolean atendenteAtribuido = usuario.id().equals(chamado.atendenteId());
                if (!atendenteAtribuido) {
                    throw new AcessoNegadoException("Somente o atendente atribuido pode alterar este status.");
                }

                if (novoStatus == StatusChamado.RESOLVIDO) {
                    if (horasTrabalhadas == null || horasTrabalhadas.compareTo(BigDecimal.ZERO) < 0
                            || custo == null || custo.compareTo(BigDecimal.ZERO) < 0) {
                        throw new RegraDeNegocioException("Horas trabalhadas e custo sao obrigatorios para resolver o chamado.");
                    }
                } else if (novoStatus == StatusChamado.REJEITADO) {
                    if (motivo == null || motivo.isBlank()) {
                        throw new RegraDeNegocioException("Motivo e obrigatorio para rejeitar o chamado.");
                    }
                }
            }

            Instant agora = clock.instant();
            Chamado atualizado = chamado.comStatus(
                    novoStatus,
                    usuario.id(),
                    agora,
                    horasTrabalhadas,
                    custo,
                    motivo,
                    "Status alterado para " + novoStatus + " por " + usuario.nome()
            );

            Chamado salvo = chamadoRepository.save(atualizado);

            if (novoStatus == StatusChamado.RESOLVIDO) {
                publicadorEventos.publicar(new EventoSaida.ChamadoResolvido(salvo.id(), salvo.atendenteId(), agora));
            } else if (novoStatus == StatusChamado.REJEITADO) {
                publicadorEventos.publicar(new EventoSaida.ChamadoRejeitado(salvo.id(), salvo.atendenteId(), motivo));
            }

            return salvo;
        });
    }

    public void aplicarTriagem(ResultadoTriagem triagem) {
        if (triagem == null || triagem.chamadoId() == null) {
            throw new RegraDeNegocioException("Dados de triagem invalidos.");
        }

        transacaoExecutor.executar(() -> {
            Chamado chamado = chamadoRepository.findById(triagem.chamadoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado nao encontrado."));

            boolean aceito = "ACEITO".equalsIgnoreCase(triagem.resultado());
            StatusChamado destino = aceito ? StatusChamado.EM_ANALISE : StatusChamado.REJEITADO;

            if (!chamado.status().podeIrPara(destino)) {
                throw new RegraDeNegocioException("Transicao de " + chamado.status() + " para " + destino + " nao permitida.");
            }

            Instant agora = clock.instant();
            Chamado atualizado = chamado.comTriagem(triagem, agora);
            chamadoRepository.save(atualizado);
        });
    }

    public void aplicarAtribuicao(Atribuicao atribuicao) {
        if (atribuicao == null || atribuicao.chamadoId() == null) {
            throw new RegraDeNegocioException("Dados de atribuicao invalidos.");
        }

        transacaoExecutor.executar(() -> {
            Chamado chamado = chamadoRepository.findById(atribuicao.chamadoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado nao encontrado."));

            if (!chamado.status().podeIrPara(StatusChamado.EM_ANDAMENTO)) {
                throw new RegraDeNegocioException("Transicao de " + chamado.status() + " para EM_ANDAMENTO nao permitida.");
            }

            Instant agora = clock.instant();
            Chamado atualizado = chamado.comAtribuicao(atribuicao, agora);
            chamadoRepository.save(atualizado);
        });
    }
}
