package com.suporteti.atendimento.controller.resolver;

import com.suporteti.atendimento.model.UsuarioLogado;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UsuarioLogado.class.equals(parameter.getParameterType());
    }

    @Override
    public UsuarioLogado resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        String id = webRequest.getHeader("X-Usuario-Id");
        String role = webRequest.getHeader("X-Usuario-Role");
        String nomeEncoded = webRequest.getHeader("X-Usuario-Nome");

        String nome = (nomeEncoded != null)
                ? URLDecoder.decode(nomeEncoded, StandardCharsets.UTF_8)
                : null;

        boolean admin = "ROLE_ADMIN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role);

        return new UsuarioLogado(id, nome, admin);
    }
}
