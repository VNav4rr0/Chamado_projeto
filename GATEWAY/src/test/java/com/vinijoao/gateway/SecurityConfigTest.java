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