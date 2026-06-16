package br.com.eventflow.repository;

import br.com.eventflow.entity.Inscricao;
import br.com.eventflow.entity.StatusInscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {

    List<Inscricao> findByAlunoId(UUID alunoId);

    List<Inscricao> findByTurmaIdAndStatus(UUID turmaId, StatusInscricao status);

    long countByTurmaIdAndStatus(UUID turmaId, StatusInscricao status);

    boolean existsByAlunoIdAndTurmaId(UUID alunoId, UUID turmaId);

    Optional<Inscricao> findFirstByTurmaIdAndStatusOrderByPosicaoEsperaAsc(UUID turmaId, StatusInscricao status);

    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM Inscricao i " +
           "JOIN i.turma t " +
           "WHERE i.aluno.id = :alunoId " +
           "AND i.status = 'CONFIRMADA' " +
           "AND t.dataHoraInicio < :fim " +
           "AND t.dataHoraFim > :inicio")
    boolean existsConflito(@Param("alunoId") UUID alunoId,
                           @Param("inicio") java.time.LocalDateTime inicio,
                           @Param("fim") java.time.LocalDateTime fim);
}
