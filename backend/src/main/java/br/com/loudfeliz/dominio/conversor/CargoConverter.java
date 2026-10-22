package br.com.loudfeliz.dominio.conversor;

import br.com.loudfeliz.dominio.Cargo;
import jakarta.persistence.Converter;

@Converter
public class CargoConverter extends EnumMinusculoConverter<Cargo> {

    public CargoConverter() {
        super(Cargo.class);
    }
}
