package br.com.eventflow.service;

import br.com.eventflow.dto.response.InscricaoResponse;
import br.com.eventflow.entity.*;
import br.com.eventflow.exception.BusinessException;
import br.com.eventflow.exception.ConflictException;
import br.com.eventflow.exception.ResourceNotFoundException;
import br.com.eventflow.repository.InscricaoRepository;
import br.com.eventflow.repository.TurmaRepository;
import br.com.eventflow.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscricaoServiceTest {

    @Mock private InscricaoRepository inscricaoRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private InscricaoService inscricaoService;

    private UUID alunoId;
    private UUID turmaId;
    private Turma turma;
    private Usuario aluno;

    @BeforeEach
    void setUp() {
        alunoId = UUID.randomUUID();
        turmaId = UUID.randomUUID();

        Evento evento = Evento.builder().id(UUID.randomUUID()).titulo("Ev").status(StatusEvento.ABERTO).build();
        aluno = Usuario.builder().id(alunoId).nome("Aluno").email("a@t.com").papel(Role.ALUNO).build();
        turma = Turma.builder()
                .id(turmaId)
                .evento(evento)
                .professor(Usuario.builder().id(UUID.randomUUID()).build())
                .nome("T1")
                .dataHoraInicio(LocalDateTime.now().plusDays(5))
                .dataHoraFim(LocalDateTime.now().plusDays(5).plusHours(4))
                .vagas(10)
                .status(StatusTurma.ABERTA)
                .build();
    }

    @Test
    void deveInscreverComSucesso_quandoTemVaga() {
        when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turma));
        when(inscricaoRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)).thenReturn(false);
        when(inscricaoRepository.existsConflito(eq(alunoId), any(), any())).thenReturn(false);
        when(usuarioRepository.findById(alunoId)).thenReturn(Optional.of(aluno));
        when(inscricaoRepository.countByTurmaIdAndStatus(turmaId, StatusInscricao.CONFIRMADA)).thenReturn(5L);
        when(inscricaoRepository.save(any())).thenAnswer(i -> {
            Inscricao insc = i.getArgument(0);
            insc.setId(UUID.randomUUID());
            return insc;
        });

        InscricaoResponse resp = inscricaoService.inscrever(turmaId, alunoId);
        assertThat(resp.getStatus()).isEqualTo("CONFIRMADA");
    }

    @Test
    void deveColocarEmListaDeEspera_quandoSemVaga() {
        when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turma));
        when(inscricaoRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)).thenReturn(false);
        when(inscricaoRepository.existsConflito(eq(alunoId), any(), any())).thenReturn(false);
        when(usuarioRepository.findById(alunoId)).thenReturn(Optional.of(aluno));
        when(inscricaoRepository.countByTurmaIdAndStatus(turmaId, StatusInscricao.CONFIRMADA)).thenReturn(10L);
        when(inscricaoRepository.countByTurmaIdAndStatus(turmaId, StatusInscricao.LISTA_ESPERA)).thenReturn(0L);
        when(inscricaoRepository.save(any())).thenAnswer(i -> {
            Inscricao insc = i.getArgument(0);
            insc.setId(UUID.randomUUID());
            return insc;
        });

        InscricaoResponse resp = inscricaoService.inscrever(turmaId, alunoId);
        assertThat(resp.getStatus()).isEqualTo("LISTA_ESPERA");
        assertThat(resp.getPosicaoEspera()).isEqualTo(1);
    }

    @Test
    void deveLancarErro_quandoTurmaFechada() {
        turma.setStatus(StatusTurma.ENCERRADA);
        when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turma));

        assertThatThrownBy(() -> inscricaoService.inscrever(turmaId, alunoId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está aberta");
    }

    @Test
    void deveLancarErro_quandoInscricaoDuplicada() {
        when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turma));
        when(inscricaoRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)).thenReturn(true);

        assertThatThrownBy(() -> inscricaoService.inscrever(turmaId, alunoId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("já inscrito");
    }

    @Test
    void deveLancarErro_quandoConflitoDeHorario() {
        when(turmaRepository.findById(turmaId)).thenReturn(Optional.of(turma));
        when(inscricaoRepository.existsByAlunoIdAndTurmaId(alunoId, turmaId)).thenReturn(false);
        when(inscricaoRepository.existsConflito(eq(alunoId), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> inscricaoService.inscrever(turmaId, alunoId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Conflito de horário");
    }

    @Test
    void deveLancarErro_quandoTurmaNaoEncontrada() {
        when(turmaRepository.findById(turmaId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inscricaoService.inscrever(turmaId, alunoId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveCancelarEPromoverPrimeiroDaFila() {
        Inscricao inscricao = Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(aluno)
                .turma(turma)
                .status(StatusInscricao.CONFIRMADA)
                .build();

        Inscricao emEspera = Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(Usuario.builder().id(UUID.randomUUID()).nome("Outro").build())
                .turma(turma)
                .status(StatusInscricao.LISTA_ESPERA)
                .posicaoEspera(1)
                .build();

        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));
        when(inscricaoRepository.findFirstByTurmaIdAndStatusOrderByPosicaoEsperaAsc(turmaId, StatusInscricao.LISTA_ESPERA))
                .thenReturn(Optional.of(emEspera));
        when(inscricaoRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        inscricaoService.cancelar(inscricao.getId(), alunoId);

        assertThat(inscricao.getStatus()).isEqualTo(StatusInscricao.CANCELADA);
        assertThat(emEspera.getStatus()).isEqualTo(StatusInscricao.CONFIRMADA);
        assertThat(emEspera.getPosicaoEspera()).isNull();
    }

    @Test
    void deveLancarErro_quandoCancelarInscricaoDeOutroAluno() {
        UUID outroAluno = UUID.randomUUID();
        Inscricao inscricao = Inscricao.builder()
                .id(UUID.randomUUID())
                .aluno(aluno)
                .turma(turma)
                .status(StatusInscricao.CONFIRMADA)
                .build();

        when(inscricaoRepository.findById(inscricao.getId())).thenReturn(Optional.of(inscricao));

        assertThatThrownBy(() -> inscricaoService.cancelar(inscricao.getId(), outroAluno))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("próprias");
    }
}
