package br.com.eventflow.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void deveRetornar404_paraResourceNotFound() {
        ResponseEntity<Map<String, Object>> resp = handler.handleNotFound(
                new ResourceNotFoundException("Não encontrado"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody().get("erro")).isEqualTo("Recurso não encontrado");
        assertThat(resp.getBody().get("detalhes")).isEqualTo("Não encontrado");
    }

    @Test
    void deveRetornar409_paraConflict() {
        ResponseEntity<Map<String, Object>> resp = handler.handleConflict(
                new ConflictException("Duplicado"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resp.getBody().get("erro")).isEqualTo("Conflito");
    }

    @Test
    void deveRetornar422_paraBusiness() {
        ResponseEntity<Map<String, Object>> resp = handler.handleBusiness(
                new BusinessException("Regra violada"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(resp.getBody().get("detalhes")).isEqualTo("Regra violada");
    }

    @Test
    void deveRetornar400_paraIllegalArgument() {
        ResponseEntity<Map<String, Object>> resp = handler.handleIllegalArgument(
                new IllegalArgumentException("Inválido"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void deveRetornar403_paraAccessDenied() {
        ResponseEntity<Map<String, Object>> resp = handler.handleAccessDenied(
                new AccessDeniedException("Denied"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().get("detalhes")).isEqualTo("Você não tem permissão para executar esta ação.");
    }

    @Test
    void deveRetornar401_paraAuthentication() {
        ResponseEntity<Map<String, Object>> resp = handler.handleAuth(
                new BadCredentialsException("Bad"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().get("detalhes")).isEqualTo("Credenciais inválidas");
    }

    @Test
    void deveRetornar500_paraExcecaoGenerica() {
        ResponseEntity<Map<String, Object>> resp = handler.handleGeneric(
                new RuntimeException("Erro interno secreto"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody().get("detalhes")).isEqualTo("Ocorreu um erro inesperado");
    }

    @Test
    void deveRetornar400_paraValidacao() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "obj");
        binding.addError(new FieldError("obj", "nome", "Nome é obrigatório"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, binding);

        ResponseEntity<Map<String, Object>> resp = handler.handleValidation(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody().get("erro")).isEqualTo("Validação falhou");
    }
}
