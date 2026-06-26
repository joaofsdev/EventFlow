package br.com.eventflow.controller;

import br.com.eventflow.dto.response.ProfessorResponse;
import br.com.eventflow.entity.Role;
import br.com.eventflow.repository.UsuarioRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Consultas de usuários")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    @GetMapping("/usuarios/professores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ProfessorResponse>> listarProfessores() {
        List<ProfessorResponse> professores = usuarioRepository.findAllByPapel(Role.PROFESSOR)
                .stream()
                .map(u -> ProfessorResponse.builder().id(u.getId()).nome(u.getNome()).build())
                .toList();
        return ResponseEntity.ok(professores);
    }
}
