package com.vinijoao.gateway;

import com.vinijoao.gateway.security.CookieBearerTokenConverter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
public class SecurityConfigTest {

    @Autowired
    private ApplicationContext context;

    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @MockBean
    private CookieBearerTokenConverter cookieConverter;

    @Test
    void semTokenDeveDar401() {
        WebTestClient client = WebTestClient.bindToApplicationContext(context).build();
        
        client.get().uri("/api/relatorio/mensal")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
