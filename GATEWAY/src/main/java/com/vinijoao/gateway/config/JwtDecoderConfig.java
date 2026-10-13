package com.vinijoao.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

import javax.crypto.spec.SecretKeySpec;

@Configuration
public class JwtDecoderConfig {

    @Value("${JWT_SECRET}")
    private String secretKey;

    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withSecretKey(
                new SecretKeySpec(secretKey.getBytes(), "HmacSHA256")
        ).build();
    }
}
