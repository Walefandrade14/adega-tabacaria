package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Cargo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FuncionarioRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank String cpf,
        @NotNull Cargo cargo,
        @NotBlank @Size(max = 60) String usuario,
        @NotBlank @Size(min = 6, max = 72) String senha) {
}
