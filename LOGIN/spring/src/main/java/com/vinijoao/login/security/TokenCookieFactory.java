package com.vinijoao.login.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenCookieFactory {

    private final String cookieNome;
    private final long expiracaoMinutos;

    public TokenCookieFactory(
            @Value("${COOKIE_NOME:tokenAgendaFlow}") String cookieNome,
            @Value("${JWT_EXPIRACAO_MINUTOS:60}") long expiracaoMinutos
    ) {
        this.cookieNome = cookieNome;
        this.expiracaoMinutos = expiracaoMinutos;
    }

    public String cookieHeader(String tokenValue) {
        return cookieNome + "=" + tokenValue + "; HttpOnly; Path=/; SameSite=Lax; Max-Age=" + (expiracaoMinutos * 60);
    }

    public String cookieHeaderLogout() {
        return cookieNome + "=; HttpOnly; Path=/; SameSite=Lax; Max-Age=0";
    }
}
