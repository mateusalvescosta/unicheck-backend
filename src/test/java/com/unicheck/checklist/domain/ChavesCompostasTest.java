package com.unicheck.checklist.domain;

import com.unicheck.gameficacao.domain.AlunoSeloPK;
import com.unicheck.usuario.domain.Aluno;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChavesCompostasTest {
    @Test
    void chavesCompostasAtendemContratoDeSerializacaoDaJpa() {
        assertInstanceOf(Serializable.class, new ChecklistConcluidoPK());
        assertInstanceOf(Serializable.class, new ProgressoItemPK());
        assertInstanceOf(Serializable.class, new ChecklistPrerequisitoPK());
        assertInstanceOf(Serializable.class, new AlunoSeloPK());
    }

    @Test
    void igualdadeUsaIdsPersistidosMesmoComInstanciasDiferentes() {
        Aluno alunoA = new Aluno(); alunoA.setId(1L);
        Aluno alunoB = new Aluno(); alunoB.setId(1L);
        ItemChecklist itemA = new ItemChecklist(); itemA.setId(2L);
        ItemChecklist itemB = new ItemChecklist(); itemB.setId(2L);
        ProgressoItemPK primeira = new ProgressoItemPK(); primeira.setAluno(alunoA); primeira.setItem(itemA);
        ProgressoItemPK segunda = new ProgressoItemPK(); segunda.setAluno(alunoB); segunda.setItem(itemB);

        assertEquals(primeira, segunda);
        assertEquals(primeira.hashCode(), segunda.hashCode());
    }
}
