package br.com.loudfeliz.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Convert;
import jakarta.persistence.PrePersist;
import br.com.loudfeliz.dominio.conversor.FormaPagamentoConverter;
import br.com.loudfeliz.dominio.conversor.StatusPagamentoConverter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id")
    private Pedido pedido;

    @Convert(converter = FormaPagamentoConverter.class)
    @Column(nullable = false)
    private FormaPagamento forma;

    @Convert(converter = StatusPagamentoConverter.class)
    @Column(nullable = false)
    private StatusPagamento status = StatusPagamento.PENDENTE;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "transacao_externa_id", length = 100)
    private String transacaoExternaId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;


    @PrePersist
    void aoCriar() {
        if (criadoEm == null) {
            criadoEm = Fuso.agora();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pedido getPedido() {
        return pedido;
    }

    public void setPedido(Pedido pedido) {
        this.pedido = pedido;
    }

    public FormaPagamento getForma() {
        return forma;
    }

    public void setForma(FormaPagamento forma) {
        this.forma = forma;
    }

    public StatusPagamento getStatus() {
        return status;
    }

    public void setStatus(StatusPagamento status) {
        this.status = status;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTransacaoExternaId() {
        return transacaoExternaId;
    }

    public void setTransacaoExternaId(String transacaoExternaId) {
        this.transacaoExternaId = transacaoExternaId;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
