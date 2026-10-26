package com.fiap.tech_challenge_backend.cadastro.domain.enums;

/**
 * Situação cadastral do cliente. Só clientes {@link #ATIVO} recebem token na
 * autenticação por CPF (Lambda auth-cpf).
 * Contexto Delimitado: cadastro
 */
public enum StatusCliente {
    ATIVO,
    INATIVO,
    BLOQUEADO
}
