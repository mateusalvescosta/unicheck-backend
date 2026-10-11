package com.unicheck.notificacao.repository;

import com.unicheck.notificacao.domain.Notificacao;
import com.unicheck.notificacao.enumeration.TipoNotificacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    // Uma linha por aluno num unico comando, pulando quem desligou o tipo. O tipo vai como texto
    // porque a query e nativa e a coluna guarda o nome do enum.
    @Modifying
    @Query(value = """
            INSERT INTO tb_notificacao (usuario_id, tipo, referencia_id, mensagem, lida, criado_em)
            SELECT u.id, :tipo, :referenciaId, :mensagem, false, now()
            FROM tb_usuario u
            WHERE u.role = 'ALUNO'
              AND NOT EXISTS (SELECT 1 FROM tb_notificacao_desativada d
                              WHERE d.usuario_id = u.id AND d.tipo = :tipo)
            """, nativeQuery = true)
    int notificarAlunos(@Param("tipo") String tipo, @Param("referenciaId") Long referenciaId,
                        @Param("mensagem") String mensagem);

    /*
     * Grava um CHECKLIST_PARADO para cada par (aluno, checklist) parado, num unico comando. Parado:
     * checklist publicado, aluno com etapa concluida nele, checklist nao concluido pelo aluno e ultima
     * etapa ha diasPrimeiro dias ou mais. O ultimo NOT EXISTS sobre tb_notificacao cobre a repeticao:
     * sem lembrete desde a ultima etapa, envia; lembrete ha menos de diasEntre dias, nao envia.
     * Os dias sao contados por data do calendario, entao o horario exato da rotina nao desloca a contagem.
     */
    @Modifying
    @Query(value = """
            INSERT INTO tb_notificacao (usuario_id, tipo, referencia_id, mensagem, lida, criado_em)
            SELECT a.usuario_id, 'CHECKLIST_PARADO', p.checklist_id,
                   LEFT('Você não avança no checklist ' || c.nome || ' há '
                        || (CAST(:agora AS date) - CAST(p.ultima AS date)) || ' dias.', 255),
                   false, :agora
            FROM (SELECT pi.aluno_id, i.checklist_id, max(pi.data_conclusao) AS ultima
                  FROM tb_progresso_item pi
                  JOIN tb_item_checklist i ON i.id = pi.item_checklist_id
                  WHERE pi.status = 'CONCLUIDO'
                  GROUP BY pi.aluno_id, i.checklist_id) p
            JOIN tb_checklist c ON c.id = p.checklist_id AND c.status = 'PUBLICADO'
            JOIN tb_aluno a ON a.id = p.aluno_id
            WHERE CAST(p.ultima AS date) <= CAST(:agora AS date) - :diasPrimeiro
              AND NOT EXISTS (SELECT 1 FROM tb_checklist_concluido cc
                              WHERE cc.aluno_id = p.aluno_id AND cc.checklist_id = p.checklist_id)
              AND NOT EXISTS (SELECT 1 FROM tb_notificacao_desativada d
                              WHERE d.usuario_id = a.usuario_id AND d.tipo = 'CHECKLIST_PARADO')
              AND NOT EXISTS (SELECT 1 FROM tb_notificacao n
                              WHERE n.usuario_id = a.usuario_id AND n.tipo = 'CHECKLIST_PARADO'
                                AND n.referencia_id = p.checklist_id
                                AND n.criado_em > p.ultima
                                AND CAST(n.criado_em AS date) > CAST(:agora AS date) - :diasEntre)
            """, nativeQuery = true)
    int notificarChecklistsParados(@Param("agora") LocalDateTime agora, @Param("diasPrimeiro") int diasPrimeiro,
                                   @Param("diasEntre") int diasEntre);

    // Retencao: lidas somem depois de diasLidas e nao lidas depois de diasNaoLidas, ambos contados da criacao
    // (nao ha registro de quando foi lida). Os lembretes de checklist parado nao dependem dos antigos: a
    // rotina so olha o mais recente, que enquanto o aluno estiver parado tem menos de 7 dias.
    @Modifying
    @Query(value = """
            DELETE FROM tb_notificacao
            WHERE (lida = true AND criado_em < CAST(:agora AS timestamp) - make_interval(days => :diasLidas))
               OR (lida = false AND criado_em < CAST(:agora AS timestamp) - make_interval(days => :diasNaoLidas))
            """, nativeQuery = true)
    int apagarAntigas(@Param("agora") LocalDateTime agora, @Param("diasLidas") int diasLidas,
                      @Param("diasNaoLidas") int diasNaoLidas);

    // Preferencias: tb_notificacao_desativada guarda so os tipos desligados, entao sem linha = ligado.
    @Query(value = """
            SELECT d.tipo FROM tb_notificacao_desativada d
            JOIN tb_usuario u ON u.id = d.usuario_id
            WHERE u.ra = :ra
            """, nativeQuery = true)
    List<String> tiposDesativados(@Param("ra") String ra);

    // ON CONFLICT torna desligar idempotente mesmo com duas requisicoes simultaneas.
    @Modifying
    @Query(value = """
            INSERT INTO tb_notificacao_desativada (usuario_id, tipo)
            SELECT u.id, :tipo FROM tb_usuario u WHERE u.ra = :ra
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int desativar(@Param("ra") String ra, @Param("tipo") String tipo);

    @Modifying
    @Query(value = """
            DELETE FROM tb_notificacao_desativada
            WHERE tipo = :tipo AND usuario_id = (SELECT u.id FROM tb_usuario u WHERE u.ra = :ra)
            """, nativeQuery = true)
    int ativar(@Param("ra") String ra, @Param("tipo") String tipo);

    // O id desempata notificacoes gravadas no mesmo instante, como as de um mesmo INSERT ... SELECT.
    Page<Notificacao> findByUsuarioRaOrderByCriadoEmDescIdDesc(String ra, Pageable pageable);

    Page<Notificacao> findByUsuarioRaAndLidaOrderByCriadoEmDescIdDesc(String ra, Boolean lida, Pageable pageable);

    long countByUsuarioRaAndLidaFalse(String ra);

    // Devolve 0 quando a notificacao nao existe ou e de outro usuario; a Service traduz os dois para 404.
    @Modifying
    @Query("update Notificacao n set n.lida = true "
            + "where n.id = :id and n.usuario.id = (select u.id from Usuario u where u.ra = :ra)")
    int marcarComoLida(@Param("id") Long id, @Param("ra") String ra);

    @Modifying
    @Query("update Notificacao n set n.lida = true "
            + "where n.lida = false and n.usuario.id = (select u.id from Usuario u where u.ra = :ra)")
    int marcarTodasComoLidas(@Param("ra") String ra);

    @Modifying
    @Query("delete from Notificacao n where n.tipo in :tipos and n.referenciaId = :referenciaId")
    int removerDoObjeto(@Param("tipos") Collection<TipoNotificacao> tipos,
                        @Param("referenciaId") Long referenciaId);
}
