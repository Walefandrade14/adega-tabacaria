package br.com.loudfeliz.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ComboItemRequest(@NotNull Long produtoId, @NotNull @Min(1) Integer quantidade) {
}
