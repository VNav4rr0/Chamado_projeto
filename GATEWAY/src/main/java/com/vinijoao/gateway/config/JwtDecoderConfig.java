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