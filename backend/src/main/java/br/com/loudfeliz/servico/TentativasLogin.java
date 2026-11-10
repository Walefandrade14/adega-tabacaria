package br.com.loudfeliz.servico;

import br.com.loudfeliz.excecao.MuitasTentativasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TentativasLogin {

    private static final int LIMPAR_ACIMA_DE = 10_000;

    private final int maximo;
    private final Duration bloqueio;
    private final Clock relogio;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    @Autowired
    public TentativasLogin(@Value("${app.login.max-tentativas:5}") int maximo,
            @Value("${app.login.bloqueio-minutos:15}") long minutos) {
        this(maximo, Duration.ofMinutes(minutos), Clock.systemUTC());
    }

    TentativasLogin(int maximo, Duration bloqueio, Clock relogio) {
        this.maximo = maximo;
        this.bloqueio = bloqueio;
        this.relogio = relogio;
    }

    public void verificar(String conta) {
        Registro registro = registros.get(chave(conta));
        Instant agora = relogio.instant();
        if (registro != null && registro.bloqueadoAte() != null && agora.isBefore(registro.bloqueadoAte())) {
            throw new MuitasTentativasException(Duration.between(agora, registro.bloqueadoAte()).toSeconds());
        }
    }

    public void falhou(String conta) {
        Instant agora = relogio.instant();
        if (registros.size() > LIMPAR_ACIMA_DE) {
            registros.values().removeIf(r -> r.inicio().plus(bloqueio).isBefore(agora)
                    && (r.bloqueadoAte() == null || r.bloqueadoAte().isBefore(agora)));
        }
        registros.compute(chave(conta), (k, r) -> {
            boolean recomecar = r == null || r.inicio().plus(bloqueio).isBefore(agora)
                    || (r.bloqueadoAte() != null && !agora.isBefore(r.bloqueadoAte()));
            int falhas = recomecar ? 1 : r.falhas() + 1;
            Instant inicio = recomecar ? agora : r.inicio();
            return new Registro(falhas, inicio, falhas >= maximo ? agora.plus(bloqueio) : null);
        });
    }

    public void sucesso(String conta) {
        registros.remove(chave(conta));
    }

    private static String chave(String conta) {
        return conta.trim().toLowerCase(Locale.ROOT);
    }

    private record Registro(int falhas, Instant inicio, Instant bloqueadoAte) {
    }
}
