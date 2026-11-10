package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilRequest(
        @NotBlank @Size(min = 2, max = 120) String nome,
        @Size(max = 20) String telefone,
        @Size(max = 14) String cpf) {
}
