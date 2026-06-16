package br.com.eventflow.service;

import br.com.eventflow.dto.request.CriarEventoRequest;
import br.com.eventflow.dto.response.EventoResponse;
import br.com.eventflow.entity.Evento;
import br.com.eventflow.entity.StatusEvento;
import br.com.eventflow.exception.ResourceNotFoundException;
import br.com.eventflow.repository.EventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock private EventoRepository eventoRepository;
    @InjectMocks private EventoService eventoService;

    private Evento criarEvento() {
        return Evento.builder()
                .id(UUID.randomUUID())
                .titulo("Evento Teste")
                .descricao("Desc")
                .dataInicio(LocalDate.now().plusDays(10))
                .local("Sala 1")
                .status(StatusEvento.ABERTO)
                .build();
    }

    @Test
    void deveCriarEventoComStatusAberto() {
        CriarEventoRequest req = new CriarEventoRequest();
        req.setTitulo("Novo");
        req.setDataInicio(LocalDate.now().plusDays(5));

        Evento saved = criarEvento();
        when(eventoRepository.save(any())).thenReturn(saved);

        EventoResponse resp = eventoService.criar(req);
        assertThat(resp.getStatus()).isEqualTo("ABERTO");
        assertThat(resp.getId()).isNotNull();
    }

    @Test
    void deveListarEventosComPaginacao() {
        Page<Evento> page = new PageImpl<>(List.of(criarEvento()));
        when(eventoRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<EventoResponse> result = eventoService.listar(null, Pageable.unpaged());
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deveListarEventosFiltradosPorStatus() {
        Page<Evento> page = new PageImpl<>(List.of(criarEvento()));
        when(eventoRepository.findByStatus(eq(StatusEvento.ABERTO), any())).thenReturn(page);

        Page<EventoResponse> result = eventoService.listar(StatusEvento.ABERTO, Pageable.unpaged());
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void deveBuscarEventoPorId() {
        Evento evento = criarEvento();
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));

        EventoResponse resp = eventoService.buscarPorId(evento.getId());
        assertThat(resp.getTitulo()).isEqualTo("Evento Teste");
    }

    @Test
    void deveLancarErro_quandoEventoNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(eventoRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventoService.buscarPorId(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deveCancelarEvento() {
        Evento evento = criarEvento();
        when(eventoRepository.findById(evento.getId())).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenReturn(evento);

        eventoService.cancelar(evento.getId());
        assertThat(evento.getStatus()).isEqualTo(StatusEvento.CANCELADO);
    }
}
