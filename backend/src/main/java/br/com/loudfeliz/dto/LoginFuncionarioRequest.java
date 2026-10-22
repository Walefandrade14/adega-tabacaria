package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginFuncionarioRequest(@NotBlank String usuario, @NotBlank String senha) {
}
