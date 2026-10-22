package br.com.loudfeliz.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "taxa_entrega_regiao")
public class TaxaEntregaRegiao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80, unique = true)
    private String bairro;

    @Column(name = "cep_prefixo", length = 5)
    private String cepPrefixo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBairro() {
        return bairro;
    }

    public void setBairro(String bairro) {
        this.bairro = bairro;
    }

    public String getCepPrefixo() {
        return cepPrefixo;
    }

    public void setCepPrefixo(String cepPrefixo) {
        this.cepPrefixo = cepPrefixo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
