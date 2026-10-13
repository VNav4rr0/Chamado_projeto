package com.suporteti.chamado.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "contadores")
public class ContadorOrm {

    @Id
    private String id;
    private long valor;

    public ContadorOrm() {}

    public ContadorOrm(String id, long valor) {
        this.id = id;
        this.valor = valor;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getValor() {
        return valor;
    }

    public void setValor(long valor) {
        this.valor = valor;
    }
}
