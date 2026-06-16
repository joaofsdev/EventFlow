package br.com.eventflow.service;

import br.com.eventflow.dto.request.CriarEventoRequest;
import br.com.eventflow.dto.response.EventoResponse;
import br.com.eventflow.entity.Evento;
import br.com.eventflow.entity.StatusEvento;
import br.com.eventflow.exception.ResourceNotFoundException;
import br.com.eventflow.repository.EventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;

    @Transactional
    public EventoResponse criar(CriarEventoRequest request) {
        Evento evento = Evento.builder()
                .titulo(request.getTitulo())
                .descricao(request.getDescricao())
                .dataInicio(request.getDataInicio())
                .dataFim(request.getDataFim())
                .local(request.getLocal())
                .cargaHoraria(request.getCargaHoraria())
                .status(StatusEvento.ABERTO)
                .build();

        evento = eventoRepository.save(evento);
        return toResponse(evento);
    }

    public Page<EventoResponse> listar(StatusEvento status, Pageable pageable) {
        Page<Evento> page = (status != null)
                ? eventoRepository.findByStatus(status, pageable)
                : eventoRepository.findAll(pageable);

        return page.map(this::toResponse);
    }

    public EventoResponse buscarPorId(UUID id) {
        Evento evento = findOrThrow(id);
        return toResponse(evento);
    }

    @Transactional
    public EventoResponse editar(UUID id, CriarEventoRequest request) {
        Evento evento = findOrThrow(id);

        evento.setTitulo(request.getTitulo());
        evento.setDescricao(request.getDescricao());
        evento.setDataInicio(request.getDataInicio());
        evento.setDataFim(request.getDataFim());
        evento.setLocal(request.getLocal());
        evento.setCargaHoraria(request.getCargaHoraria());

        evento = eventoRepository.save(evento);
        return toResponse(evento);
    }

    @Transactional
    public void cancelar(UUID id) {
        Evento evento = findOrThrow(id);
        evento.setStatus(StatusEvento.CANCELADO);
        eventoRepository.save(evento);
    }

    private Evento findOrThrow(UUID id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado: " + id));
    }

    private EventoResponse toResponse(Evento evento) {
        return EventoResponse.builder()
                .id(evento.getId())
                .titulo(evento.getTitulo())
                .descricao(evento.getDescricao())
                .dataInicio(evento.getDataInicio())
                .dataFim(evento.getDataFim())
                .local(evento.getLocal())
                .cargaHoraria(evento.getCargaHoraria())
                .status(evento.getStatus().name())
                .criadoEm(evento.getCriadoEm())
                .build();
    }
}
