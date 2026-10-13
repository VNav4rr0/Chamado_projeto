package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.ItemFila;
import com.suporteti.atendimento.model.enumerable.StatusItemFila;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FilaRepository {
    ItemFila save(ItemFila item);
    Optional<ItemFila> buscarPorChamadoId(String chamadoId);
    List<ItemFila> listar(String departamentoId, StatusItemFila status);
    Optional<ItemFila> reivindicar(String chamadoId, String departamentoId, String atendenteId, Instant instante);
    void concluir(String chamadoId, Instant instante);
}
