package br.com.eventflow.repository;

import br.com.eventflow.entity.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TurmaRepository extends JpaRepository<Turma, UUID> {
    List<Turma> findByEventoId(UUID eventoId);
    List<Turma> findByProfessorId(UUID professorId);
}
