package com.suporteti.chamado.repository;

import com.suporteti.chamado.model.Chamado;
import com.suporteti.chamado.model.FiltroChamados;
import com.suporteti.chamado.model.Pagina;

import java.util.Optional;

public interface ChamadoRepository {
    Chamado save(Chamado chamado);
    Optional<Chamado> findById(String id);
    Pagina<Chamado> buscar(FiltroChamados filtro);
}
