package br.com.loudfeliz.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "combo")
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "desconto_percentual", precision = 5, scale = 2)
    private BigDecimal descontoPercentual;

    @Column(name = "desconto_valor_fixo", precision = 10, scale = 2)
    private BigDecimal descontoValorFixo;

    @Column(nullable = false)
    private boolean ativo = true;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComboItem> itens = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getDescontoPercentual() {
        return descontoPercentual;
    }

    public void setDescontoPercentual(BigDecimal descontoPercentual) {
        this.descontoPercentual = descontoPercentual;
    }

    public BigDecimal getDescontoValorFixo() {
        return descontoValorFixo;
    }

    public void setDescontoValorFixo(BigDecimal descontoValorFixo) {
        this.descontoValorFixo = descontoValorFixo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<ComboItem> getItens() {
        return itens;
    }
}
