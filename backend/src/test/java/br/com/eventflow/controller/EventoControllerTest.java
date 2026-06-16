package br.com.eventflow.controller;

import br.com.eventflow.audit.AuditLogger;
import br.com.eventflow.config.SecurityConfig;
import br.com.eventflow.dto.response.EventoResponse;
import br.com.eventflow.security.AuthEntryPoint;
import br.com.eventflow.security.JwtAuthFilter;
import br.com.eventflow.security.JwtService;
import br.com.eventflow.service.EventoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventoController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, AuthEntryPoint.class})
class EventoControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private EventoService eventoService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AuditLogger auditLogger;
    @MockitoBean private br.com.eventflow.security.CustomUserDetailsService userDetailsService;

    private EventoResponse eventoResponse() {
        return EventoResponse.builder()
                .id(UUID.randomUUID())
                .titulo("Evento")
                .dataInicio(LocalDate.now().plusDays(5))
                .status("ABERTO")
                .build();
    }

    @Test
    void deveListarEventos_semAutenticacao() throws Exception {
        when(eventoService.listar(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(eventoResponse())));

        mockMvc.perform(get("/eventos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].titulo").value("Evento"));
    }

    @Test
    void deveCriarEvento_comAdmin() throws Exception {
        when(eventoService.criar(any())).thenReturn(eventoResponse());

        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        String body = "{\"titulo\":\"Novo\",\"dataInicio\":\"2027-01-01\"}";

        mockMvc.perform(post("/eventos")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Evento"));
    }

    @Test
    void deveRetornar401_quandoSemToken() throws Exception {
        mockMvc.perform(post("/eventos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"X\",\"dataInicio\":\"2027-01-01\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar403_quandoAlunoTentaCriar() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ALUNO")));

        mockMvc.perform(post("/eventos")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"X\",\"dataInicio\":\"2027-01-01\"}"))
                .andExpect(status().isForbidden());
    }
}
