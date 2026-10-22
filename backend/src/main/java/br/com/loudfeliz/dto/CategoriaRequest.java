package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(@NotBlank @Size(max = 60) String nome) {
}
