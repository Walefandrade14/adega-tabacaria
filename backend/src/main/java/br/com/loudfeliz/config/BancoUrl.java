package br.com.loudfeliz.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class BancoUrl {

    private BancoUrl() {
    }

    public static void aplicar(String databaseUrl, String nomeBanco) {
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }
        URI uri = URI.create(databaseUrl.trim());
        String banco = nomeBanco == null || nomeBanco.isBlank() ? "adega_tabacaria" : nomeBanco.trim();
        int porta = uri.getPort() > 0 ? uri.getPort() : 3306;
        String consulta = uri.getRawQuery() == null ? "" : uri.getRawQuery().toLowerCase(Locale.ROOT);
        boolean exigeSsl = consulta.contains("ssl-mode=required") || consulta.contains("sslmode=required") || consulta.contains("sslmode=require");
        String parametros = exigeSsl
                ? "sslMode=REQUIRED&createDatabaseIfNotExist=true"
                : "createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true";
        System.setProperty("spring.datasource.url", "jdbc:mysql://" + uri.getHost() + ":" + porta + "/" + banco + "?" + parametros);
        String credenciais = uri.getRawUserInfo();
        if (credenciais != null) {
            String[] partes = credenciais.split(":", 2);
            System.setProperty("spring.datasource.username", decodificar(partes[0]));
            if (partes.length > 1) {
                System.setProperty("spring.datasource.password", decodificar(partes[1]));
            }
        }
    }

    private static String decodificar(String valor) {
        return URLDecoder.decode(valor, StandardCharsets.UTF_8);
    }
}
