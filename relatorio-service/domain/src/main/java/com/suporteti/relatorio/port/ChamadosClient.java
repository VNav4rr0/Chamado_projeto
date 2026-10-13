package com.suporteti.relatorio.port;

import com.suporteti.relatorio.model.ChamadoConcluido;

import java.time.YearMonth;
import java.util.List;

public interface ChamadosClient {
    List<ChamadoConcluido> listarConcluidos(YearMonth periodo);
}
