package com.unicheck.beneficio.service;

import com.unicheck.beneficio.domain.Beneficio;
import com.unicheck.beneficio.domain.PassoBeneficio;
import com.unicheck.beneficio.dto.BeneficioRequestDTO;
import com.unicheck.beneficio.dto.BeneficioResponseDTO;
import com.unicheck.beneficio.dto.BeneficioSummaryDTO;
import com.unicheck.beneficio.dto.PassoBeneficioDTO;
import com.unicheck.beneficio.enumeration.TipoBeneficio;
import com.unicheck.beneficio.repository.BeneficioFavoritoRepository;
import com.unicheck.beneficio.repository.BeneficioRepository;
import com.unicheck.categoria.domain.Categoria;
import com.unicheck.categoria.repository.CategoriaRepository;
import com.unicheck.usuario.domain.Aluno;
import com.unicheck.usuario.domain.Usuario;
import com.unicheck.usuario.repository.AlunoRepository;
import com.unicheck.usuario.service.IdentidadeService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.unicheck.exception.ResourceConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @apiNote Testes unitários da BeneficioService, com repositories e IdentidadeService mockados.
 *
 *          Cobre as decisões que não são óbvias no código: o sort da URL é descartado, o termo
 *          em branco desliga o filtro em vez de virar "%%", e categoria desativada impede novo
 *          vínculo sem esconder os benefícios que já a usam.
 * @author Mateus Alves Costa
 * @since 19.09.2026
 */
@ExtendWith(MockitoExtension.class)
class BeneficioServiceTest {
    @Mock private BeneficioRepository beneficioRepository;
    @Mock private CategoriaRepository categoriaRepository;
    @Mock private BeneficioFavoritoRepository favoritoRepository;
    @Mock private AlunoRepository alunoRepository;
    @Mock private IdentidadeService identidadeService;
    @InjectMocks private BeneficioService service;

    /**
     * @apiNote O termo chega ao SQL já em minúsculas e entre percentuais, com as pontas aparadas.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void buscarNormalizaOTermoParaMinusculasEntrePercentuais() {
        when(beneficioRepository.buscar(any(), any(), any(), any())).thenReturn(Page.empty());
        when(alunoRepository.findByUsuario_Ra("RA001")).thenReturn(Optional.empty());

        service.buscar("  BolSa  ", TipoBeneficio.GRATUITO, 3L, "RA001", PageRequest.of(0, 12));

        verify(beneficioRepository).buscar(eq("%bolsa%"), eq("GRATUITO"), eq(3L), any());
    }

    /**
     * @apiNote Termo em branco vira null, que desliga o filtro no SQL. "%%" casaria com tudo,
     *          mas mudaria o plano da consulta à toa.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void buscarComTermoEmBrancoNaoFiltraPorTexto() {
        when(beneficioRepository.buscar(any(), any(), any(), any())).thenReturn(Page.empty());
        when(alunoRepository.findByUsuario_Ra("RA001")).thenReturn(Optional.empty());

        service.buscar("   ", null, null, "RA001", PageRequest.of(0, 12));

        verify(beneficioRepository).buscar(eq(null), eq(null), eq(null), any());
    }

    /**
     * @apiNote O favorito não sai da entidade: é resolvido para a página inteira numa consulta só.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void buscarMarcaComoFavoritoApenasOQueOAlunoFavoritou() {
        Beneficio favoritado = beneficio(1L, "Bolsa");
        Beneficio comum = beneficio(2L, "Desconto");
        when(beneficioRepository.buscar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(favoritado, comum)));
        when(alunoRepository.findByUsuario_Ra("RA001")).thenReturn(Optional.of(aluno(10L)));
        when(favoritoRepository.findBeneficioIdsFavoritados(10L, List.of(1L, 2L))).thenReturn(Set.of(1L));

        Page<BeneficioSummaryDTO> retorno = service.buscar(null, null, null, "RA001", PageRequest.of(0, 12));

        assertTrue(retorno.getContent().get(0).favorito());
        assertFalse(retorno.getContent().get(1).favorito());
    }

    /**
     * @apiNote Um ADMIN autenticado não tem Aluno vinculado e mesmo assim precisa conseguir listar.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void buscarComUsuarioSemPerfilDeAlunoNaoConsultaFavoritos() {
        when(beneficioRepository.buscar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(beneficio(1L, "Bolsa"))));
        when(alunoRepository.findByUsuario_Ra("ADMIN0001")).thenReturn(Optional.empty());

        Page<BeneficioSummaryDTO> retorno = service.buscar(null, null, null, "ADMIN0001", PageRequest.of(0, 12));

        assertFalse(retorno.getContent().get(0).favorito());
        verify(favoritoRepository, never()).findBeneficioIdsFavoritados(any(), any());
    }

    /**
     * @apiNote Página vazia não dispara a consulta de favoritos, porque uma cláusula IN com lista
     *          vazia é SQL inválido em vários bancos.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void buscarComPaginaVaziaNaoConsultaFavoritos() {
        when(beneficioRepository.buscar(any(), any(), any(), any())).thenReturn(Page.empty());
        when(alunoRepository.findByUsuario_Ra("RA001")).thenReturn(Optional.of(aluno(10L)));

        service.buscar(null, null, null, "RA001", PageRequest.of(0, 12));

        verify(favoritoRepository, never()).findBeneficioIdsFavoritados(any(), any());
    }

    /**
     * @apiNote Desativar categoria não pode sumir com o benefício que já a usa; o estado vai no DTO.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void buscarDevolveBeneficioDeCategoriaDesativadaComOEstadoNoDto() {
        Beneficio comCategoriaDesativada = beneficio(1L, "Bolsa");
        comCategoriaDesativada.getCategorias().add(categoria(5L, "Antiga", false));
        when(beneficioRepository.buscar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(comCategoriaDesativada)));
        when(alunoRepository.findByUsuario_Ra("RA001")).thenReturn(Optional.empty());

        Page<BeneficioSummaryDTO> retorno = service.buscar(null, null, null, "RA001", PageRequest.of(0, 12));

        assertEquals(1, retorno.getTotalElements());
        assertFalse(retorno.getContent().get(0).categorias().iterator().next().ativo());
    }

    /**
     * @apiNote ID inexistente resulta em 404 com o id na mensagem.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void findByIdInexistenteInformaOIdNaMensagem() {
        when(beneficioRepository.findWithDetalhesById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class, () -> service.findById(99L));

        assertEquals("Benefício não encontrado com id: 99", erro.getMessage());
    }

    /**
     * @apiNote O card expandido carrega passos, palavras-chave e categorias montados pelo toDto.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void findByIdMontaPassosECategoriasNoDto() {
        Beneficio beneficio = beneficio(1L, "Bolsa");
        beneficio.getCategorias().add(categoria(5L, "Educacao", true));
        beneficio.getPalavrasChave().add("bolsa");
        beneficio.getPassos().add(new PassoBeneficio(1, "Acesse o site", "Entre com o RA"));
        when(beneficioRepository.findWithDetalhesById(1L)).thenReturn(Optional.of(beneficio));

        BeneficioResponseDTO retorno = service.findById(1L);

        assertEquals(1, retorno.passos().size());
        assertEquals("Acesse o site", retorno.passos().get(0).titulo());
        assertEquals(Set.of("bolsa"), retorno.palavrasChave());
        assertEquals("Educacao", retorno.categorias().iterator().next().nome());
    }

    /**
     * @apiNote Categoria desativada não pode entrar em benefício novo, e a mensagem diz qual é.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void createComCategoriaDesativadaNaoSalvaECitaONome() {
        when(beneficioRepository.existsByNomeIgnoreCase("Bolsa")).thenReturn(false);
        when(categoriaRepository.findAllById(Set.of(5L))).thenReturn(List.of(categoria(5L, "Antiga", false)));

        ResourceConflictException erro = assertThrows(ResourceConflictException.class,
                () -> service.create(request("Bolsa", Set.of(5L))));

        assertEquals("Não é possível vincular categoria desativada: Antiga.", erro.getMessage());
        verify(beneficioRepository, never()).save(any());
    }

    /**
     * @apiNote ID de categoria que não existe resulta em 404 nomeando os ids que faltaram.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void createComCategoriaInexistenteNomeiaOsIdsQueFaltaram() {
        when(beneficioRepository.existsByNomeIgnoreCase("Bolsa")).thenReturn(false);
        when(categoriaRepository.findAllById(any())).thenReturn(List.of());

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class,
                () -> service.create(request("Bolsa", Set.of(404L))));

        assertEquals("Categoria não encontrada com id: 404", erro.getMessage());
        verify(beneficioRepository, never()).save(any());
    }

    /**
     * @apiNote Com categoria ativa o cadastro segue normalmente e o vínculo aparece no retorno.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void createComCategoriaAtivaVinculaESalva() {
        when(beneficioRepository.existsByNomeIgnoreCase("Bolsa")).thenReturn(false);
        when(categoriaRepository.findAllById(Set.of(5L))).thenReturn(List.of(categoria(5L, "Educacao", true)));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocacao -> {
            Beneficio salvo = invocacao.getArgument(0);
            salvo.setId(1L);
            return salvo;
        });

        BeneficioResponseDTO retorno = service.create(request("Bolsa", Set.of(5L)));

        assertEquals(1L, retorno.id());
        assertEquals("Educacao", retorno.categorias().iterator().next().nome());
    }

    /**
     * @apiNote Nome duplicado interrompe antes de consultar categorias, poupando ida ao banco.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void createComNomeRepetidoNaoConsultaCategorias() {
        when(beneficioRepository.existsByNomeIgnoreCase("Bolsa")).thenReturn(true);

        ResourceConflictException erro = assertThrows(ResourceConflictException.class,
                () -> service.create(request("Bolsa", Set.of(5L))));

        assertEquals("Já existe um benefício com o nome 'Bolsa'.", erro.getMessage());
        verify(categoriaRepository, never()).findAllById(any());
    }

    /**
     * @apiNote Desativar a categoria não pode travar a edição de quem já a usava: reenviá-la passa.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void updateMantemCategoriaDesativadaQueOBeneficioJaTinha() {
        Categoria desativada = categoria(5L, "Antiga", false);
        Beneficio existente = beneficio(1L, "Bolsa");
        existente.getCategorias().add(desativada);
        when(beneficioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findAllById(Set.of(5L))).thenReturn(List.of(desativada));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        BeneficioResponseDTO retorno = service.update(1L, request(null, Set.of(5L)));

        assertEquals(1, retorno.categorias().size());
        assertFalse(retorno.categorias().iterator().next().ativo());
    }

    /**
     * @apiNote A edição não pode acrescentar categoria desativada que o benefício ainda não tinha.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void updateNaoDeixaAcrescentarCategoriaDesativadaNova() {
        Beneficio existente = beneficio(1L, "Bolsa");
        existente.getCategorias().add(categoria(5L, "Educacao", true));
        when(beneficioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(categoriaRepository.findAllById(Set.of(9L)))
                .thenReturn(List.of(categoria(9L, "Antiga", false)));

        ResourceConflictException erro = assertThrows(ResourceConflictException.class,
                () -> service.update(1L, request(null, Set.of(9L))));

        assertEquals("Não é possível vincular categoria desativada: Antiga.", erro.getMessage());
        verify(beneficioRepository, never()).save(any());
    }

    /**
     * @apiNote Lista vazia desvincula todas as categorias do benefício.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void updateComListaVaziaDesvinculaTodasAsCategorias() {
        Beneficio existente = beneficio(1L, "Bolsa");
        existente.getCategorias().add(categoria(5L, "Educacao", true));
        when(beneficioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        BeneficioResponseDTO retorno = service.update(1L, request(null, Set.of()));

        assertTrue(retorno.categorias().isEmpty());
    }

    /**
     * @apiNote No PUT, campo ausente significa "não mexe", e não "apaga".
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void updateComCampoAusenteNaoMexeNoValorAtual() {
        Beneficio existente = beneficio(1L, "Bolsa");
        existente.setChamada("chamada original");
        when(beneficioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(beneficioRepository.save(any(Beneficio.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        BeneficioResponseDTO retorno = service.update(1L, request(null, null));

        assertEquals("chamada original", retorno.chamada());
        assertEquals("Bolsa", retorno.nome());
    }

    /**
     * @apiNote Remoção de id inexistente falha antes de tocar o banco.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void deleteDeIdInexistenteNaoChamaORepositorio() {
        when(beneficioRepository.existsById(99L)).thenReturn(false);

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class, () -> service.delete(99L));

        assertEquals("Benefício não encontrado com id: 99", erro.getMessage());
        verify(beneficioRepository, never()).deleteById(any());
    }

    /**
     * @apiNote Favoritar é idempotente: repetir não é erro, mas também não grava de novo.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void favoritarDuasVezesNaoDuplicaOFavorito() {
        when(identidadeService.alunoPorRa("RA001")).thenReturn(aluno(10L));
        when(beneficioRepository.findById(1L)).thenReturn(Optional.of(beneficio(1L, "Bolsa")));
        when(favoritoRepository.contarFavorito(10L, 1L)).thenReturn(1L);

        service.favoritar("RA001", 1L);

        verify(favoritoRepository, never()).save(any());
    }

    /**
     * @apiNote Na primeira vez o favorito é persistido.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void favoritarPelaPrimeiraVezGrava() {
        when(identidadeService.alunoPorRa("RA001")).thenReturn(aluno(10L));
        when(beneficioRepository.findById(1L)).thenReturn(Optional.of(beneficio(1L, "Bolsa")));
        when(favoritoRepository.contarFavorito(10L, 1L)).thenReturn(0L);

        service.favoritar("RA001", 1L);

        verify(favoritoRepository).save(any());
    }

    /**
     * @apiNote Favoritar benefício inexistente resulta em 404 com o id, sem gravar nada.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void favoritarBeneficioInexistenteInformaOId() {
        when(identidadeService.alunoPorRa("RA001")).thenReturn(aluno(10L));
        when(beneficioRepository.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class,
                () -> service.favoritar("RA001", 99L));

        assertEquals("Benefício não encontrado com id: 99", erro.getMessage());
        verify(favoritoRepository, never()).save(any());
    }

    /**
     * @apiNote Desfavoritar o que não está nos favoritos é 404, distinguido pela contagem de
     *          linhas afetadas em vez de uma consulta extra.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void desfavoritarOQueNaoEstaNosFavoritosE404() {
        when(identidadeService.alunoPorRa("RA001")).thenReturn(aluno(10L));
        when(favoritoRepository.deleteByAlunoAndBeneficio(10L, 1L)).thenReturn(0);

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class,
                () -> service.desfavoritar("RA001", 1L));

        assertEquals("O benefício de id 1 não está nos favoritos deste aluno.", erro.getMessage());
    }

    /**
     * @apiNote A lista já é o recorte dos favoritos, então consultar favoritos de novo seria redundante.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    @Test
    void listarFavoritosMarcaTodosComoFavoritos() {
        when(identidadeService.alunoPorRa("RA001")).thenReturn(aluno(10L));
        when(favoritoRepository.findBeneficiosPorAluno(eq(10L), any()))
                .thenReturn(new PageImpl<>(List.of(beneficio(1L, "Bolsa"), beneficio(2L, "Desconto"))));

        Page<BeneficioSummaryDTO> retorno = service.listarFavoritos("RA001", PageRequest.of(0, 12));

        assertTrue(retorno.getContent().stream().allMatch(BeneficioSummaryDTO::favorito));
        verify(favoritoRepository, never()).findBeneficioIdsFavoritados(any(), any());
    }

    /**
     * @apiNote Monta um BeneficioRequestDTO de teste; com nome null os obrigatórios ficam
     *          ausentes, o que simula o PUT parcial.
     * @param nome --> Nome do benefício, ou null para um corpo de atualização parcial.
     * @param categoriaIds --> IDs de categoria a enviar, ou null para não mexer no vínculo.
     * @return --> Retorna o corpo montado.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private BeneficioRequestDTO request(String nome, Set<Long> categoriaIds) {
        return new BeneficioRequestDTO(null, nome, nome == null ? null : "chamada", nome == null ? null : "descricao",
                nome == null ? null : "url", nome == null ? null : TipoBeneficio.GRATUITO,
                nome == null ? null : "publico", null, null, null, null,
                List.<PassoBeneficioDTO>of(), categoriaIds);
    }

    /**
     * @apiNote Monta um Beneficio de teste com id, nome e tipo.
     * @param id --> Identificador do benefício.
     * @param nome --> Nome do benefício.
     * @return --> Retorna o benefício montado.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private Beneficio beneficio(Long id, String nome) {
        Beneficio beneficio = new Beneficio();
        beneficio.setId(id);
        beneficio.setNome(nome);
        beneficio.setTipo(TipoBeneficio.GRATUITO);
        return beneficio;
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

    /**
     * @apiNote Monta um Aluno de teste com o id informado.
     * @param id --> Identificador do aluno.
     * @return --> Retorna o aluno montado.
     * @author Mateus Alves Costa
     * @since 19.09.2026
     */
    private Aluno aluno(Long id) {
        Aluno aluno = new Aluno();
        aluno.setId(id);
        aluno.setUsuario(new Usuario());
        return aluno;
    }
}
