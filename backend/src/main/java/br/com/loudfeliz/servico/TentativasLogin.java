package br.com.loudfeliz.servico;

import br.com.loudfeliz.dominio.TentativaLogin;
import br.com.loudfeliz.excecao.MuitasTentativasException;
import br.com.loudfeliz.repositorio.TentativaLoginRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TentativasLogin {

    private static final int TAMANHO_CHAVE = 200;

    private final int maximo;
    private final Duration bloqueio;
    private final Clock relogio;
    private final TentativaLoginRepositorio registros;

    @Autowired
    public TentativasLogin(@Value("${app.login.max-tentativas:5}") int maximo,
            @Value("${app.login.bloqueio-minutos:15}") long minutos, TentativaLoginRepositorio registros) {
        this(maximo, Duration.ofMinutes(minutos), Clock.systemUTC(), registros);
    }

    TentativasLogin(int maximo, Duration bloqueio, Clock relogio, TentativaLoginRepositorio registros) {
        this.maximo = maximo;
        this.bloqueio = bloqueio;
        this.relogio = relogio;
        this.registros = registros;
    }

    public void verificar(String conta) {
        Instant agora = relogio.instant();
        registros.findById(chave(conta))
                .map(TentativaLogin::getBloqueadoAte)
                .filter(agora::isBefore)
                .ifPresent(ate -> {
                    throw new MuitasTentativasException(Duration.between(agora, ate).toSeconds());
                });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void falhou(String conta) {
        Instant agora = relogio.instant();
        registros.apagarVencidas(agora.minus(bloqueio), agora);
        String chave = chave(conta);
        TentativaLogin registro = registros.findById(chave).orElse(null);
        boolean recomecar = registro == null || registro.getInicio().plus(bloqueio).isBefore(agora)
                || (registro.getBloqueadoAte() != null && !agora.isBefore(registro.getBloqueadoAte()));
        if (registro == null) {
            registro = new TentativaLogin();
            registro.setConta(chave);
        }
        int falhas = recomecar ? 1 : registro.getFalhas() + 1;
        registro.setFalhas(falhas);
        registro.setInicio(recomecar ? agora : registro.getInicio());
        registro.setBloqueadoAte(falhas >= maximo ? agora.plus(bloqueio) : null);
        registros.save(registro);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sucesso(String conta) {
        registros.deleteById(chave(conta));
    }

    private static String chave(String conta) {
        String chave = conta.trim().toLowerCase(Locale.ROOT);
        return chave.length() > TAMANHO_CHAVE ? chave.substring(0, TAMANHO_CHAVE) : chave;
    }
}
