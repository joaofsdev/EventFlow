package br.com.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PresencaResponse {
    private UUID inscricaoId;
    private UUID alunoId;
    private String alunoNome;
    private String status;
    private Boolean presente;
}
