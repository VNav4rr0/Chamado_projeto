package com.vinijoao.login.security;

import com.vinijoao.login.model.Token;
import com.vinijoao.login.model.Usuario;
import com.vinijoao.login.port.TokenEmissor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.nio.charset.StandardCharsets;

@Component
public class JwtTokenEmissor implements TokenEmissor {

    private final SecretKey chave;
    private final long expiracaoMinutos;

    public JwtTokenEmissor(@Value("${JWT_SECRET}") String secret,
                           @Value("${JWT_EXPIRACAO_MINUTOS:60}") long expiracaoMinutos) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET precisa ter no minimo 32 caracteres.");
        }
        this.chave = Keys.hmacShaKeyFor(bytes);
        this.expiracaoMinutos = expiracaoMinutos;
    }

    @Override
    public Token emitir(Usuario usuario) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMinutos * 60 * 1000L);
        String jwt = Jwts.builder()
                .subject(usuario.email())
                .claim("uid", usuario.id())
                .claim("nome", usuario.nome())
                .claim("roles", List.of("ROLE_" + usuario.role().name()))
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave, Jwts.SIG.HS256)
                .compact();
        return new Token(jwt);
    }
}
