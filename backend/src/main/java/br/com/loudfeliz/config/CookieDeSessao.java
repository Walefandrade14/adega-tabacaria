package br.com.loudfeliz.config;

import br.com.loudfeliz.dto.TokenResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class CookieDeSessao {

    public static final String NOME = "loudfeliz_token";
    private static final String CAMINHO = "/api";

    public void gravar(HttpServletRequest requisicao, HttpServletResponse resposta, TokenResponse token) {
        Duration validade = Duration.between(Instant.now(), token.expiraEm());
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookie(requisicao, token.token(), validade.isNegative() ? Duration.ZERO : validade).toString());
    }

    public void apagar(HttpServletRequest requisicao, HttpServletResponse resposta) {
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookie(requisicao, "", Duration.ZERO).toString());
    }

    public String ler(HttpServletRequest requisicao) {
        Cookie[] cookies = requisicao.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (NOME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static ResponseCookie cookie(HttpServletRequest requisicao, String valor, Duration validade) {
        return ResponseCookie.from(NOME, valor)
                .httpOnly(true)
                .secure(requisicao.isSecure())
                .sameSite("Lax")
                .path(CAMINHO)
                .maxAge(validade)
                .build();
    }
}
