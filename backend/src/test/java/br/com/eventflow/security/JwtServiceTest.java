package br.com.eventflow.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "testSecretKeyThatIsLongEnoughForHS256Algorithm!!");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900000L);
    }

    @Test
    void deveGerarTokenValido() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "ROLE_ADMIN");

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void deveExtrairUserIdDoToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "ROLE_ALUNO");

        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void deveExtrairRoleDoToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "ROLE_PROFESSOR");

        assertThat(jwtService.extractRole(token)).isEqualTo("ROLE_PROFESSOR");
    }

    @Test
    void deveRetornarFalso_paraTokenInvalido() {
        assertThat(jwtService.isTokenValid("token.invalido.xyz")).isFalse();
    }

    @Test
    void deveRetornarFalso_paraTokenExpirado() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "ROLE_ADMIN");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
