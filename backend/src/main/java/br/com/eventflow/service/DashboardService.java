package br.com.eventflow.service;

import br.com.eventflow.dto.response.DashboardAdminResponse;
import br.com.eventflow.dto.response.DashboardAlunoResponse;
import br.com.eventflow.dto.response.InscricaoResponse;
import br.com.eventflow.entity.Inscricao;
import br.com.eventflow.entity.StatusInscricao;
import br.com.eventflow.repository.EventoRepository;
import br.com.eventflow.repository.InscricaoRepository;
import br.com.eventflow.repository.TurmaRepository;
import br.com.eventflow.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EventoRepository eventoRepository;
    private final TurmaRepository turmaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardAdminResponse dashboardAdmin() {
        long totalEventos = eventoRepository.count();
        long totalTurmas = turmaRepository.count();
        long totalInscricoes = inscricaoRepository.count();
        long totalAlunos = usuarioRepository.count();

        long totalVagas = turmaRepository.findAll().stream()
                .mapToInt(t -> t.getVagas())
                .sum();
        long confirmadas = inscricaoRepository.findAll().stream()
                .filter(i -> i.getStatus() == StatusInscricao.CONFIRMADA)
                .count();

        double taxaOcupacao = totalVagas > 0 ? (double) confirmadas / totalVagas * 100 : 0;

        return DashboardAdminResponse.builder()
                .totalEventos(totalEventos)
                .totalTurmas(totalTurmas)
                .totalInscricoes(totalInscricoes)
                .totalAlunos(totalAlunos)
                .taxaOcupacao(Math.round(taxaOcupacao * 100.0) / 100.0)
                .build();
    }

    public DashboardAlunoResponse dashboardAluno(UUID alunoId) {
        List<Inscricao> inscricoes = inscricaoRepository.findByAlunoId(alunoId);

        long totalInscricoes = inscricoes.stream()
                .filter(i -> i.getStatus() != StatusInscricao.CANCELADA)
                .count();

        long turmasConcluidas = inscricoes.stream()
                .filter(i -> i.getStatus() == StatusInscricao.CONFIRMADA && Boolean.TRUE.equals(i.getPresente()))
                .count();

        long presencasRegistradas = inscricoes.stream()
                .filter(i -> i.getPresente() != null)
                .count();

        List<InscricaoResponse> inscricaoResponses = inscricoes.stream()
                .filter(i -> i.getStatus() != StatusInscricao.CANCELADA)
                .map(this::toInscricaoResponse)
                .toList();

        return DashboardAlunoResponse.builder()
                .totalInscricoes(totalInscricoes)
                .turmasConcluidas(turmasConcluidas)
                .presencasRegistradas(presencasRegistradas)
                .inscricoes(inscricaoResponses)
                .build();
    }

    private InscricaoResponse toInscricaoResponse(Inscricao inscricao) {
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
