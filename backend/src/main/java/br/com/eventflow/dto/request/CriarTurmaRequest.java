package br.com.eventflow.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CriarTurmaRequest {

    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotNull(message = "Data/hora de início é obrigatória")
    @Future(message = "Data/hora de início deve ser futura")
    private LocalDateTime dataHoraInicio;

    @NotNull(message = "Data/hora de fim é obrigatória")
    @Future(message = "Data/hora de fim deve ser futura")
    private LocalDateTime dataHoraFim;

    @NotNull(message = "Número de vagas é obrigatório")
    @Min(value = 1, message = "Número mínimo de vagas é 1")
    @Max(value = 500, message = "Número máximo de vagas é 500")
    private Integer vagas;

    @NotNull(message = "Professor é obrigatório")
    private UUID professorId;
}
