package com.vinijoao.login.security;

import com.vinijoao.login.port.SenhaCodificador;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SenhaCodificadorBCrypt implements SenhaCodificador {

    private final BCryptPasswordEncoder encoder;

    public SenhaCodificadorBCrypt(BCryptPasswordEncoder encoder) {
        this.encoder = encoder;
    }

    @Override
    public String codificar(String senha) {
        return encoder.encode(senha);
    }

    @Override
    public boolean confere(String senha, String hash) {
        return encoder.matches(senha, hash);
    }
}