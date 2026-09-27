package com.unicheck.manual.service;

import com.unicheck.exception.ResourceConflictException;
import com.unicheck.manual.domain.AtalhoManual;
import com.unicheck.manual.domain.LocalAtendimento;
import com.unicheck.manual.domain.Manual;
import com.unicheck.manual.domain.PassoManual;
import com.unicheck.manual.dto.AtalhoManualDTO;
import com.unicheck.manual.dto.CategoriaManualDTO;
import com.unicheck.manual.dto.LocalAtendimentoDTO;
import com.unicheck.manual.dto.ManualRequestDTO;
import com.unicheck.manual.dto.ManualResponseDTO;
import com.unicheck.manual.dto.ManualSummaryDTO;
import com.unicheck.manual.dto.PassoManualDTO;
import com.unicheck.manual.enumeration.CategoriaManual;
import com.unicheck.manual.repository.AtalhoManualRepository;
import com.unicheck.manual.repository.ManualRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @apiNote Testes unitários da ManualService, com repositories mockados.
 *
 *          Cobre as decisões que não são óbvias no código: a atualização é completa, a ordem
 *          desloca os vizinhos em vez de gerar conflito, o anterior e o próximo saem da ordem, e
 *          remover um manual o tira das listas de relacionados antes do DELETE.
 * @author Mateus Alves Costa
 * @since 27.09.2026
 */
@ExtendWith(MockitoExtension.class)
class ManualServiceTest {
    @Mock private ManualRepository manualRepository;
    @Mock private AtalhoManualRepository atalhoRepository;
    @InjectMocks private ManualService service;

    /**
     * @apiNote O termo chega ao SQL em minúsculas e entre percentuais, e a categoria pelo nome.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarNormalizaTermoECategoria() {
        when(manualRepository.buscar(any(), any(), any())).thenReturn(Page.empty());

        service.buscar("  BoLeto ", CategoriaManual.UTILIDADES_INSTITUCIONAIS, PageRequest.of(0, 20));

        verify(manualRepository).buscar(eq("%boleto%"), eq("UTILIDADES_INSTITUCIONAIS"), any());
    }

    /**
     * @apiNote Termo em branco desliga o filtro de texto em vez de virar "%%".
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarComTermoEmBrancoNaoFiltraPorTexto() {
        when(manualRepository.buscar(any(), any(), any())).thenReturn(Page.empty());

        service.buscar("   ", null, PageRequest.of(0, 20));

        verify(manualRepository).buscar(eq(null), eq(null), any());
    }

    /**
     * @apiNote Todas as categorias aparecem, na ordem do enum, e a sem manual vem com zero.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listarCategoriasTrazTodasComAQuantidade() {
        when(manualRepository.countByCategoria(any())).thenReturn(0L);
        when(manualRepository.countByCategoria(CategoriaManual.MATRICULA_VIDA_ACADEMICA)).thenReturn(9L);

        List<CategoriaManualDTO> categorias = service.listarCategorias();

        assertEquals(CategoriaManual.values().length, categorias.size());
        assertEquals(CategoriaManual.MATRICULA_VIDA_ACADEMICA, categorias.get(0).codigo());
        assertEquals(9L, categorias.get(0).quantidadeManuais());
        assertEquals(0L, categorias.get(1).quantidadeManuais());
        assertEquals("Vida acadêmica", categorias.get(0).nomeCurto());
    }

    /**
     * @apiNote O rodapé sai da ordem: anterior e próximo são os vizinhos na sequência.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void findByIdResolveAnteriorEProximoPelaOrdem() {
        Manual manual = manual(2L, "Rematrícula", 5);
        when(manualRepository.findById(2L)).thenReturn(Optional.of(manual));
        when(manualRepository.findFirstByOrdemLessThanOrderByOrdemDesc(5))
                .thenReturn(Optional.of(manual(1L, "RA e identidade estudantil", 4)));
        when(manualRepository.findFirstByOrdemGreaterThanOrderByOrdemAsc(5)).thenReturn(Optional.empty());

        ManualResponseDTO dto = service.findById(2L);

        assertEquals("RA e identidade estudantil", dto.anterior().titulo());
        assertNull(dto.proximo());
    }

    /**
     * @apiNote Manual inexistente vira 404.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void findByIdDeManualInexistenteLancaNaoEncontrado() {
        when(manualRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.findById(99L));
    }

    /**
     * @apiNote Sem ordem, o manual entra no fim e ninguém é deslocado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createSemOrdemEntraNoFim() {
        when(manualRepository.maiorOrdem()).thenReturn(7);
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        ManualResponseDTO dto = service.create(request(null, List.of()));

        assertEquals(8, dto.ordem());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote Com ordem no meio da sequência, os manuais a partir dela descem uma posição.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComOrdemNoMeioDeslocaOsSeguintes() {
        when(manualRepository.maiorOrdem()).thenReturn(7);
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        ManualResponseDTO dto = service.create(request(3, List.of()));

        assertEquals(3, dto.ordem());
        verify(manualRepository).deslocarOrdem(3, 7, 1);
    }

    /**
     * @apiNote Título repetido, ignorando maiúsculas, é conflito e nada é gravado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComTituloRepetidoLancaConflito() {
        when(manualRepository.existsByTituloIgnoreCase("Portal Acadêmico")).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.create(request(null, List.of())));
        verify(manualRepository, never()).save(any());
    }

    /**
     * @apiNote Relacionados e próximo passo são resolvidos pelo id, e id inexistente vira 404.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComRelacionadoInexistenteLancaNaoEncontrado() {
        when(manualRepository.findAllById(List.of(4L, 5L))).thenReturn(List.of(manual(4L, "AVA", 1)));

        EntityNotFoundException erro = assertThrows(EntityNotFoundException.class,
                () -> service.create(request(null, List.of(4L, 5L))));
        assertTrue(erro.getMessage().endsWith("5"));
    }

    /**
     * @apiNote Os relacionados mantêm a ordem em que o admin os enviou.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createMantemAOrdemDosRelacionados() {
        when(manualRepository.findAllById(List.of(5L, 4L)))
                .thenReturn(List.of(manual(4L, "AVA", 1), manual(5L, "Rematrícula", 2)));
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        ManualResponseDTO dto = service.create(request(null, List.of(5L, 4L)));

        assertEquals(List.of(5L, 4L), dto.relacionados().stream().map(r -> r.id()).toList());
    }

    /**
     * @apiNote Relacionado repetido é conflito.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComRelacionadoRepetidoLancaConflito() {
        assertThrows(ResourceConflictException.class, () -> service.create(request(null, List.of(4L, 4L))));
    }

    /**
     * @apiNote Um manual não pode ser relacionado de si mesmo.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateQueRelacionaASiMesmoLancaConflito() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 1)));

        assertThrows(ResourceConflictException.class, () -> service.update(1L, request(null, List.of(1L))));
        verify(manualRepository, never()).save(any());
    }

    /**
     * @apiNote Na atualização completa, bloco opcional ausente é removido.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateCompletoRemoveBlocoOpcionalAusente() {
        Manual manual = manual(1L, "Portal Acadêmico", 1);
        manual.setQuandoSeAplica("Para consultar registros.");
        manual.getEntendaMelhor().add("O Portal registra a vida acadêmica.");
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual));
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManualResponseDTO dto = service.update(1L, request(null, List.of()));

        assertNull(dto.quandoSeAplica());
        assertTrue(dto.entendaMelhor().isEmpty());
        assertEquals(1, dto.ordem());
    }

    /**
     * @apiNote Subir de posição desloca para baixo os manuais entre a nova e a antiga.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateQueSobeDePosicaoDeslocaOsDoMeio() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 6)));
        when(manualRepository.maiorOrdem()).thenReturn(10);
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManualResponseDTO dto = service.update(1L, request(2, List.of()));

        assertEquals(2, dto.ordem());
        verify(manualRepository).deslocarOrdem(2, 5, 1);
    }

    /**
     * @apiNote Descer além do fim leva o manual para a última posição, sem deixar buraco.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateQueDesceAlemDoFimVaiParaAUltimaPosicao() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 3)));
        when(manualRepository.maiorOrdem()).thenReturn(10);
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManualResponseDTO dto = service.update(1L, request(50, List.of()));

        assertEquals(10, dto.ordem());
        verify(manualRepository).deslocarOrdem(4, 10, -1);
    }

    /**
     * @apiNote Remover tira o manual das listas de relacionados, apaga e fecha o buraco da ordem.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deleteTiraDosRelacionadosEFechaAOrdem() {
        Manual removido = manual(1L, "Portal Acadêmico", 4);
        Manual quemRelaciona = manual(2L, "Rematrícula", 5);
        quemRelaciona.getRelacionados().add(removido);
        when(manualRepository.findById(1L)).thenReturn(Optional.of(removido));
        when(manualRepository.findQueRelacionam(1L)).thenReturn(List.of(quemRelaciona));

        service.delete(1L);

        assertTrue(quemRelaciona.getRelacionados().isEmpty());
        verify(manualRepository).delete(removido);
        verify(manualRepository).deslocarOrdem(5, Integer.MAX_VALUE, -1);
    }

    /**
     * @apiNote A lista é trocada inteira: os atuais são apagados e os novos gravados com a ordem
     *          igual à posição na lista recebida.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosNumeraPelaPosicao() {
        when(manualRepository.findById(7L)).thenReturn(Optional.of(manual(7L, "Portal Acadêmico", 2)));
        when(manualRepository.findById(3L)).thenReturn(Optional.of(manual(3L, "Mensalidades e boletos", 1)));
        ArgumentCaptor<AtalhoManual> gravados = ArgumentCaptor.forClass(AtalhoManual.class);

        service.substituirAtalhos(List.of(
                new AtalhoManualDTO(7L, "Acessar o Portal"),
                new AtalhoManualDTO(3L, " Emitir boleto ")));

        verify(atalhoRepository).deleteAllInBatch();
        verify(atalhoRepository, times(2)).save(gravados.capture());
        assertEquals(1, gravados.getAllValues().get(0).getOrdem());
        assertEquals(7L, gravados.getAllValues().get(0).getManual().getId());
        assertEquals(2, gravados.getAllValues().get(1).getOrdem());
        assertEquals("Emitir boleto", gravados.getAllValues().get(1).getNome());
    }

    /**
     * @apiNote O mesmo manual em dois atalhos é conflito, e a lista atual não é apagada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosComManualRepetidoLancaConflito() {
        assertThrows(ResourceConflictException.class, () -> service.substituirAtalhos(List.of(
                new AtalhoManualDTO(7L, "Acessar o Portal"),
                new AtalhoManualDTO(7L, "Abrir Portal"))));
        verify(atalhoRepository, never()).deleteAllInBatch();
    }

    /**
     * @apiNote Manual inexistente vira 404; a remoção já feita é desfeita pela transação.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosComManualInexistenteLancaNaoEncontrado() {
        when(manualRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.substituirAtalhos(List.of(
                new AtalhoManualDTO(99L, "Acessar o Portal"))));
        verify(atalhoRepository, never()).save(any());
    }

    /**
     * @apiNote Cada atalho volta só com o nome e o manual para onde aponta.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listarAtalhosTrazNomeEManual() {
        when(atalhoRepository.findAllByOrderByOrdemAsc()).thenReturn(List.of(
                new AtalhoManual(1L, manual(3L, "Mensalidades e boletos", 1), "Emitir boleto", 1)));

        assertEquals(List.of(new AtalhoManualDTO(3L, "Emitir boleto")), service.listarAtalhos());
    }

    // ---------------------------------------------------------------- busca e leitura

    /**
     * @apiNote O card da busca traz o nome da categoria, e não só a constante.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarDevolveCardComNomeDaCategoria() {
        when(manualRepository.buscar(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(manual(1L, "Rematrícula", 2))));

        Page<ManualSummaryDTO> pagina = service.buscar(null, null, PageRequest.of(0, 20));

        ManualSummaryDTO card = pagina.getContent().get(0);
        assertEquals("Rematrícula", card.titulo());
        assertEquals("Matrícula e vida acadêmica", card.categoriaNome());
        assertEquals(2, card.ordem());
    }

    /**
     * @apiNote A página pedida chega intacta ao repository.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void buscarRepassaAPaginacao() {
        when(manualRepository.buscar(any(), any(), any())).thenReturn(Page.empty());

        service.buscar(null, null, PageRequest.of(2, 5));

        verify(manualRepository).buscar(null, null, PageRequest.of(2, 5));
    }

    /**
     * @apiNote A página traz todos os blocos e as referências a outros manuais já resolvidas.
     *          O primeiro manual da sequência não tem anterior.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void findByIdCopiaBlocosEReferencias() {
        Manual manual = manual(1L, "Portal Acadêmico", 1);
        manual.getAtencao().add("Atenção");
        manual.getPassos().add(new PassoManual(1, "Acesse o Portal."));
        manual.getOndeResolver().add(new LocalAtendimento("Portal Acadêmico", "https://portal"));
        manual.setProximoPassoManual(manual(9L, "AVA", 9));
        manual.getRelacionados().add(manual(4L, "Rematrícula", 4));
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual));
        when(manualRepository.findFirstByOrdemLessThanOrderByOrdemDesc(1)).thenReturn(Optional.empty());
        when(manualRepository.findFirstByOrdemGreaterThanOrderByOrdemAsc(1))
                .thenReturn(Optional.of(manual(2L, "Meu EduCONNECT", 2)));

        ManualResponseDTO dto = service.findById(1L);

        assertEquals(List.of("Atenção"), dto.atencao());
        assertEquals(List.of(new PassoManualDTO(1, "Acesse o Portal.")), dto.passos());
        assertEquals(List.of(new LocalAtendimentoDTO("Portal Acadêmico", "https://portal")), dto.ondeResolver());
        assertEquals("AVA", dto.proximoPassoManual().titulo());
        assertEquals("Rematrícula", dto.relacionados().get(0).titulo());
        assertEquals("Matrícula e vida acadêmica", dto.relacionados().get(0).categoriaNome());
        assertNull(dto.anterior());
        assertEquals("Meu EduCONNECT", dto.proximo().titulo());
    }

    /**
     * @apiNote As listas do DTO são cópias: mexer na entidade depois não altera a resposta, e a
     *          resposta não deixa mexer nas listas.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void findByIdDevolveListasDesligadasDaEntidade() {
        Manual manual = manual(1L, "Portal Acadêmico", 1);
        manual.getDetalhes().add("Detalhe");
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual));

        ManualResponseDTO dto = service.findById(1L);
        manual.getDetalhes().add("Adicionado depois");

        assertEquals(List.of("Detalhe"), dto.detalhes());
        assertThrows(UnsupportedOperationException.class, () -> dto.detalhes().add("x"));
    }

    /**
     * @apiNote Sem nenhum manual cadastrado, todas as categorias aparecem com zero.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listarCategoriasSemManuaisTrazTodasComZero() {
        List<CategoriaManualDTO> categorias = service.listarCategorias();

        assertEquals(8, categorias.size());
        assertTrue(categorias.stream().allMatch(categoria -> categoria.quantidadeManuais() == 0));
        assertEquals(CategoriaManual.COMECANDO_UNISALES, categorias.get(7).codigo());
    }

    // ---------------------------------------------------------------- cadastro

    /**
     * @apiNote Textos chegam aparados, e opcional em branco vira null em vez de string vazia.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createAparaTextosEOpcionalEmBrancoViraNull() {
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));
        Requisicao requisicao = new Requisicao();
        requisicao.titulo = "  Portal Acadêmico  ";
        requisicao.quandoSeAplica = "   ";
        requisicao.linkTexto = " Abrir Portal ";
        requisicao.linkUrl = " https://portal ";
        requisicao.antesDeComecar = List.of("  Ter seu RA.  ");
        requisicao.ondeResolver = List.of(new LocalAtendimentoDTO(" Portal ", "  "));

        ManualResponseDTO dto = service.create(requisicao.build());

        assertEquals("Portal Acadêmico", dto.titulo());
        assertNull(dto.quandoSeAplica());
        assertEquals("Abrir Portal", dto.linkTexto());
        assertEquals("https://portal", dto.linkUrl());
        assertEquals(List.of("Ter seu RA."), dto.antesDeComecar());
        assertEquals(List.of(new LocalAtendimentoDTO("Portal", null)), dto.ondeResolver());
        verify(manualRepository).existsByTituloIgnoreCase("Portal Acadêmico");
    }

    /**
     * @apiNote Os passos são gravados na ordem numérica, não na ordem em que chegaram.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createOrdenaPassosPelaOrdem() {
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        ManualResponseDTO dto = service.create(request(null, List.of()));

        assertEquals(List.of(1, 2), dto.passos().stream().map(PassoManualDTO::ordem).toList());
    }

    /**
     * @apiNote Dois passos com a mesma ordem é conflito, antes de chegar à chave do banco.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComPassosDeMesmaOrdemLancaConflito() {
        Requisicao requisicao = new Requisicao();
        requisicao.passos = List.of(new PassoManualDTO(1, "Um"), new PassoManualDTO(1, "Outro"));

        assertThrows(ResourceConflictException.class, () -> service.create(requisicao.build()));
        verify(manualRepository, never()).save(any());
    }

    /**
     * @apiNote Ordem além do fim não deixa buraco: o manual entra logo depois do último.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComOrdemAlemDoFimEntraLogoDepoisDoUltimo() {
        when(manualRepository.maiorOrdem()).thenReturn(7);
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        assertEquals(8, service.create(request(50, List.of())).ordem());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote A primeira posição livre não desloca ninguém.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createNaPrimeiraPosicaoLivreNaoDesloca() {
        when(manualRepository.maiorOrdem()).thenReturn(7);
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        assertEquals(8, service.create(request(8, List.of())).ordem());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote O primeiro manual cadastrado fica com ordem 1, qualquer que seja a pedida.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createDoPrimeiroManualFicaComOrdemUm() {
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 1L));

        assertEquals(1, service.create(request(3, List.of())).ordem());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote Ordem 1 com manuais cadastrados desloca a sequência inteira.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createNaPrimeiraPosicaoDeslocaTodos() {
        when(manualRepository.maiorOrdem()).thenReturn(5);
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));

        assertEquals(1, service.create(request(1, List.of())).ordem());
        verify(manualRepository).deslocarOrdem(1, 5, 1);
    }

    /**
     * @apiNote O manual indicado em "Seu próximo passo" é resolvido pelo id.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComProximoPassoGuardaAReferencia() {
        when(manualRepository.findById(9L)).thenReturn(Optional.of(manual(9L, "AVA", 9)));
        when(manualRepository.save(any())).thenAnswer(inv -> comId(inv.getArgument(0), 10L));
        Requisicao requisicao = new Requisicao();
        requisicao.proximoPassoDescricao = "Veja a orientação do AVA.";
        requisicao.proximoPassoManualId = 9L;

        ManualResponseDTO dto = service.create(requisicao.build());

        assertEquals("Veja a orientação do AVA.", dto.proximoPassoDescricao());
        assertEquals(9L, dto.proximoPassoManual().id());
    }

    /**
     * @apiNote Próximo passo apontando para manual inexistente é 404, e nada é gravado nem
     *          deslocado.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComProximoPassoInexistenteLancaNaoEncontrado() {
        when(manualRepository.findById(9L)).thenReturn(Optional.empty());
        Requisicao requisicao = new Requisicao();
        requisicao.proximoPassoDescricao = "Veja.";
        requisicao.proximoPassoManualId = 9L;

        assertThrows(EntityNotFoundException.class, () -> service.create(requisicao.build()));
        verify(manualRepository, never()).save(any());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote Título repetido barra antes de qualquer deslocamento na sequência.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void createComTituloRepetidoNaoDeslocaASequencia() {
        when(manualRepository.existsByTituloIgnoreCase(any())).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.create(request(1, List.of())));
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    // ---------------------------------------------------------------- atualização

    /**
     * @apiNote Atualizar manual inexistente é 404, sem gravar nada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateDeManualInexistenteLancaNaoEncontrado() {
        when(manualRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.update(99L, request(null, List.of())));
        verify(manualRepository, never()).save(any());
    }

    /**
     * @apiNote Título de outro manual é conflito; a verificação ignora o próprio manual.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateComTituloDeOutroManualLancaConflito() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Antigo", 1)));
        when(manualRepository.existsByTituloIgnoreCaseAndIdNot("Portal Acadêmico", 1L)).thenReturn(true);

        assertThrows(ResourceConflictException.class, () -> service.update(1L, request(null, List.of())));
        verify(manualRepository, never()).save(any());
    }

    /**
     * @apiNote Um manual não pode indicar a si mesmo em "Seu próximo passo".
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateQueIndicaASiMesmoComoProximoPassoLancaConflito() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 1)));
        Requisicao requisicao = new Requisicao();
        requisicao.proximoPassoDescricao = "Veja.";
        requisicao.proximoPassoManualId = 1L;

        assertThrows(ResourceConflictException.class, () -> service.update(1L, requisicao.build()));
        verify(manualRepository, never()).save(any());
    }

    /**
     * @apiNote Sem ordem no corpo, a posição não muda e a sequência nem é consultada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateSemOrdemMantemAPosicao() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 4)));
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(4, service.update(1L, request(null, List.of())).ordem());
        verify(manualRepository, never()).maiorOrdem();
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote Mandar a mesma ordem que o manual já tem não desloca ninguém.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateComAMesmaOrdemNaoDesloca() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 4)));
        when(manualRepository.maiorOrdem()).thenReturn(10);
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(4, service.update(1L, request(4, List.of())).ordem());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote Descer dentro da sequência sobe uma casa os manuais entre a antiga e a nova.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateQueDesceDentroDaSequenciaDeslocaOsDoMeio() {
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual(1L, "Portal Acadêmico", 3)));
        when(manualRepository.maiorOrdem()).thenReturn(10);
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(6, service.update(1L, request(6, List.of())).ordem());
        verify(manualRepository).deslocarOrdem(4, 6, -1);
    }

    /**
     * @apiNote A lista de relacionados é trocada, não somada à anterior.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateSubstituiOsRelacionadosAnteriores() {
        Manual manual = manual(1L, "Portal Acadêmico", 1);
        manual.getRelacionados().add(manual(4L, "Antigo", 4));
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual));
        when(manualRepository.findAllById(List.of(5L))).thenReturn(List.of(manual(5L, "Novo", 5)));
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManualResponseDTO dto = service.update(1L, request(null, List.of(5L)));

        assertEquals(List.of(5L), dto.relacionados().stream().map(r -> r.id()).toList());
    }

    /**
     * @apiNote Sem "Ver orientação" no corpo, a indicação anterior é removida.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void updateSemProximoPassoRemoveAIndicacaoAnterior() {
        Manual manual = manual(1L, "Portal Acadêmico", 1);
        manual.setProximoPassoManual(manual(9L, "AVA", 9));
        manual.setProximoPassoDescricao("Veja o AVA.");
        when(manualRepository.findById(1L)).thenReturn(Optional.of(manual));
        when(manualRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManualResponseDTO dto = service.update(1L, request(null, List.of()));

        assertNull(dto.proximoPassoManual());
        assertNull(dto.proximoPassoDescricao());
    }

    // ---------------------------------------------------------------- remoção

    /**
     * @apiNote Remover manual inexistente é 404, sem apagar nem deslocar nada.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deleteDeManualInexistenteLancaNaoEncontrado() {
        when(manualRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.delete(99L));
        verify(manualRepository, never()).delete(any());
        verify(manualRepository, never()).deslocarOrdem(anyInt(), anyInt(), anyInt());
    }

    /**
     * @apiNote Quem indicava o manual em "Seu próximo passo" perde a indicação antes do DELETE; se
     *          ficasse apontando para o removido, o Hibernate recusaria o flush.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deleteLimpaQuemOIndicavaNoProximoPasso() {
        Manual removido = manual(1L, "AVA", 4);
        Manual quemIndica = manual(2L, "Portal Acadêmico", 5);
        quemIndica.setProximoPassoManual(removido);
        when(manualRepository.findById(1L)).thenReturn(Optional.of(removido));
        when(manualRepository.findByProximoPassoManualId(1L)).thenReturn(List.of(quemIndica));

        service.delete(1L);

        assertNull(quemIndica.getProximoPassoManual());
        verify(manualRepository).delete(removido);
    }

    /**
     * @apiNote A remoção acontece antes do deslocamento, para a ordem do removido ficar livre.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void deleteApagaAntesDeFecharAOrdem() {
        Manual removido = manual(1L, "Portal Acadêmico", 4);
        when(manualRepository.findById(1L)).thenReturn(Optional.of(removido));

        service.delete(1L);

        InOrder ordem = inOrder(manualRepository);
        ordem.verify(manualRepository).delete(removido);
        ordem.verify(manualRepository).deslocarOrdem(5, Integer.MAX_VALUE, -1);
    }

    // ---------------------------------------------------------------- atalhos

    /**
     * @apiNote Lista vazia apaga todos os atalhos e não grava nenhum.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosComListaVaziaApagaTodos() {
        assertTrue(service.substituirAtalhos(List.of()).isEmpty());
        verify(atalhoRepository).deleteAllInBatch();
        verify(atalhoRepository, never()).save(any());
    }

    /**
     * @apiNote A resposta é o que ficou gravado, relido do banco.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void substituirAtalhosDevolveOQueFicouGravado() {
        Manual portal = manual(7L, "Portal Acadêmico", 1);
        when(manualRepository.findById(7L)).thenReturn(Optional.of(portal));
        when(atalhoRepository.findAllByOrderByOrdemAsc())
                .thenReturn(List.of(new AtalhoManual(1L, portal, "Acessar o Portal", 1)));

        List<AtalhoManualDTO> atalhos = service.substituirAtalhos(List.of(new AtalhoManualDTO(7L, "Acessar o Portal")));

        assertEquals(List.of(new AtalhoManualDTO(7L, "Acessar o Portal")), atalhos);
    }

    /**
     * @apiNote Sem atalhos configurados, a lista vem vazia.
     * @author Mateus Alves Costa
     * @since 27.09.2026
     */
    @Test
    void listarAtalhosSemNenhumConfiguradoVemVazio() {
        assertTrue(service.listarAtalhos().isEmpty());
    }

    private Manual manual(Long id, String titulo, int ordem) {
        Manual manual = new Manual();
        manual.setId(id);
        manual.setTitulo(titulo);
        manual.setOrdem(ordem);
        manual.setCategoria(CategoriaManual.MATRICULA_VIDA_ACADEMICA);
        manual.setResumo("Resumo");
        manual.setOQueSaber("Saber");
        manual.setTempoLeitura(1);
        return manual;
    }

    private Manual comId(Manual manual, Long id) {
        manual.setId(id);
        return manual;
    }

    private ManualRequestDTO request(Integer ordem, List<Long> relacionadoIds) {
        return new ManualRequestDTO(ordem, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS, 1, "Portal Acadêmico",
                "Conheça o canal.", null, null, null, "O Portal Acadêmico é o sistema.", null, null,
                List.of(new PassoManualDTO(2, "Escolha o serviço."), new PassoManualDTO(1, "Acesse o Portal.")),
                List.of(new LocalAtendimentoDTO("Portal Acadêmico", null)), null, null, null, null, null, null,
                null, relacionadoIds);
    }

    /**
     * @apiNote Monta um ManualRequestDTO válido por padrão; cada teste troca só o que interessa.
     */
    private static class Requisicao {
        String titulo = "Portal Acadêmico";
        String linkTexto;
        String linkUrl;
        String quandoSeAplica;
        List<String> antesDeComecar;
        List<PassoManualDTO> passos;
        List<LocalAtendimentoDTO> ondeResolver;
        String proximoPassoDescricao;
        Long proximoPassoManualId;

        ManualRequestDTO build() {
            return new ManualRequestDTO(null, CategoriaManual.PORTAL_FERRAMENTAS_DIGITAIS, 1, titulo,
                    "Conheça o canal.", linkTexto, linkUrl, quandoSeAplica, "O Portal Acadêmico é o sistema.",
                    null, antesDeComecar, passos, ondeResolver, null, null, null, null, proximoPassoDescricao,
                    proximoPassoManualId, null, null);
        }
    }
}
