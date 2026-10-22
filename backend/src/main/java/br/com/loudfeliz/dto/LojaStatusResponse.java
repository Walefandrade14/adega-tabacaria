package br.com.loudfeliz.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record LojaStatusResponse(
        boolean lojaAberta,
        boolean deliveryDisponivel,
        BigDecimal valorMinimoDelivery,
        LocalDateTime agora,
        List<HorarioResponse> horarios) {
}
