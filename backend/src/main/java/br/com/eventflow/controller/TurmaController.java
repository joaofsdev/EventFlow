package br.com.eventflow.controller;

import br.com.eventflow.dto.request.CriarTurmaRequest;
import br.com.eventflow.dto.response.TurmaResponse;
import br.com.eventflow.service.TurmaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Turmas", description = "CRUD de turmas vinculadas a eventos")
public class TurmaController {

    private final TurmaService turmaService;

    @PostMapping("/eventos/{eventoId}/turmas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TurmaResponse> criar(@PathVariable UUID eventoId,
                                               @Valid @RequestBody CriarTurmaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(turmaService.criar(eventoId, request));
    }

    @GetMapping("/eventos/{eventoId}/turmas")
    public ResponseEntity<List<TurmaResponse>> listarPorEvento(@PathVariable UUID eventoId) {
        return ResponseEntity.ok(turmaService.listarPorEvento(eventoId));
    }

    @PutMapping("/turmas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TurmaResponse> editar(@PathVariable UUID id,
                                                @Valid @RequestBody CriarTurmaRequest request) {
        return ResponseEntity.ok(turmaService.editar(id, request));
    }

    @DeleteMapping("/turmas/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        turmaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
