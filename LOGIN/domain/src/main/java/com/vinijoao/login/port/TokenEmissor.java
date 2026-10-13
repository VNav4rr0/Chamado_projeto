package com.vinijoao.login.port;

import com.vinijoao.login.model.Token;
import com.vinijoao.login.model.Usuario;

public interface TokenEmissor {
    Token emitir(Usuario usuario);
}