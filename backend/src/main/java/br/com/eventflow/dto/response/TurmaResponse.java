package br.com.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class TurmaResponse {
    private UUID id;
    private String nome;
    private UUID eventoId;
    private String eventoTitulo;
    private UUID professorId;
    private String professorNome;
    private LocalDateTime dataHoraInicio;
    private LocalDateTime dataHoraFim;
    private Integer vagas;
    private String status;
}
