package br.com.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardAdminResponse {
    private long totalEventos;
    private long totalTurmas;
    private long totalInscricoes;
    private long totalAlunos;
    private double taxaOcupacao;
}
