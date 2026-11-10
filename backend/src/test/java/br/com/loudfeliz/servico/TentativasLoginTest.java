package br.com.loudfeliz.servico;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.loudfeliz.excecao.MuitasTentativasException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TentativasLoginTest {

    private Instant agora = Instant.parse("2026-11-05T12:00:00Z");

    private final Clock relogio = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    };

    private final TentativasLogin tentativas = new TentativasLogin(5, Duration.ofMinutes(15), relogio);

    @Test
    void bloqueiaNaQuintaFalhaELiberaDepoisDoPrazo() {
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("cliente:ana@teste.com");
        }
        assertDoesNotThrow(() -> tentativas.verificar("cliente:ana@teste.com"));
        tentativas.falhou("cliente:ANA@teste.com ");
        assertThrows(MuitasTentativasException.class, () -> tentativas.verificar("cliente:ana@teste.com"));
        assertDoesNotThrow(() -> tentativas.verificar("cliente:outra@teste.com"));

        agora = agora.plus(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> tentativas.verificar("cliente:ana@teste.com"));
        tentativas.falhou("cliente:ana@teste.com");
        assertDoesNotThrow(() -> tentativas.verificar("cliente:ana@teste.com"));
    }

    @Test
    void acertarASenhaZeraAsFalhas() {
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("funcionario:dono");
        }
        tentativas.sucesso("funcionario:dono");
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("funcionario:dono");
        }
        assertDoesNotThrow(() -> tentativas.verificar("funcionario:dono"));
    }

    @Test
    void falhasEspalhadasNaoSomamDepoisDaJanela() {
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("cliente:bia@teste.com");
        }
        agora = agora.plus(Duration.ofMinutes(16));
        tentativas.falhou("cliente:bia@teste.com");
        assertDoesNotThrow(() -> tentativas.verificar("cliente:bia@teste.com"));
    }
}
