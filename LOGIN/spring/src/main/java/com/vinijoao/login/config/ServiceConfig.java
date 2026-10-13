package com.vinijoao.login.config;

import com.vinijoao.login.port.SenhaCodificador;
import com.vinijoao.login.port.TokenEmissor;
import com.vinijoao.login.repository.UsuarioRepository;
import com.vinijoao.login.security.SenhaCodificadorBCrypt;
import com.vinijoao.login.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ServiceConfig {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SenhaCodificador senhaCodificador(BCryptPasswordEncoder encoder) {
        return new SenhaCodificadorBCrypt(encoder);
    }

    @Bean
    public AuthService authService(
            UsuarioRepository usuarioRepository,
            SenhaCodificador senhaCodificador,
            TokenEmissor tokenEmissor,
            @Value("${ADMIN_CODIGO}") String codigoAdmin
    ) {
        return new AuthService(usuarioRepository, senhaCodificador, tokenEmissor, codigoAdmin);
    }
}
