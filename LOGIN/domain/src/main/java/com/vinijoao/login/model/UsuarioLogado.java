package com.vinijoao.login.model;

/** Quem esta fazendo a requisicao (identidade que o Gateway repassa nos headers). */
public record UsuarioLogado(String id, String nome, boolean admin) {
}