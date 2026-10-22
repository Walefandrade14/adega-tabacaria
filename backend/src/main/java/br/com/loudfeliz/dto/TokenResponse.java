package br.com.loudfeliz.dto;

import java.time.Instant;

public record TokenResponse(String token, String tipo, Instant expiraEm, String nome, String perfil) {
}
