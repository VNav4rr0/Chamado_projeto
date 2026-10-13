package com.vinijoao.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UsuarioHeadersFilterTest {

    private final UsuarioHeadersFilter filtro = new UsuarioHeadersFilter();
    private final AtomicInteger chamadasDaCadeia = new AtomicInteger();

    private HttpHeaders executar(MockServerHttpRequest requisicao, JwtAuthenticationToken autenticacao) {
        MockServerWebExchange troca = MockServerWebExchange.from(requisicao);
        AtomicReference<ServerWebExchange> capturada = new AtomicReference<>();
        GatewayFilterChain cadeia = ex -> {
            chamadasDaCadeia.incrementAndGet();
            capturada.set(ex);
            return Mono.empty();
        };

        Mono<Void> resultado = filtro.filter(troca, cadeia);
        if (autenticacao != null) {
            resultado = resultado.contextWrite(ReactiveSecurityContextHolder.withAuthentication(autenticacao));
        }
        resultado.block();
        return capturada.get().getRequest().getHeaders();
    }

    private static JwtAuthenticationToken tokenDe(String uid, String nome, String role) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "HS256")
                .claim("uid", uid)
                .claim("nome", nome)
                .claim("roles", List.of(role))
                .build();
        return new JwtAuthenticationToken(jwt);
    }

    @Test
    void headersForjadosPeloClienteSaoDescartados() {
        MockServerHttpRequest requisicao = MockServerHttpRequest.get("/api/chamados")
                .header("X-Usuario-Id", "forjado")
                .header("X-Usuario-Role", "ADMIN")
                .header("X-Usuario-Nome", "Hacker")
                .build();

        HttpHeaders headers = executar(requisicao, null);

        assertThat(headers.containsKey("X-Usuario-Id")).isFalse();
        assertThat(headers.containsKey("X-Usuario-Role")).isFalse();
        assertThat(headers.containsKey("X-Usuario-Nome")).isFalse();
        assertThat(chamadasDaCadeia.get()).isEqualTo(1);
    }

    @Test
    void usuarioAutenticadoRecebeIdentidadeDoTokenEnaoDoCliente() {
        MockServerHttpRequest requisicao = MockServerHttpRequest.get("/api/chamados")
                .header("X-Usuario-Id", "forjado")
                .header("X-Usuario-Role", "ADMIN")
                .build();

        HttpHeaders headers = executar(requisicao, tokenDe("u123", "João Silva", "ROLE_USUARIO"));

        assertThat(headers.get("X-Usuario-Id")).containsExactly("u123");
        assertThat(headers.get("X-Usuario-Role")).containsExactly("USUARIO");
        assertThat(headers.get("X-Usuario-Nome")).containsExactly("Jo%C3%A3o+Silva");
        // A cadeia precisa rodar uma unica vez, senao a requisicao chegaria duplicada ao servico
        assertThat(chamadasDaCadeia.get()).isEqualTo(1);
    }
}