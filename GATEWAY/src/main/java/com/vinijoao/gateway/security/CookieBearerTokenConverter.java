package com.vinijoao.gateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CookieBearerTokenConverter implements ServerAuthenticationConverter {

    @Value("${COOKIE_NOME:tokenAgendaFlow}")
    private String cookieNome;

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        // Primeiro procura no header
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return Mono.just(new BearerTokenAuthenticationToken(authHeader.substring(7)));
        }

        // Se nao tem, procura no cookie
        List<HttpCookie> cookies = exchange.getRequest().getCookies().get(cookieNome);
        if (cookies != null && !cookies.isEmpty()) {
            String token = cookies.get(0).getValue();
            return Mono.just(new BearerTokenAuthenticationToken(token));
        }

        return Mono.empty();
    }
}
