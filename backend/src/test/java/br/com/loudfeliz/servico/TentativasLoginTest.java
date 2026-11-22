package br.com.loudfeliz.servico;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.loudfeliz.excecao.MuitasTentativasException;
import br.com.loudfeliz.repositorio.TentativaLoginRepositorio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TentativasLoginTest {

    @Autowired
    private TentativaLoginRepositorio repositorio;

    private TentativasLogin tentativas;
    private String sufixo;

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

    @BeforeEach
    void preparar() {
        tentativas = new TentativasLogin(5, Duration.ofMinutes(15), relogio, repositorio);
        sufixo = UUID.randomUUID().toString().substring(0, 8);
    }

    @Test
    void oBloqueioFicaGuardadoNoBanco() {
        String conta = "cliente:persistente-" + sufixo + "@teste.com";
        for (int i = 0; i < 5; i++) {
            tentativas.falhou(conta);
        }
        TentativasLogin depoisDeReiniciar = new TentativasLogin(5, Duration.ofMinutes(15), relogio, repositorio);
        assertThrows(MuitasTentativasException.class, () -> depoisDeReiniciar.verificar(conta));
        depoisDeReiniciar.sucesso(conta);
        assertDoesNotThrow(() -> tentativas.verificar(conta));
    }

    @Test
    void bloqueiaNaQuintaFalhaELiberaDepoisDoPrazo() {
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("cliente:ana-" + sufixo + "@teste.com");
        }
        assertDoesNotThrow(() -> tentativas.verificar("cliente:ana-" + sufixo + "@teste.com"));
        tentativas.falhou("cliente:ANA-" + sufixo + "@teste.com ");
        assertThrows(MuitasTentativasException.class, () -> tentativas.verificar("cliente:ana-" + sufixo + "@teste.com"));
        assertDoesNotThrow(() -> tentativas.verificar("cliente:outra-" + sufixo + "@teste.com"));

        agora = agora.plus(Duration.ofMinutes(15));
        assertDoesNotThrow(() -> tentativas.verificar("cliente:ana-" + sufixo + "@teste.com"));
        tentativas.falhou("cliente:ana-" + sufixo + "@teste.com");
        assertDoesNotThrow(() -> tentativas.verificar("cliente:ana-" + sufixo + "@teste.com"));
    }

    @Test
    void acertarASenhaZeraAsFalhas() {
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("funcionario:dono-" + sufixo);
        }
        tentativas.sucesso("funcionario:dono-" + sufixo);
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("funcionario:dono-" + sufixo);
        }
        assertDoesNotThrow(() -> tentativas.verificar("funcionario:dono-" + sufixo));
    }

    @Test
    void falhasEspalhadasNaoSomamDepoisDaJanela() {
        for (int i = 0; i < 4; i++) {
            tentativas.falhou("cliente:bia-" + sufixo + "@teste.com");
        }
        agora = agora.plus(Duration.ofMinutes(16));
        tentativas.falhou("cliente:bia-" + sufixo + "@teste.com");
        assertDoesNotThrow(() -> tentativas.verificar("cliente:bia-" + sufixo + "@teste.com"));
    }
}
