package br.com.eventflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token é obrigatório")
    private String refreshToken;
}
