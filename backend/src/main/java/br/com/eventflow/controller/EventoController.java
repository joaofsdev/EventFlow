package br.com.eventflow.controller;

import br.com.eventflow.audit.AuditLogger;
import br.com.eventflow.dto.request.CriarEventoRequest;
import br.com.eventflow.dto.response.EventoResponse;
import br.com.eventflow.entity.StatusEvento;
import br.com.eventflow.service.EventoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "CRUD de eventos acadêmicos")
public class EventoController {

    private final EventoService eventoService;
    private final AuditLogger auditLogger;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar evento", description = "Apenas ADMIN")
    public ResponseEntity<EventoResponse> criar(@Valid @RequestBody CriarEventoRequest request,
                                                 Authentication authentication) {
        EventoResponse response = eventoService.criar(request);
        UUID userId = (UUID) authentication.getPrincipal();
        auditLogger.logAcao("CRIAR_EVENTO", userId, "eventoId=" + response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<EventoResponse>> listar(
            @RequestParam(required = false) StatusEvento status,
            Pageable pageable) {
        return ResponseEntity.ok(eventoService.listar(status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(eventoService.buscarPorId(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventoResponse> editar(@PathVariable UUID id, @Valid @RequestBody CriarEventoRequest request) {
        return ResponseEntity.ok(eventoService.editar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        eventoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
