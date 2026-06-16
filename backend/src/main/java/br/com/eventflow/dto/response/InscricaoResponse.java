package br.com.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InscricaoResponse {
    private UUID id;
    private UUID turmaId;
    private String turmaNome;
    private String eventoTitulo;
    private String status;
    private Integer posicaoEspera;
    private Boolean presente;
    private LocalDateTime criadaEm;
}
