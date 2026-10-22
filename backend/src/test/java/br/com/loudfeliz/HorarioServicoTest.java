package br.com.loudfeliz;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.loudfeliz.dominio.HorarioFuncionamento;
import br.com.loudfeliz.servico.HorarioServico;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorarioServicoTest {

    private static HorarioFuncionamento horario(int dia, String abre, String fecha, boolean delivery) {
        HorarioFuncionamento h = new HorarioFuncionamento();
        h.setDiaSemana(dia);
        h.setHoraAbertura(LocalTime.parse(abre));
        h.setHoraFechamento(LocalTime.parse(fecha));
        h.setAceitaDelivery(delivery);
        return h;
    }

    @Test
    void intervaloQueCruzaMeiaNoitePertenceAoDiaDaAbertura() {
        List<HorarioFuncionamento> segunda = List.of(horario(1, "18:00", "02:00", true));
        assertTrue(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-19T19:00"), segunda, true));
        assertTrue(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-20T01:30"), segunda, true));
        assertFalse(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-20T02:00"), segunda, true));
        assertFalse(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-19T17:59"), segunda, true));
        assertFalse(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-19T01:00"), segunda, true));
    }

    @Test
    void intervaloNoMesmoDia() {
        List<HorarioFuncionamento> quinta = List.of(horario(4, "10:00", "22:00", true));
        assertTrue(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-22T10:00"), quinta, true));
        assertFalse(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-22T22:00"), quinta, true));
        assertFalse(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-23T11:00"), quinta, true));
    }

    @Test
    void diaSemDeliveryContaComoLojaAbertaMasSemEntrega() {
        List<HorarioFuncionamento> quinta = List.of(horario(4, "10:00", "22:00", false));
        assertFalse(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-22T12:00"), quinta, true));
        assertTrue(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-22T12:00"), quinta, false));
    }

    @Test
    void aberturaIgualAoFechamentoSignificaVinteEQuatroHoras() {
        List<HorarioFuncionamento> quinta = List.of(horario(4, "00:00", "00:00", true));
        assertTrue(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-22T03:00"), quinta, true));
        assertTrue(HorarioServico.dentroDoHorario(LocalDateTime.parse("2026-10-22T23:59"), quinta, true));
    }
}
