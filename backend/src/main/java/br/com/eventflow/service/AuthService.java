package br.com.eventflow.service;

import br.com.eventflow.dto.request.LoginRequest;
import br.com.eventflow.dto.request.RefreshRequest;
import br.com.eventflow.dto.request.RegisterRequest;
import br.com.eventflow.dto.response.AuthResponse;
import br.com.eventflow.entity.RefreshToken;
import br.com.eventflow.entity.Role;
import br.com.eventflow.entity.Usuario;
import br.com.eventflow.repository.RefreshTokenRepository;
import br.com.eventflow.repository.UsuarioRepository;
import br.com.eventflow.security.JwtService;
import br.com.eventflow.audit.AuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuditLogger auditLogger;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public void register(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email já cadastrado");
        }

        Usuario usuario = Usuario.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .senha(passwordEncoder.encode(request.getSenha()))
                .papel(Role.ALUNO)
                .build();

        usuarioRepository.save(usuario);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha())
            );
        } catch (org.springframework.security.core.AuthenticationException e) {
            auditLogger.logLoginFalha(request.getEmail());
            throw e;
        }

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow();

        String accessToken = jwtService.generateToken(usuario.getId(), "ROLE_" + usuario.getPapel().name());
        String refreshToken = createRefreshToken(usuario);

        auditLogger.logLoginSucesso(usuario.getId(), usuario.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Refresh token inválido"));

        if (refreshToken.getRevogado() || refreshToken.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Refresh token expirado ou revogado");
        }

        Usuario usuario = refreshToken.getUsuario();
        String accessToken = jwtService.generateToken(usuario.getId(), "ROLE_" + usuario.getPapel().name());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .build();
    }

    @Transactional
    public void logout(UUID usuarioId) {
        refreshTokenRepository.deleteByUsuarioId(usuarioId);
    }

    private String createRefreshToken(Usuario usuario) {
        refreshTokenRepository.deleteByUsuarioId(usuario.getId());

        RefreshToken refreshToken = RefreshToken.builder()
                .usuario(usuario)
                .token(UUID.randomUUID().toString())
                .expiraEm(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .revogado(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshToken.getToken();
    }
}
