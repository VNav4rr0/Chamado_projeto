package com.suporteti.chamado.service;

import com.suporteti.chamado.model.enumerable.StatusChamado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StatusChamadoTest {

    @Test
    void deveValidarTransicoesValidas() {
        // CRIADO -> EM_ANALISE, REJEITADO
        assertTrue(StatusChamado.CRIADO.podeIrPara(StatusChamado.EM_ANALISE));
        assertTrue(StatusChamado.CRIADO.podeIrPara(StatusChamado.REJEITADO));

        // EM_ANALISE -> EM_ANDAMENTO
        assertTrue(StatusChamado.EM_ANALISE.podeIrPara(StatusChamado.EM_ANDAMENTO));

        // EM_ANDAMENTO -> AGUARDANDO_TERCEIRO, RESOLVIDO, REJEITADO
        assertTrue(StatusChamado.EM_ANDAMENTO.podeIrPara(StatusChamado.AGUARDANDO_TERCEIRO));
        assertTrue(StatusChamado.EM_ANDAMENTO.podeIrPara(StatusChamado.RESOLVIDO));
        assertTrue(StatusChamado.EM_ANDAMENTO.podeIrPara(StatusChamado.REJEITADO));

        // AGUARDANDO_TERCEIRO -> EM_ANDAMENTO, RESOLVIDO
        assertTrue(StatusChamado.AGUARDANDO_TERCEIRO.podeIrPara(StatusChamado.EM_ANDAMENTO));
        assertTrue(StatusChamado.AGUARDANDO_TERCEIRO.podeIrPara(StatusChamado.RESOLVIDO));

        // RESOLVIDO -> FECHADO
        assertTrue(StatusChamado.RESOLVIDO.podeIrPara(StatusChamado.FECHADO));
    }

    @Test
    void deveInvalidarTransicoesInvalidas() {
        // Nulo
        assertFalse(StatusChamado.CRIADO.podeIrPara(null));

        // De CRIADO nao pode pular para EM_ANDAMENTO, RESOLVIDO, FECHADO
        assertFalse(StatusChamado.CRIADO.podeIrPara(StatusChamado.EM_ANDAMENTO));
        assertFalse(StatusChamado.CRIADO.podeIrPara(StatusChamado.RESOLVIDO));
        assertFalse(StatusChamado.CRIADO.podeIrPara(StatusChamado.FECHADO));

        // De EM_ANALISE nao pode ir direto para RESOLVIDO ou FECHADO
        assertFalse(StatusChamado.EM_ANALISE.podeIrPara(StatusChamado.RESOLVIDO));
        assertFalse(StatusChamado.EM_ANALISE.podeIrPara(StatusChamado.FECHADO));

        // De RESOLVIDO nao pode voltar para EM_ANDAMENTO
        assertFalse(StatusChamado.RESOLVIDO.podeIrPara(StatusChamado.EM_ANDAMENTO));

        // Terminais nao podem ir para lugar nenhum
        for (StatusChamado status : StatusChamado.values()) {
            assertFalse(StatusChamado.FECHADO.podeIrPara(status));
            assertFalse(StatusChamado.REJEITADO.podeIrPara(status));
        }
    }
}
