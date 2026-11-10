package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.HorarioFuncionamento;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

public record HorarioResponse(Integer diaSemana, String nomeDia, LocalTime horaAbertura, LocalTime horaFechamento, boolean aceitaDelivery) {

    public static HorarioResponse de(HorarioFuncionamento h) {
        String nome = DayOfWeek.of(h.getDiaSemana()).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR"));
        return new HorarioResponse(h.getDiaSemana(), nome, h.getHoraAbertura(), h.getHoraFechamento(), h.isAceitaDelivery());
    }
}
