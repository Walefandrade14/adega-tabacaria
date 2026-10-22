package br.com.loudfeliz.dominio.conversor;

import jakarta.persistence.AttributeConverter;
import java.util.Locale;

public abstract class EnumMinusculoConverter<E extends Enum<E>> implements AttributeConverter<E, String> {

    private final Class<E> tipo;

    protected EnumMinusculoConverter(Class<E> tipo) {
        this.tipo = tipo;
    }

    @Override
    public String convertToDatabaseColumn(E valor) {
        return valor == null ? null : valor.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public E convertToEntityAttribute(String valor) {
        return valor == null ? null : Enum.valueOf(tipo, valor.toUpperCase(Locale.ROOT));
    }
}
