package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.Fuso;
import br.com.loudfeliz.dominio.HorarioFuncionamento;
import br.com.loudfeliz.dto.HorarioRequest;
import br.com.loudfeliz.dto.HorarioResponse;
import br.com.loudfeliz.dto.LojaStatusResponse;
import br.com.loudfeliz.excecao.RequisicaoInvalidaException;
import br.com.loudfeliz.repositorio.HorarioRepositorio;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HorarioServico {

    private final HorarioRepositorio horarios;
    private final ConfiguracaoServico configuracoes;

    public HorarioServico(HorarioRepositorio horarios, ConfiguracaoServico configuracoes) {
        this.horarios = horarios;
        this.configuracoes = configuracoes;
    }

    @Transactional(readOnly = true)
    public boolean deliveryDisponivel(LocalDateTime momento) {
        return dentroDoHorario(momento, horarios.findAll(), true);
    }

    @Transactional(readOnly = true)
    public LojaStatusResponse status() {
        LocalDateTime agora = Fuso.agora();
        List<HorarioFuncionamento> lista = horarios.findAllByOrderByDiaSemanaAsc();
        return new LojaStatusResponse(
                dentroDoHorario(agora, lista, false),
                dentroDoHorario(agora, lista, true),
                configuracoes.valorMinimoDelivery(),
                agora,
                lista.stream().map(HorarioResponse::de).toList());
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listar() {
        return horarios.findAllByOrderByDiaSemanaAsc().stream().map(HorarioResponse::de).toList();
    }

    @Transactional
    public HorarioResponse atualizar(int diaSemana, HorarioRequest requisicao) {
        if (diaSemana < 1 || diaSemana > 7) {
            throw new RequisicaoInvalidaException("O dia da semana vai de 1 (segunda) a 7 (domingo).");
        }
        HorarioFuncionamento horario = horarios.findByDiaSemana(diaSemana).orElseGet(() -> {
            HorarioFuncionamento novo = new HorarioFuncionamento();
            novo.setDiaSemana(diaSemana);
            return novo;
        });
        horario.setHoraAbertura(requisicao.horaAbertura());
        horario.setHoraFechamento(requisicao.horaFechamento());
        horario.setAceitaDelivery(requisicao.aceitaDelivery());
        return HorarioResponse.de(horarios.save(horario));
    }

    public static boolean dentroDoHorario(LocalDateTime momento, Collection<HorarioFuncionamento> lista, boolean somenteDelivery) {
        int hoje = momento.getDayOfWeek().getValue();
        int ontem = momento.minusDays(1).getDayOfWeek().getValue();
        LocalTime hora = momento.toLocalTime();
        for (HorarioFuncionamento h : lista) {
            if (somenteDelivery && !h.isAceitaDelivery()) {
                continue;
            }
            LocalTime abre = h.getHoraAbertura();
            LocalTime fecha = h.getHoraFechamento();
            int dia = h.getDiaSemana();
            if (abre.equals(fecha)) {
                if (dia == hoje) {
                    return true;
                }
                continue;
            }
            boolean cruzaMeiaNoite = fecha.isBefore(abre);
            if (dia == hoje) {
                boolean aberto = cruzaMeiaNoite
                        ? !hora.isBefore(abre)
                        : !hora.isBefore(abre) && hora.isBefore(fecha);
                if (aberto) {
                    return true;
                }
            }
            if (dia == ontem && cruzaMeiaNoite && hora.isBefore(fecha)) {
                return true;
            }
        }
        return false;
    }
}
