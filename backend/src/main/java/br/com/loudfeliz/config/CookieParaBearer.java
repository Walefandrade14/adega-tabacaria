package br.com.loudfeliz.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

public class CookieParaBearer extends OncePerRequestFilter {

    public static final String ATRIBUTO = CookieParaBearer.class.getName() + ".USOU_COOKIE";

    private final CookieDeSessao cookies;

    public CookieParaBearer(CookieDeSessao cookies) {
        this.cookies = cookies;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        boolean autenticacao = requisicao.getRequestURI().startsWith(requisicao.getContextPath() + "/api/auth/");
        String token = autenticacao || requisicao.getHeader(HttpHeaders.AUTHORIZATION) != null ? null : cookies.ler(requisicao);
        if (token == null) {
            cadeia.doFilter(requisicao, resposta);
            return;
        }
        requisicao.setAttribute(ATRIBUTO, Boolean.TRUE);
        cadeia.doFilter(new ComBearer(requisicao, "Bearer " + token), resposta);
    }

    private static final class ComBearer extends HttpServletRequestWrapper {

        private final String valor;

        ComBearer(HttpServletRequest requisicao, String valor) {
            super(requisicao);
            this.valor = valor;
        }

        @Override
        public String getHeader(String nome) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(nome) ? valor : super.getHeader(nome);
        }

        @Override
        public Enumeration<String> getHeaders(String nome) {
            return HttpHeaders.AUTHORIZATION.equalsIgnoreCase(nome) ? Collections.enumeration(List.of(valor)) : super.getHeaders(nome);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> nomes = new ArrayList<>(Collections.list(super.getHeaderNames()));
            nomes.add(HttpHeaders.AUTHORIZATION);
            return Collections.enumeration(nomes);
        }
    }
}
