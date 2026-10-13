package com.vinijoao.login.config;

import com.vinijoao.login.model.enumerable.Role;
import com.vinijoao.login.repository.UsuarioRepository;
import com.vinijoao.login.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DadosIniciais {

    private static final Logger log = LoggerFactory.getLogger(DadosIniciais.class);

    @Bean
    public ApplicationRunner inicializarAdmin(
            UsuarioRepository usuarioRepository,
            AuthService authService,
            @Value("${ADMIN_EMAIL}") String adminEmail,
            @Value("${ADMIN_SENHA}") String adminSenha,
            @Value("${ADMIN_CODIGO}") String codigoAdmin
    ) {
        return args -> {
            String emailNormalizado = adminEmail.trim().toLowerCase();
            if (!usuarioRepository.existsByEmail(emailNormalizado)) {
                authService.cadastrar("Administrador", emailNormalizado, adminSenha, Role.ADMIN, codigoAdmin);
                log.info("Admin inicial criado: {}", emailNormalizado);
            }
        };
    }
}
