package com.unicheck.categoria.service;

import com.unicheck.categoria.domain.Categoria;
import com.unicheck.categoria.dto.CategoriaRequestDTO;
import com.unicheck.categoria.dto.CategoriaResponseDTO;
import com.unicheck.categoria.repository.CategoriaRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @apiNote Testes unitários da CategoriaService, com o repository mockado.
 *
 *          O foco é a regra de desativação: a categoria sai da lista de escolha sem ser
 *          removida, porque os benefícios já vinculados a ela continuam valendo.
 * @author Mateus Alves Costa
 * @since 19.09.2026
 */
@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {
    @Mock private CategoriaRepository repository;
    @InjectMocks private CategoriaService service;

    /**
     * @apiNote A listagem de administração enxerga também a desativada, com o estado no DTO,
     *          para que a tela possa oferecer a reativação.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void findAllDevolveAtivasEDesativadasComOEstadoNoDto() {
        when(repository.findAllByOrderByNomeAsc())
                .thenReturn(List.of(categoria(1L, "Alimentacao", true), categoria(2L, "Antiga", false)));

        List<CategoriaResponseDTO> retorno = service.findAll();

        assertEquals(2, retorno.size());
        assertTrue(retorno.get(0).ativo());
        assertFalse(retorno.get(1).ativo());
        assertEquals("Antiga", retorno.get(1).nome());
    }

    /**
     * @apiNote O filtro de ativas é responsabilidade do banco; filtrar em memória quebraria
     *          uma paginação futura.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void findAllAtivasUsaAConsultaFiltradaENaoAListaCompleta() {
        when(repository.findAllByAtivoTrueOrderByNomeAsc())
                .thenReturn(List.of(categoria(1L, "Alimentacao", true)));

        List<CategoriaResponseDTO> retorno = service.findAllAtivas();

        assertEquals(1, retorno.size());
        assertEquals("Alimentacao", retorno.get(0).nome());
        verify(repository, never()).findAllByOrderByNomeAsc();
    }

    /**
     * @apiNote ID inexistente resulta em 404 com o id na mensagem, e não em erro genérico.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void findByIdInexistenteInformaOIdNaMensagem() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class, () -> service.findById(99L));

        assertEquals("Categoria não encontrada com id: 99", erro.getMessage());
    }

    /**
     * @apiNote Categoria recém-criada já pode ser escolhida no cadastro de benefício, e o nome
     *          chega ao banco sem espaços nas pontas.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void createSalvaComNomeSemEspacosENasceAtiva() {
        when(repository.existsByNomeIgnoreCase("  Transporte  ")).thenReturn(false);
        when(repository.save(any(Categoria.class))).thenAnswer(invocacao -> {
            Categoria salva = invocacao.getArgument(0);
            salva.setId(7L);
            return salva;
        });

        CategoriaResponseDTO retorno = service.create(new CategoriaRequestDTO("  Transporte  "));

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(repository).save(captor.capture());
        assertEquals("Transporte", captor.getValue().getNome());
        assertTrue(captor.getValue().isAtivo());
        assertEquals(7L, retorno.id());
    }

    /**
     * @apiNote Nome duplicado é conflito, cita o nome na mensagem e não chega a salvar.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void createComNomeRepetidoCitaONomeNaMensagemENaoSalva() {
        when(repository.existsByNomeIgnoreCase("Transporte")).thenReturn(true);

        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> service.create(new CategoriaRequestDTO("Transporte")));

        assertEquals("Já existe uma categoria com o nome 'Transporte'.", erro.getMessage());
        verify(repository, never()).save(any());
    }

    /**
     * @apiNote Renomear não pode reativar pelas costas; para isso existe o endpoint de ativar.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void updateNaoAlteraOEstadoAtivo() {
        Categoria desativada = categoria(3L, "Antiga", false);
        when(repository.findById(3L)).thenReturn(Optional.of(desativada));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Novo Nome", 3L)).thenReturn(false);
        when(repository.save(any(Categoria.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CategoriaResponseDTO retorno = service.update(3L, new CategoriaRequestDTO("Novo Nome"));

        assertEquals("Novo Nome", retorno.nome());
        assertFalse(retorno.ativo());
    }

    /**
     * @apiNote Nome já usado por outra categoria é conflito e não chega a salvar.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void updateComNomeDeOutraCategoriaNaoSalva() {
        when(repository.findById(3L)).thenReturn(Optional.of(categoria(3L, "Antiga", true)));
        when(repository.existsByNomeIgnoreCaseAndIdNot("Transporte", 3L)).thenReturn(true);

        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> service.update(3L, new CategoriaRequestDTO("Transporte")));

        assertEquals("Já existe outra categoria com o nome 'Transporte'.", erro.getMessage());
        verify(repository, never()).save(any());
    }

    /**
     * @apiNote Desativar vira a flag e não remove a linha, porque os benefícios já vinculados
     *          dependem dela continuar existindo.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void desativarApenasViraAFlagENaoRemoveNada() {
        Categoria ativa = categoria(1L, "Alimentacao", true);
        when(repository.findById(1L)).thenReturn(Optional.of(ativa));
        when(repository.save(any(Categoria.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CategoriaResponseDTO retorno = service.desativar(1L);

        assertFalse(retorno.ativo());
        assertEquals(1L, retorno.id());
        verify(repository, never()).delete(any());
        verify(repository, never()).deleteById(any());
    }

    /**
     * @apiNote Desativar o que já está desativado é conflito, com o nome na mensagem.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void desativarDuasVezesEConflitoECitaONome() {
        when(repository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Alimentacao", false)));

        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> service.desativar(1L));

        assertEquals("A categoria 'Alimentacao' já está desativada.", erro.getMessage());
        verify(repository, never()).save(any());
    }

    /**
     * @apiNote Ativar devolve a categoria à lista oferecida no cadastro de benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void ativarDevolveACategoriaDisponivelDeNovo() {
        when(repository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Alimentacao", false)));
        when(repository.save(any(Categoria.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        CategoriaResponseDTO retorno = service.ativar(1L);

        assertTrue(retorno.ativo());
    }

    /**
     * @apiNote Ativar o que já está ativo é conflito, com o nome na mensagem.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void ativarOQueJaEstaAtivoEConflito() {
        when(repository.findById(1L)).thenReturn(Optional.of(categoria(1L, "Alimentacao", true)));

        DataIntegrityViolationException erro = assertThrows(DataIntegrityViolationException.class,
                () -> service.ativar(1L));

        assertEquals("A categoria 'Alimentacao' já está ativa.", erro.getMessage());
        verify(repository, never()).save(any());
    }

    /**
     * @apiNote Em id inexistente a resposta é 404, e não o conflito de estado: a categoria
     *          nem chega a ser avaliada.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void ativarEDesativarDeIdInexistenteSao404ENaoConflito() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.ativar(99L));
        assertThrows(EntityNotFoundException.class, () -> service.desativar(99L));
    }

    /**
     * @apiNote Remoção de id inexistente falha antes de tocar o banco.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void deleteDeIdInexistenteNaoChamaORepositorio() {
        when(repository.existsById(99L)).thenReturn(false);

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class, () -> service.delete(99L));

        assertEquals("Categoria não encontrada com id: 99", erro.getMessage());
        verify(repository, never()).deleteById(any());
    }

    /**
     * @apiNote Remoção de id existente delega ao repository.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void deleteDeIdExistenteRemove() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    /**
     * @apiNote Monta uma Categoria de teste.
     * @param id --> Identificador da categoria.
     * @param nome --> Nome da categoria.
     * @param ativo --> Estado desejado.
     * @return --> Retorna a categoria montada.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private Categoria categoria(Long id, String nome, boolean ativo) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setNome(nome);
        categoria.setAtivo(ativo);
        return categoria;
    }
}
