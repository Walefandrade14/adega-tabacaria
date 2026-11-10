package br.com.loudfeliz.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CancelamentoRequest(@Min(0) @Max(100) Integer percentualEstorno) {
}
