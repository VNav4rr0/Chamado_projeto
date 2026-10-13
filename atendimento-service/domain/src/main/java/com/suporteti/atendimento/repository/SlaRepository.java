package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.Sla;

import java.util.List;
import java.util.Optional;

public interface SlaRepository {
    Sla save(Sla sla);
    Optional<Sla> findByPrioridade(String prioridade);
    List<Sla> listar();
}
