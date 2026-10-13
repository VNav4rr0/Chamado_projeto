package com.vinijoao.login.port;

public interface SenhaCodificador {
    String codificar(String senha);
    boolean confere(String senha, String hash);
}