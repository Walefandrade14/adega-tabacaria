package br.com.loudfeliz.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record HorarioRequest(@NotNull LocalTime horaAbertura, @NotNull LocalTime horaFechamento, boolean aceitaDelivery) {
}
