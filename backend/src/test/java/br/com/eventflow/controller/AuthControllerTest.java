package br.com.eventflow.controller;

import br.com.eventflow.dto.response.AuthResponse;
import br.com.eventflow.config.SecurityConfig;
import br.com.eventflow.security.AuthEntryPoint;
import br.com.eventflow.security.JwtAuthFilter;
import br.com.eventflow.security.JwtService;
import br.com.eventflow.audit.AuditLogger;
import br.com.eventflow.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, AuthEntryPoint.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AuditLogger auditLogger;
    @MockitoBean private br.com.eventflow.security.CustomUserDetailsService userDetailsService;

    @Test
    void deveRegistrarComSucesso_retorna201() throws Exception {
        String body = "{\"nome\":\"Test\",\"email\":\"t@t.com\",\"senha\":\"12345678\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        verify(authService).register(any());
    }

    @Test
    void deveRetornar400_quandoPayloadInvalido() throws Exception {
        String body = "{\"nome\":\"\",\"email\":\"invalido\",\"senha\":\"123\"}";

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detalhes").exists());
    }

    @Test
    void deveRetornarTokens_quandoLoginValido() throws Exception {
        AuthResponse resp = AuthResponse.builder().accessToken("at").refreshToken("rt").build();
        when(authService.login(any())).thenReturn(resp);

        String body = "{\"email\":\"t@t.com\",\"senha\":\"12345678\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("at"))
                .andExpect(jsonPath("$.refreshToken").value("rt"));
    }

    @Test
    void deveRetornar400_quandoLoginSemEmail() throws Exception {
        String body = "{\"senha\":\"12345678\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
