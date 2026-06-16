package br.com.eventflow.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RegistrarPresencaRequest {

    @NotEmpty(message = "Lista de presenças não pode estar vazia")
    @Valid
    private List<PresencaItem> presencas;

    @Data
    public static class PresencaItem {
        @jakarta.validation.constraints.NotNull(message = "ID da inscrição é obrigatório")
        private java.util.UUID inscricaoId;

        @jakarta.validation.constraints.NotNull(message = "Campo presente é obrigatório")
        private Boolean presente;
    }
}
