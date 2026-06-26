package br.com.eventflow.service;

import br.com.eventflow.dto.response.InscricaoResponse;
import br.com.eventflow.entity.*;
import br.com.eventflow.exception.BusinessException;
import br.com.eventflow.exception.ConflictException;
import br.com.eventflow.exception.ResourceNotFoundException;
import br.com.eventflow.repository.InscricaoRepository;
import br.com.eventflow.repository.TurmaRepository;
import br.com.eventflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public InscricaoResponse inscrever(UUID turmaId, UUID alunoId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada: " + turmaId));

        if (turma.getStatus() != StatusTurma.ABERTA) {
            throw new BusinessException("Turma não está aberta para inscrições");
        }

        if (inscricaoRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)) {
            throw new ConflictException("Aluno já inscrito nesta turma");
        }

        if (inscricaoRepository.existsConflito(alunoId, turma.getDataHoraInicio(), turma.getDataHoraFim())) {
            throw new ConflictException("Conflito de horário com outra turma já inscrita");
        }

        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado"));

        long confirmadas = inscricaoRepository.countByTurmaIdAndStatus(turmaId, StatusInscricao.CONFIRMADA);
        boolean temVaga = confirmadas < turma.getVagas();

        Inscricao inscricao = Inscricao.builder()
                .aluno(aluno)
                .turma(turma)
                .status(temVaga ? StatusInscricao.CONFIRMADA : StatusInscricao.LISTA_ESPERA)
                .posicaoEspera(temVaga ? null : calcularPosicaoEspera(turmaId))
                .build();

        inscricao = inscricaoRepository.save(inscricao);
        return toResponse(inscricao);
    }

    @Transactional
    public void cancelar(UUID inscricaoId, UUID alunoId) {
        Inscricao inscricao = inscricaoRepository.findById(inscricaoId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada: " + inscricaoId));

        if (!inscricao.getAluno().getId().equals(alunoId)) {
            throw new BusinessException("Você só pode cancelar suas próprias inscrições");
        }

        StatusInscricao statusAnterior = inscricao.getStatus();
        inscricao.setStatus(StatusInscricao.CANCELADA);
        inscricaoRepository.save(inscricao);

        if (statusAnterior == StatusInscricao.CONFIRMADA) {
            promoverPrimeiroDaFila(inscricao.getTurma().getId());
        }
    }

    @Transactional(readOnly = true)
    public List<InscricaoResponse> listarMinhas(UUID alunoId) {
        return inscricaoRepository.findByAlunoId(alunoId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void promoverPrimeiroDaFila(UUID turmaId) {
        inscricaoRepository.findFirstByTurmaIdAndStatusOrderByPosicaoEsperaAsc(turmaId, StatusInscricao.LISTA_ESPERA)
                .ifPresent(i -> {
                    i.setStatus(StatusInscricao.CONFIRMADA);
                    i.setPosicaoEspera(null);
                    inscricaoRepository.save(i);
                });
    }

    private int calcularPosicaoEspera(UUID turmaId) {
        return (int) inscricaoRepository.countByTurmaIdAndStatus(turmaId, StatusInscricao.LISTA_ESPERA) + 1;
    }

    private InscricaoResponse toResponse(Inscricao inscricao) {
        return InscricaoResponse.builder()
                .id(inscricao.getId())
                .turmaId(inscricao.getTurma().getId())
                .turmaNome(inscricao.getTurma().getNome())
                .eventoTitulo(inscricao.getTurma().getEvento().getTitulo())
                .status(inscricao.getStatus().name())
                .posicaoEspera(inscricao.getPosicaoEspera())
                .presente(inscricao.getPresente())
                .criadaEm(inscricao.getCriadaEm())
                .build();
    }
}
