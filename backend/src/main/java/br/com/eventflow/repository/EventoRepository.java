package br.com.eventflow.repository;

import br.com.eventflow.entity.Evento;
import br.com.eventflow.entity.StatusEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    Page<Evento> findByStatus(StatusEvento status, Pageable pageable);
}
