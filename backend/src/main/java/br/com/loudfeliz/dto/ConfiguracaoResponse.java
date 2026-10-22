package br.com.loudfeliz.dto;

import br.com.loudfeliz.dominio.Configuracao;

public record ConfiguracaoResponse(String chave, String valor) {

    public static ConfiguracaoResponse de(Configuracao c) {
        return new ConfiguracaoResponse(c.getChave(), c.getValor());
    }
}
