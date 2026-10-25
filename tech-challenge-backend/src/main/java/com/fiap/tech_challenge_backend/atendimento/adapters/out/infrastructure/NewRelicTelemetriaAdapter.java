package com.fiap.tech_challenge_backend.atendimento.adapters.out.infrastructure;

import com.fiap.tech_challenge_backend.atendimento.application.ports.out.TelemetriaAtendimentoPort;
import com.fiap.tech_challenge_backend.atendimento.domain.enums.StatusOrdemServico;
import com.newrelic.api.agent.Insights;
import com.newrelic.api.agent.NewRelic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Envia os eventos de negócio como Custom Events do New Relic (RFC-005), consultáveis
 * por NRQL nos dashboards e nas condições de alerta. Sem o agente Java (testes,
 * ambiente local), a API do New Relic é no-op. Cada evento também sai no log.
 * Contexto Delimitado: atendimento
 * Camada: Infrastructure
 */
@Component
public class NewRelicTelemetriaAdapter implements TelemetriaAtendimentoPort {

    static final String EVENTO_OS_CRIADA = "OrdemServicoCriada";
    static final String EVENTO_STATUS_ALTERADO = "OrdemServicoStatusAlterado";
    static final String EVENTO_FALHA_OS = "OrdemServicoFalha";
    static final String EVENTO_FALHA_INTEGRACAO = "IntegracaoFalha";

    private static final Logger log = LoggerFactory.getLogger(NewRelicTelemetriaAdapter.class);

    private final Supplier<Insights> insights;

    public NewRelicTelemetriaAdapter() {
        this(() -> NewRelic.getAgent().getInsights());
    }

    NewRelicTelemetriaAdapter(Supplier<Insights> insights) {
        this.insights = insights;
    }

    @Override
    public void ordemServicoCriada(Long ordemServicoId, boolean urgente) {
        Map<String, Object> atributos = base();
        atributos.put("ordemServicoId", ordemServicoId);
        atributos.put("urgente", urgente);
        registrar(EVENTO_OS_CRIADA, atributos);
    }

    @Override
    public void statusAlterado(Long ordemServicoId, StatusOrdemServico de, StatusOrdemServico para,
                               Duration tempoNoStatusAnterior) {
        Map<String, Object> atributos = base();
        atributos.put("ordemServicoId", ordemServicoId);
        atributos.put("statusAnterior", de != null ? de.name() : "NENHUM");
        atributos.put("statusNovo", para.name());
        if (tempoNoStatusAnterior != null) {
            atributos.put("segundosNoStatusAnterior", tempoNoStatusAnterior.toSeconds());
        }
        registrar(EVENTO_STATUS_ALTERADO, atributos);
    }

    @Override
    public void falhaProcessamento(String operacao, Long ordemServicoId, Throwable erro) {
        Map<String, Object> atributos = base();
        atributos.put("operacao", operacao);
        if (ordemServicoId != null) {
            atributos.put("ordemServicoId", ordemServicoId);
        }
        atributos.put("erro", erro.getClass().getSimpleName());
        atributos.put("mensagem", mensagem(erro));
        registrar(EVENTO_FALHA_OS, atributos);
    }

    @Override
    public void falhaIntegracao(String integracao, String operacao, Throwable erro) {
        Map<String, Object> atributos = base();
        atributos.put("integracao", integracao);
        atributos.put("operacao", operacao);
        atributos.put("erro", erro.getClass().getSimpleName());
        atributos.put("mensagem", mensagem(erro));
        registrar(EVENTO_FALHA_INTEGRACAO, atributos);
    }

    private Map<String, Object> base() {
        Map<String, Object> atributos = new HashMap<>();
        String correlationId = MDC.get("correlationId");
        if (correlationId != null) {
            atributos.put("correlationId", correlationId);
        }
        return atributos;
    }

    private void registrar(String evento, Map<String, Object> atributos) {
        log.info("evento de negócio {} {}", evento, atributos);
        try {
            insights.get().recordCustomEvent(evento, atributos);
        } catch (RuntimeException e) {
            // Observabilidade nunca derruba a operação de negócio.
            log.warn("falha ao registrar evento {} no New Relic: {}", evento, e.getMessage());
        }
    }

    private static String mensagem(Throwable erro) {
        String mensagem = erro.getMessage();
        if (mensagem == null) {
            return "";
        }
        // Limite de tamanho de atributo do New Relic: 4096 caracteres.
        return mensagem.length() > 255 ? mensagem.substring(0, 255) : mensagem;
    }
}
