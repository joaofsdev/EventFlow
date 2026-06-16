package br.com.eventflow.controller;

import br.com.eventflow.dto.request.LoginRequest;
import br.com.eventflow.dto.request.RefreshRequest;
import br.com.eventflow.dto.request.RegisterRequest;
import br.com.eventflow.dto.response.AuthResponse;
import br.com.eventflow.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registro, login, refresh e logout")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar novo aluno")
    @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Login — retorna access e refresh token")
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token via refresh token")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout — revoga refresh token")
    @ApiResponse(responseCode = "204", description = "Logout realizado")
    public ResponseEntity<Void> logout(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}
