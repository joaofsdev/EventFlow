package br.com.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardAlunoResponse {
    private long totalInscricoes;
    private long turmasConcluidas;
    private long presencasRegistradas;
    private List<InscricaoResponse> inscricoes;
}
