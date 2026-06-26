package br.com.eventflow.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ProfessorResponse {
    private UUID id;
    private String nome;
}
