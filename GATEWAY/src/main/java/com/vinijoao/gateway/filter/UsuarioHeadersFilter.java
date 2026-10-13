package com.vinijoao.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class UsuarioHeadersFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Primeiro remove headers que vieram do cliente, para ninguem forjar a identidade
        ServerWebExchange semHeaders = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("X-Usuario-Id");
                    h.remove("X-Usuario-Role");
                    h.remove("X-Usuario-Nome");
                }))
                .build();

        // map + defaultIfEmpty: a cadeia roda UMA vez, com ou sem usuario autenticado
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(auth -> comIdentidade(semHeaders, auth.getToken()))
                .defaultIfEmpty(semHeaders)
                .flatMap(chain::filter);
    }

    private ServerWebExchange comIdentidade(ServerWebExchange troca, Jwt jwt) {
        String uid = jwt.getClaimAsString("uid");
        String nome = jwt.getClaimAsString("nome");
        String role = extrairRole(jwt);

        return troca.mutate()
                .request(r -> r.headers(h -> {
                    if (uid != null) h.add("X-Usuario-Id", uid);
                    if (role != null) h.add("X-Usuario-Role", role);
                    // Header HTTP nao aceita acento: o nome vai codificado em URL (UTF-8)
                    if (nome != null) h.add("X-Usuario-Nome", URLEncoder.encode(nome, StandardCharsets.UTF_8));
                }))
                .build();
    }

    private String extrairRole(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null && !roles.isEmpty()) {
            String primeiro = roles.get(0);
            if (primeiro.startsWith("ROLE_")) {
                return primeiro.substring(5);
            }
            return primeiro;
        }
        return null;
    }
}