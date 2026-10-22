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
import java.math.BigDecimal;

@Entity
@Table(name = "produto")
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;

    @Column(name = "estoque_quantidade", nullable = false)
    private Integer estoqueQuantidade = 0;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id")
    private CategoriaProduto categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fornecedor_id")
    private Fornecedor fornecedor;

    @Column(name = "restrito_maior_idade", nullable = false)
    private boolean restritoMaiorIdade;

    @Column(name = "oferta_especial", nullable = false)
    private boolean ofertaEspecial;

    @Column(name = "limite_diario_oferta")
    private Integer limiteDiarioOferta;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "imagem_url")
    private String imagemUrl;

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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public Integer getEstoqueQuantidade() {
        return estoqueQuantidade;
    }

    public void setEstoqueQuantidade(Integer estoqueQuantidade) {
        this.estoqueQuantidade = estoqueQuantidade;
    }

    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProduto categoria) {
        this.categoria = categoria;
    }

    public Fornecedor getFornecedor() {
        return fornecedor;
    }

    public void setFornecedor(Fornecedor fornecedor) {
        this.fornecedor = fornecedor;
    }

    public boolean isRestritoMaiorIdade() {
        return restritoMaiorIdade;
    }

    public void setRestritoMaiorIdade(boolean restritoMaiorIdade) {
        this.restritoMaiorIdade = restritoMaiorIdade;
    }

    public boolean isOfertaEspecial() {
        return ofertaEspecial;
    }

    public void setOfertaEspecial(boolean ofertaEspecial) {
        this.ofertaEspecial = ofertaEspecial;
    }

    public Integer getLimiteDiarioOferta() {
        return limiteDiarioOferta;
    }

    public void setLimiteDiarioOferta(Integer limiteDiarioOferta) {
        this.limiteDiarioOferta = limiteDiarioOferta;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }
}
