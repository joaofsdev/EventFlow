package br.com.eventflow.controller;

import br.com.eventflow.dto.response.DashboardAdminResponse;
import br.com.eventflow.dto.response.DashboardAlunoResponse;
import br.com.eventflow.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Visão geral e histórico de participação")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardAdminResponse> dashboardAdmin() {
        return ResponseEntity.ok(dashboardService.dashboardAdmin());
    }

    @GetMapping("/aluno")
    @PreAuthorize("hasRole('ALUNO')")
    public ResponseEntity<DashboardAlunoResponse> dashboardAluno(Authentication authentication) {
        UUID alunoId = (UUID) authentication.getPrincipal();
        return ResponseEntity.ok(dashboardService.dashboardAluno(alunoId));
    }
}
