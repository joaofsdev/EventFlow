package br.com.eventflow.controller;

import br.com.eventflow.audit.AuditLogger;
import br.com.eventflow.dto.response.InscricaoResponse;
import br.com.eventflow.service.InscricaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Inscrições", description = "Inscrição, cancelamento e lista de espera")
public class InscricaoController {

    private final InscricaoService inscricaoService;
    private final AuditLogger auditLogger;

    @PostMapping("/turmas/{turmaId}/inscricoes")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<InscricaoResponse> inscrever(@PathVariable UUID turmaId,
                                                       Authentication authentication) {
        UUID alunoId = (UUID) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricaoService.inscrever(turmaId, alunoId));
    }

    @DeleteMapping("/inscricoes/{id}")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id, Authentication authentication) {
        UUID alunoId = (UUID) authentication.getPrincipal();
        inscricaoService.cancelar(id, alunoId);
        auditLogger.logAcao("CANCELAR_INSCRICAO", alunoId, "inscricaoId=" + id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/inscricoes/minhas")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<List<InscricaoResponse>> listarMinhas(Authentication authentication) {
        UUID alunoId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(inscricaoService.listarMinhas(alunoId));
    }
}
