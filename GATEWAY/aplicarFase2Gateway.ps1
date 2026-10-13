# Aplica a Fase 2 no GATEWAY. Rode DENTRO da pasta GATEWAY (a que tem o pom.xml).
if (-not (Test-Path 'pom.xml') -or -not (Test-Path 'src\main')) { Write-Host 'ERRO: rode dentro da pasta GATEWAY.' -ForegroundColor Red; return }
$utf8 = New-Object System.Text.UTF8Encoding($false)
function Salvar($caminho, $texto) {
  $completo = Join-Path (Get-Location) $caminho
  New-Item -ItemType Directory -Force -Path (Split-Path $completo) | Out-Null
  [System.IO.File]::WriteAllText($completo, $texto, $utf8)
  Write-Host "OK  $caminho"
}

Salvar '.env.example' @'
# Copie para .env (ja ignorado pelo git) ou configure no Run Configuration da IDE.
# JWT_SECRET deve ser IDENTICO ao do Login, com no minimo 32 caracteres.
JWT_SECRET=
COOKIE_NOME=tokenSuporteTi

# Opcionais (padrao: localhost nas portas 8090 a 8093)
LOGIN_URL=http://localhost:8090
CHAMADO_URL=http://localhost:8091
ATENDIMENTO_URL=http://localhost:8092
RELATORIO_URL=http://localhost:8093
'@

Salvar 'src\main\java\com\vinijoao\gateway\config\JwtDecoderConfig.java' @'
package com.vinijoao.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtDecoderConfig {

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        // UTF-8 explicito: o Login assina o token com os bytes UTF-8 do segredo.
        return NimbusReactiveJwtDecoder.withSecretKey(
                new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
        ).build();
    }
}
'@

Salvar 'src\main\java\com\vinijoao\gateway\config\SecurityConfig.java' @'
package com.vinijoao.gateway.config;

import com.vinijoao.gateway.security.CookieBearerTokenConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Flux;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String ADMIN = "ROLE_ADMIN";

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            CookieBearerTokenConverter tokenConverter,
            ReactiveJwtAuthenticationConverter jwtAuthenticationConverter) {
        return http
                // Justificativa: cookie SameSite=Lax, corpo JSON e CORS com origens listadas.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(auth -> auth
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/logout").permitAll()
                        .pathMatchers("/api/relatorio/**").hasAuthority(ADMIN)
                        .pathMatchers(HttpMethod.GET, "/api/atendimento/categorias").authenticated()
                        .pathMatchers("/api/atendimento/**").hasAuthority(ADMIN)
                        .pathMatchers("/api/chamados/**").authenticated()
                        .pathMatchers("/api/auth/me", "/api/usuarios/**").authenticated()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenConverter(tokenConverter)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    /** Le a claim "roles" (ex.: ["ROLE_ADMIN"]) sem acrescentar prefixo. */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter conversor = new JwtGrantedAuthoritiesConverter();
        conversor.setAuthoritiesClaimName("roles");
        conversor.setAuthorityPrefix("");

        ReactiveJwtAuthenticationConverter reativo = new ReactiveJwtAuthenticationConverter();
        reativo.setJwtGrantedAuthoritiesConverter(jwt -> Flux.fromIterable(conversor.convert(jwt)));
        return reativo;
    }
}
'@

Salvar 'src\main\java\com\vinijoao\gateway\filter\UsuarioHeadersFilter.java' @'
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
'@

Salvar 'src\main\java\com\vinijoao\gateway\security\CookieBearerTokenConverter.java' @'
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

    @Value("${COOKIE_NOME:tokenSuporteTi}")
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
'@

Salvar 'src\main\resources\application.yaml' @'
server:
  port: 8080

spring:
  application:
    name: api-gateway
  config:
    import: "optional:file:.env[.properties]"
  cloud:
    gateway:
      server:
        webflux:
          httpclient:
            connect-timeout: 5000
            response-timeout: 10s
          routes:
            - id: loginApi
              uri: lb://login-service
              predicates:
                - Path=/api/auth/**, /api/usuarios/**

            - id: chamadoApi
              uri: lb://chamado-service
              predicates:
                - Path=/api/chamados/**

            - id: atendimentoApi
              uri: lb://atendimento-service
              predicates:
                - Path=/api/atendimento/**

            - id: relatorioApi
              uri: lb://relatorio-service
              predicates:
                - Path=/api/relatorio/**
    discovery:
      client:
        simple:
          instances:
            login-service:
              - uri: ${LOGIN_URL:http://localhost:8090}
            chamado-service:
              - uri: ${CHAMADO_URL:http://localhost:8091}
            atendimento-service:
              - uri: ${ATENDIMENTO_URL:http://localhost:8092}
            relatorio-service:
              - uri: ${RELATORIO_URL:http://localhost:8093}

jwt:
  secret: ${JWT_SECRET}

cookie:
  nome: ${COOKIE_NOME:tokenSuporteTi}

cors:
  origens:
    - "http://localhost:8081"
    - "http://localhost:19006"
    - "http://localhost:3000"
    - "http://localhost:5173"
    - "http://127.0.0.1:8081"
    - "http://127.0.0.1:5500"
'@

Salvar 'src\test\java\com\vinijoao\gateway\SecurityConfigTest.java' @'
package com.vinijoao.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@SpringBootTest(properties = {
        "JWT_SECRET=chaveSoParaTesteComMaisDeTrintaEDoisCaracteres",
        "COOKIE_NOME=tokenSuporteTi"
})
class SecurityConfigTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient client;

    @BeforeEach
    void preparar() {
        client = WebTestClient.bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    private WebTestClient comRole(String role) {
        return client.mutateWith(mockJwt().authorities(new SimpleGrantedAuthority(role)));
    }

    /** "Passou do Gateway" = nao foi barrado por 401/403 (o servico de destino nem precisa estar no ar). */
    private static void passouDoGateway(WebTestClient.ResponseSpec resposta) {
        resposta.expectStatus().value(status -> assertThat(status).isNotIn(401, 403));
    }

    @Test
    void semTokenEmChamadosDeveDar401() {
        client.get().uri("/api/chamados").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void semTokenEmRelatorioDeveDar401() {
        client.get().uri("/api/relatorio/mensal").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void usuarioEmRelatorioDeveDar403() {
        comRole("ROLE_USUARIO").get().uri("/api/relatorio/mensal").exchange().expectStatus().isForbidden();
    }

    @Test
    void usuarioNaFilaDoAtendimentoDeveDar403() {
        comRole("ROLE_USUARIO").get().uri("/api/atendimento/fila").exchange().expectStatus().isForbidden();
    }

    @Test
    void usuarioPodeListarCategorias() {
        passouDoGateway(comRole("ROLE_USUARIO").get().uri("/api/atendimento/categorias").exchange());
    }

    @Test
    void usuarioPodeAcessarChamados() {
        passouDoGateway(comRole("ROLE_USUARIO").get().uri("/api/chamados").exchange());
    }

    @Test
    void adminPodeAcessarRelatorio() {
        passouDoGateway(comRole("ROLE_ADMIN").get().uri("/api/relatorio/mensal").exchange());
    }

    @Test
    void adminPodeAcessarFilaDoAtendimento() {
        passouDoGateway(comRole("ROLE_ADMIN").get().uri("/api/atendimento/fila").exchange());
    }
}
'@

Salvar 'src\test\java\com\vinijoao\gateway\filter\UsuarioHeadersFilterTest.java' @'
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
'@

Write-Host 'Fase 2 do Gateway aplicada. Agora rode: C:\maven\bin\mvn.cmd clean test' -ForegroundColor Green