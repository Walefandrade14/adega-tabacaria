package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfiguracaoRequest(@NotBlank String valor) {
}
