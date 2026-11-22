package br.com.loudfeliz.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "tentativa_login")
public class TentativaLogin {

    @Id
    @Column(length = 200)
    private String conta;

    @Column(nullable = false)
    private int falhas;

    @Column(nullable = false)
    private Instant inicio;

    @Column(name = "bloqueado_ate")
    private Instant bloqueadoAte;

    public String getConta() {
        return conta;
    }

    public void setConta(String conta) {
        this.conta = conta;
    }

    public int getFalhas() {
        return falhas;
    }

    public void setFalhas(int falhas) {
        this.falhas = falhas;
    }

    public Instant getInicio() {
        return inicio;
    }

    public void setInicio(Instant inicio) {
        this.inicio = inicio;
    }

    public Instant getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(Instant bloqueadoAte) {
        this.bloqueadoAte = bloqueadoAte;
    }
}
