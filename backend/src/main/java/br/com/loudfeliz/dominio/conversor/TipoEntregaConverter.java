package br.com.loudfeliz.dominio.conversor;

import br.com.loudfeliz.dominio.TipoEntrega;
import jakarta.persistence.Converter;

@Converter
public class TipoEntregaConverter extends EnumMinusculoConverter<TipoEntrega> {

    public TipoEntregaConverter() {
        super(TipoEntrega.class);
    }
}
