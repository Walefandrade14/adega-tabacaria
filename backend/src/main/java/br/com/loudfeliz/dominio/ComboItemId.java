package br.com.loudfeliz.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ComboItemId implements Serializable {

    @Column(name = "combo_id")
    private Long comboId;

    @Column(name = "produto_id")
    private Long produtoId;

    public ComboItemId() {
    }

    public ComboItemId(Long comboId, Long produtoId) {
        this.comboId = comboId;
        this.produtoId = produtoId;
    }

    public Long getComboId() {
        return comboId;
    }

    public Long getProdutoId() {
        return produtoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ComboItemId outro)) {
            return false;
        }
        return Objects.equals(comboId, outro.comboId) && Objects.equals(produtoId, outro.produtoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comboId, produtoId);
    }
}
