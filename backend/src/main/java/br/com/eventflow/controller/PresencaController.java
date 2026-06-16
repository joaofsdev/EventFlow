package br.com.eventflow.controller;

import br.com.eventflow.dto.request.RegistrarPresencaRequest;
import br.com.eventflow.dto.response.PresencaResponse;
import br.com.eventflow.service.PresencaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/turmas/{turmaId}/presenca")
@RequiredArgsConstructor
@Tag(name = "Presença", description = "Registro e consulta de presença por turma")
public class PresencaController {

    private final PresencaService presencaService;

    @PutMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<Void> registrar(@PathVariable UUID turmaId,
                                          @Valid @RequestBody RegistrarPresencaRequest request,
                                          Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        presencaService.registrar(turmaId, usuarioId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PROFESSOR', 'ADMIN')")
    public ResponseEntity<List<PresencaResponse>> listar(@PathVariable UUID turmaId,
                                                         Authentication authentication) {
        UUID usuarioId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(presencaService.listar(turmaId, usuarioId));
    }
}
