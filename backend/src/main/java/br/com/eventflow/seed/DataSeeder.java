package br.com.eventflow.seed;

import br.com.eventflow.entity.*;
import br.com.eventflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile({"dev", "prod"})
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final EventoRepository eventoRepository;
    private final TurmaRepository turmaRepository;
    private final InscricaoRepository inscricaoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() > 0) return;

        // Usuários demo
        Usuario admin = usuarioRepository.save(Usuario.builder()
                .nome("Administrador")
                .email("admin@eventflow.dev")
                .senha(passwordEncoder.encode("Admin@1234"))
                .papel(Role.ADMIN)
                .build());

        Usuario professor = usuarioRepository.save(Usuario.builder()
                .nome("Professor Demo")
                .email("professor@eventflow.dev")
                .senha(passwordEncoder.encode("Prof@1234"))
                .papel(Role.PROFESSOR)
                .build());

        Usuario alunoPrincipal = usuarioRepository.save(Usuario.builder()
                .nome("Aluno Demo")
                .email("aluno@eventflow.dev")
                .senha(passwordEncoder.encode("Aluno@1234"))
                .papel(Role.ALUNO)
                .build());

        // 10 alunos fictícios
        List<Usuario> alunos = new ArrayList<>();
        alunos.add(alunoPrincipal);
        for (int i = 1; i <= 9; i++) {
            alunos.add(usuarioRepository.save(Usuario.builder()
                    .nome("Aluno " + i)
                    .email("aluno" + i + "@eventflow.dev")
                    .senha(passwordEncoder.encode("Aluno@1234"))
                    .papel(Role.ALUNO)
                    .build()));
        }

        // 3 eventos
        Evento evento1 = eventoRepository.save(Evento.builder()
                .titulo("Semana de Tecnologia 2026")
                .descricao("Evento com minicursos e oficinas de TI")
                .dataInicio(LocalDate.now().plusDays(30))
                .dataFim(LocalDate.now().plusDays(35))
                .local("Bloco A - Auditório")
                .cargaHoraria(20)
                .status(StatusEvento.ABERTO)
                .build());

        Evento evento2 = eventoRepository.save(Evento.builder()
                .titulo("Workshop de Spring Boot")
                .descricao("Workshop prático de desenvolvimento com Spring Boot")
                .dataInicio(LocalDate.now().plusDays(15))
                .dataFim(LocalDate.now().plusDays(15))
                .local("Lab 3")
                .cargaHoraria(8)
                .status(StatusEvento.ABERTO)
                .build());

        Evento evento3 = eventoRepository.save(Evento.builder()
                .titulo("Oficina de React")
                .descricao("Construção de interfaces modernas com React")
                .dataInicio(LocalDate.now().plusDays(45))
                .dataFim(LocalDate.now().plusDays(46))
                .local("Lab 2")
                .cargaHoraria(12)
                .status(StatusEvento.ABERTO)
                .build());

        // Turmas
        Turma turma1 = turmaRepository.save(Turma.builder()
                .evento(evento1)
                .professor(professor)
                .nome("Python Básico - Manhã")
                .dataHoraInicio(LocalDateTime.now().plusDays(30).withHour(8).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(30).withHour(12).withMinute(0))
                .vagas(5)
                .status(StatusTurma.ABERTA)
                .build());

        Turma turma2 = turmaRepository.save(Turma.builder()
                .evento(evento1)
                .professor(professor)
                .nome("Python Básico - Tarde")
                .dataHoraInicio(LocalDateTime.now().plusDays(30).withHour(14).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(30).withHour(18).withMinute(0))
                .vagas(20)
                .status(StatusTurma.ABERTA)
                .build());

        Turma turma3 = turmaRepository.save(Turma.builder()
                .evento(evento2)
                .professor(professor)
                .nome("Spring Boot Intensivo")
                .dataHoraInicio(LocalDateTime.now().plusDays(15).withHour(9).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(15).withHour(17).withMinute(0))
                .vagas(15)
                .status(StatusTurma.ABERTA)
                .build());

        Turma turma4 = turmaRepository.save(Turma.builder()
                .evento(evento3)
                .professor(professor)
                .nome("React do Zero")
                .dataHoraInicio(LocalDateTime.now().plusDays(45).withHour(8).withMinute(0))
                .dataHoraFim(LocalDateTime.now().plusDays(45).withHour(12).withMinute(0))
                .vagas(10)
                .status(StatusTurma.ABERTA)
                .build());

        // Inscrições - turma1 lotada (5 vagas, 5 confirmados + 2 em espera)
        for (int i = 0; i < 5; i++) {
            Inscricao insc = inscricaoRepository.save(Inscricao.builder()
                    .aluno(alunos.get(i))
                    .turma(turma1)
                    .status(StatusInscricao.CONFIRMADA)
                    .presente(i < 4)
                    .build());
        }
        // Lista de espera na turma1
        for (int i = 5; i < 7; i++) {
            inscricaoRepository.save(Inscricao.builder()
                    .aluno(alunos.get(i))
                    .turma(turma1)
                    .status(StatusInscricao.LISTA_ESPERA)
                    .posicaoEspera(i - 4)
                    .build());
        }

        // Inscrições na turma2
        for (int i = 0; i < 3; i++) {
            inscricaoRepository.save(Inscricao.builder()
                    .aluno(alunos.get(i + 5))
                    .turma(turma2)
                    .status(StatusInscricao.CONFIRMADA)
                    .build());
        }

        // Inscrições na turma3
        for (int i = 0; i < 4; i++) {
            inscricaoRepository.save(Inscricao.builder()
                    .aluno(alunos.get(i))
                    .turma(turma3)
                    .status(StatusInscricao.CONFIRMADA)
                    .presente(i < 3)
                    .build());
        }

        System.out.println("[DataSeeder] Banco populado com dados de demonstração.");
    }
}
