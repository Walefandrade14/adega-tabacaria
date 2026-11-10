package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NovaSenhaRequest(@NotBlank @Size(min = 6, max = 72) String senha) {
}