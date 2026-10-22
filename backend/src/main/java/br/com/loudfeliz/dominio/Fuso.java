package br.com.loudfeliz.dominio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class Fuso {

    public static final ZoneId PADRAO = ZoneId.of("America/Sao_Paulo");

    private Fuso() {
    }

    public static LocalDateTime agora() {
        return LocalDateTime.now(PADRAO);
    }

    public static LocalDate hoje() {
        return LocalDate.now(PADRAO);
    }
}
