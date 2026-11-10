package br.com.loudfeliz.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModoPagamento {

    private boolean simulado;

    public ModoPagamento(@Value("${app.pagamento.simulado:true}") boolean simulado) {
        this.simulado = simulado;
    }

    public boolean simulado() {
        return simulado;
    }
}
