package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.StatusPedido;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull StatusPedido status) {
}
