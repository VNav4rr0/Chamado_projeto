package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.Atendente;

import java.util.List;
import java.util.Optional;

public interface AtendenteRepository {
    Atendente save(Atendente atendente);
    Optional<Atendente> findById(String id);
    List<Atendente> listar(String departamentoId);
    boolean reservarCapacidade(String atendenteId);
    void liberarCapacidade(String atendenteId);
}
