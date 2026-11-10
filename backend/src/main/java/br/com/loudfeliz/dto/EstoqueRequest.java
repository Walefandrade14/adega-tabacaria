package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotNull;

public record EstoqueRequest(@NotNull Integer ajuste) {
}
