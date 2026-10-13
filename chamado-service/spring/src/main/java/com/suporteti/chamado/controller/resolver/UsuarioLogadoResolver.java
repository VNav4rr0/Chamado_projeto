package com.suporteti.chamado.controller.resolver;

import com.suporteti.chamado.model.UsuarioLogado;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UsuarioLogado.class);
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        String id = webRequest.getHeader("X-Usuario-Id");
        String role = webRequest.getHeader("X-Usuario-Role");
        String nomeCodificado = webRequest.getHeader("X-Usuario-Nome");

        if (id == null || id.isBlank()) {
            return null;
        }

        String nome = id;
        if (nomeCodificado != null && !nomeCodificado.isBlank()) {
            try {
                nome = URLDecoder.decode(nomeCodificado, StandardCharsets.UTF_8);
            } catch (Exception e) {
                nome = nomeCodificado;
            }
        }

        boolean admin = role != null && role.toUpperCase().contains("ADMIN");
        return new UsuarioLogado(id, nome, admin);
    }
}
