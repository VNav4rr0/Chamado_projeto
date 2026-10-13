package com.suporteti.atendimento.repository;

import com.suporteti.atendimento.model.Departamento;

import java.util.List;
import java.util.Optional;

public interface DepartamentoRepository {
    Departamento save(Departamento departamento);
    Optional<Departamento> findById(String id);
    List<Departamento> listar();
    Optional<Departamento> buscarPorCategoria(String categoria);
}
