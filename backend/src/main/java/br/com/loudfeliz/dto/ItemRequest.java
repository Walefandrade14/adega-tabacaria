package br.com.loudfeliz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(@NotNull Long produtoId, @NotNull @Min(1) @Max(999) Integer quantidade) {
}
