package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record PagamentoRequest(@NotNull FormaPagamento forma, boolean simularRecusa) {
}
