package br.com.loudfeliz.web;

import org.springframework.security.oauth2.jwt.Jwt;

final class Usuario {

    private Usuario() {
    }

    static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
