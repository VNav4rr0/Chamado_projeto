package com.vinijoao.login.controller.resolver;

import com.vinijoao.login.model.UsuarioLogado;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * Monta o UsuarioLogado a partir dos headers que o Gateway escreve (X-Usuario-*).
 * Fica num so lugar: os controllers apenas declaram o parametro.
 */
public class UsuarioLogadoResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return UsuarioLogado.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        String id = webRequest.getHeader("X-Usuario-Id");
        String role = webRequest.getHeader("X-Usuario-Role");
        String nomeCodificado = webRequest.getHeader("X-Usuario-Nome");
        // O nome vem codificado em URL (header HTTP nao aceita acento)
        String nome = nomeCodificado == null ? null : URLDecoder.decode(nomeCodificado, StandardCharsets.UTF_8);
        return new UsuarioLogado(id, nome, "ADMIN".equals(role));
    }
}