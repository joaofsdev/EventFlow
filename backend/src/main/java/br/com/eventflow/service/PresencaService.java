package br.com.eventflow.service;

import br.com.eventflow.dto.request.RegistrarPresencaRequest;
import br.com.eventflow.dto.response.PresencaResponse;
import br.com.eventflow.entity.Inscricao;
import br.com.eventflow.entity.StatusInscricao;
import br.com.eventflow.entity.Turma;
import br.com.eventflow.exception.BusinessException;
import br.com.eventflow.exception.ResourceNotFoundException;
import br.com.eventflow.entity.Role;
import br.com.eventflow.entity.Usuario;
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
public class PresencaService {

    private final InscricaoRepository inscricaoRepository;
    private final TurmaRepository turmaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public void registrar(UUID turmaId, UUID usuarioId, RegistrarPresencaRequest request) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada: " + turmaId));

        validarAcesso(turma, usuarioId);

        for (RegistrarPresencaRequest.PresencaItem item : request.getPresencas()) {
            Inscricao inscricao = inscricaoRepository.findById(item.getInscricaoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inscrição não encontrada: " + item.getInscricaoId()));

            if (!inscricao.getTurma().getId().equals(turmaId)) {
                throw new BusinessException("Inscrição " + item.getInscricaoId() + " não pertence a esta turma");
            }

            inscricao.setPresente(item.getPresente());
            inscricaoRepository.save(inscricao);
        }
    }

    public List<PresencaResponse> listar(UUID turmaId, UUID usuarioId) {
        Turma turma = turmaRepository.findById(turmaId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada: " + turmaId));

        validarAcesso(turma, usuarioId);

        return inscricaoRepository.findByTurmaIdAndStatus(turmaId, StatusInscricao.CONFIRMADA).stream()
                .map(this::toResponse)
                .toList();
    }

    private void validarAcesso(Turma turma, UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
        if (usuario.getPapel() == Role.ADMIN) {
            return;
        }
        if (!turma.getProfessor().getId().equals(usuarioId)) {
            throw new BusinessException("Apenas o professor responsável pode gerenciar presença desta turma");
        }
    }

    private PresencaResponse toResponse(Inscricao inscricao) {
        return PresencaResponse.builder()
                .inscricaoId(inscricao.getId())
                .alunoId(inscricao.getAluno().getId())
                .alunoNome(inscricao.getAluno().getNome())
                .status(inscricao.getStatus().name())
                .presente(inscricao.getPresente())
                .build();
    }
}
