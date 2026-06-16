package br.com.eventflow.service;

import br.com.eventflow.dto.request.CriarTurmaRequest;
import br.com.eventflow.dto.response.TurmaResponse;
import br.com.eventflow.entity.*;
import br.com.eventflow.exception.ResourceNotFoundException;
import br.com.eventflow.repository.EventoRepository;
import br.com.eventflow.repository.TurmaRepository;
import br.com.eventflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TurmaService {

    private final TurmaRepository turmaRepository;
    private final EventoRepository eventoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public TurmaResponse criar(UUID eventoId, CriarTurmaRequest request) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + eventoId));

        Usuario professor = usuarioRepository.findById(request.getProfessorId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado: " + request.getProfessorId()));

        Turma turma = Turma.builder()
                .evento(evento)
                .professor(professor)
                .nome(request.getNome())
                .dataHoraInicio(request.getDataHoraInicio())
                .dataHoraFim(request.getDataHoraFim())
                .vagas(request.getVagas())
                .status(StatusTurma.ABERTA)
                .build();

        turma = turmaRepository.save(turma);
        return toResponse(turma);
    }

    public List<TurmaResponse> listarPorEvento(UUID eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new ResourceNotFoundException("Evento não encontrado: " + eventoId);
        }
        return turmaRepository.findByEventoId(eventoId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TurmaResponse editar(UUID id, CriarTurmaRequest request) {
        Turma turma = findOrThrow(id);

        Usuario professor = usuarioRepository.findById(request.getProfessorId())
                .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado: " + request.getProfessorId()));

        turma.setProfessor(professor);
        turma.setNome(request.getNome());
        turma.setDataHoraInicio(request.getDataHoraInicio());
        turma.setDataHoraFim(request.getDataHoraFim());
        turma.setVagas(request.getVagas());

        turma = turmaRepository.save(turma);
        return toResponse(turma);
    }

    @Transactional
    public void cancelar(UUID id) {
        Turma turma = findOrThrow(id);
        turma.setStatus(StatusTurma.CANCELADA);
        turmaRepository.save(turma);
    }

    public Turma findOrThrow(UUID id) {
        return turmaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada: " + id));
    }

    private TurmaResponse toResponse(Turma turma) {
        return TurmaResponse.builder()
                .id(turma.getId())
                .nome(turma.getNome())
                .eventoId(turma.getEvento().getId())
                .eventoTitulo(turma.getEvento().getTitulo())
                .professorId(turma.getProfessor().getId())
                .professorNome(turma.getProfessor().getNome())
                .dataHoraInicio(turma.getDataHoraInicio())
                .dataHoraFim(turma.getDataHoraFim())
                .vagas(turma.getVagas())
                .status(turma.getStatus().name())
                .build();
    }
}
