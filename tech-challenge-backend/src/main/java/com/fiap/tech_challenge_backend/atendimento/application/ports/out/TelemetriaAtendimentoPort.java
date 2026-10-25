package com.fiap.tech_challenge_backend.atendimento.application.ports.out;

import com.fiap.tech_challenge_backend.atendimento.domain.enums.StatusOrdemServico;

import java.time.Duration;

/**
 * Porta de saída para os eventos de negócio do atendimento usados nos dashboards e
 * alertas de observabilidade (volume diário de OS, tempo por status, falhas).
 * Contexto Delimitado: atendimento
 * Camada: Application
 */
public interface TelemetriaAtendimentoPort {

    void ordemServicoCriada(Long ordemServicoId, boolean urgente);

    /**
     * @param tempoNoStatusAnterior quanto tempo a OS ficou em {@code de} antes desta mudança
     */
    void statusAlterado(Long ordemServicoId, StatusOrdemServico de, StatusOrdemServico para,
                        Duration tempoNoStatusAnterior);

    /** Falha ao processar uma OS (abertura ou mudança de status). */
    void falhaProcessamento(String operacao, Long ordemServicoId, Throwable erro);

    /** Falha de uma integração externa (ex.: SMTP), sem interromper a operação. */
    void falhaIntegracao(String integracao, String operacao, Throwable erro);
}
