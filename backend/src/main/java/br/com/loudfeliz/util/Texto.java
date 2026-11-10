package br.com.loudfeliz.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Texto {

    private Texto() {
    }

    public static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }
}
