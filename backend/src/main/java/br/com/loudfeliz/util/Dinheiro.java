package br.com.loudfeliz.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Dinheiro {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private Dinheiro() {
    }

    public static String formatar(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(BRASIL).format(valor).replace(' ', ' ');
    }
}
