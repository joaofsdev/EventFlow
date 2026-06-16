package br.com.eventflow.service;

import br.com.eventflow.dto.request.LoginRequest;
import br.com.eventflow.dto.request.RefreshRequest;
import br.com.eventflow.dto.request.RegisterRequest;
import br.com.eventflow.dto.response.AuthResponse;
import br.com.eventflow.entity.RefreshToken;
import br.com.eventflow.entity.Role;
import br.com.eventflow.entity.Usuario;
import br.com.eventflow.audit.AuditLogger;
import br.com.eventflow.repository.RefreshTokenRepository;
import br.com.eventflow.repository.UsuarioRepository;
import br.com.eventflow.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private AuditLogger auditLogger;

    @InjectMocks private AuthService authService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(UUID.randomUUID())
                .nome("Test")
                .email("test@test.com")
                .senha("encoded")
                .papel(Role.ALUNO)
                .build();
    }

    @Test
    void deveRegistrarUsuarioComSucesso() {
        RegisterRequest req = new RegisterRequest();
        req.setNome("Novo");
        req.setEmail("novo@test.com");
        req.setSenha("12345678");

        when(usuarioRepository.existsByEmail("novo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("12345678")).thenReturn("hashed");
        when(usuarioRepository.save(any())).thenReturn(usuario);

        assertThatCode(() -> authService.register(req)).doesNotThrowAnyException();
        verify(usuarioRepository).save(any());
    }

    @Test
    void deveLancarErro_quandoEmailJaCadastrado() {
        RegisterRequest req = new RegisterRequest();
        req.setNome("Dup");
        req.setEmail("dup@test.com");
        req.setSenha("12345678");

        when(usuarioRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email j\u00e1 cadastrado");
    }

    @Test
    void deveRetornarTokens_quandoLoginValido() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setSenha("senha");

        when(usuarioRepository.findByEmail("test@test.com")).thenReturn(Optional.of(usuario));
        when(jwtService.generateToken(any(), any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void deveLancarErro_quandoCredenciaisInvalidas() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setSenha("errada");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditLogger).logLoginFalha("test@test.com");
    }

    @Test
    void deveRenovarToken_quandoRefreshValido() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("valid-token");

        RefreshToken rt = RefreshToken.builder()
                .token("valid-token")
                .usuario(usuario)
                .expiraEm(LocalDateTime.now().plusDays(1))
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(rt));
        when(jwtService.generateToken(any(), any())).thenReturn("new-access");

        AuthResponse response = authService.refresh(req);
        assertThat(response.getAccessToken()).isEqualTo("new-access");
    }

    @Test
    void deveLancarErro_quandoRefreshTokenRevogado() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("revoked");

        RefreshToken rt = RefreshToken.builder()
                .token("revoked")
                .usuario(usuario)
                .expiraEm(LocalDateTime.now().plusDays(1))
                .revogado(true)
                .build();

        when(refreshTokenRepository.findByToken("revoked")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expirado ou revogado");
    }

    @Test
    void deveLancarErro_quandoRefreshTokenExpirado() {
        RefreshRequest req = new RefreshRequest();
        req.setRefreshToken("expired");

        RefreshToken rt = RefreshToken.builder()
                .token("expired")
                .usuario(usuario)
                .expiraEm(LocalDateTime.now().minusDays(1))
                .revogado(false)
                .build();

        when(refreshTokenRepository.findByToken("expired")).thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.refresh(req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deveExecutarLogout() {
        UUID userId = UUID.randomUUID();
        authService.logout(userId);
        verify(refreshTokenRepository).deleteByUsuarioId(userId);
    }
}
