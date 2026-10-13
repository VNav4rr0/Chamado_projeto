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

@Component
public class UsuarioHeadersFilter implements GlobalFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Primeiro remove headers que vieram do cliente para seguranca
        ServerWebExchange requestMutated = exchange.mutate()
                .request(r -> r.headers(h -> {
                    h.remove("X-Usuario-Id");
                    h.remove("X-Usuario-Role");
                }))
                .build();

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {
                    String uid = jwt.getClaimAsString("uid");
                    String role = extrairRole(jwt);
                    
                    ServerWebExchange finalExchange = requestMutated.mutate()
                            .request(r -> r.headers(h -> {
                                if (uid != null) h.add("X-Usuario-Id", uid);
                                if (role != null) h.add("X-Usuario-Role", role);
                            }))
                            .build();
                    return chain.filter(finalExchange);
                })
                .switchIfEmpty(chain.filter(requestMutated));
    }

    private String extrairRole(Jwt jwt) {
        java.util.List<String> roles = jwt.getClaimAsStringList("roles");
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
