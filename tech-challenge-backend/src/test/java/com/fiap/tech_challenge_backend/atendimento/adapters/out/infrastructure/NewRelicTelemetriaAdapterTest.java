package com.fiap.tech_challenge_backend.atendimento.adapters.out.infrastructure;

import com.fiap.tech_challenge_backend.atendimento.domain.enums.StatusOrdemServico;
import com.newrelic.api.agent.Insights;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("NewRelicTelemetriaAdapter")
class NewRelicTelemetriaAdapterTest {

    private final Insights insights = mock(Insights.class);
    private final NewRelicTelemetriaAdapter adapter = new NewRelicTelemetriaAdapter(() -> insights);

    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> atributosDe(String evento) {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(insights).recordCustomEvent(eq(evento), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("OS criada vira evento OrdemServicoCriada com o correlationId da requisição")
    void ordemServicoCriada() {
        MDC.put("correlationId", "req-123");

        adapter.ordemServicoCriada(20260010L, true);

        assertThat(atributosDe("OrdemServicoCriada"))
                .containsEntry("ordemServicoId", 20260010L)
                .containsEntry("urgente", true)
                .containsEntry("correlationId", "req-123");
    }

    @Test
    @DisplayName("mudança de status leva o tempo no status anterior em segundos")
    void statusAlterado() {
        adapter.statusAlterado(1L, StatusOrdemServico.EM_DIAGNOSTICO, StatusOrdemServico.AGUARDANDO_APROVACAO,
                Duration.ofMinutes(90));

        assertThat(atributosDe("OrdemServicoStatusAlterado"))
                .containsEntry("statusAnterior", "EM_DIAGNOSTICO")
                .containsEntry("statusNovo", "AGUARDANDO_APROVACAO")
                .containsEntry("segundosNoStatusAnterior", 5400L)
                .doesNotContainKey("correlationId");
    }

    @Test
    @DisplayName("sem duração conhecida, o evento sai sem o atributo de tempo")
    void statusAlteradoSemDuracao() {
        adapter.statusAlterado(1L, null, StatusOrdemServico.RECEBIDA, null);

        assertThat(atributosDe("OrdemServicoStatusAlterado"))
                .containsEntry("statusAnterior", "NENHUM")
                .doesNotContainKey("segundosNoStatusAnterior");
    }

    @Test
    @DisplayName("falha no processamento de OS registra operação, erro e mensagem truncada")
    void falhaProcessamento() {
        adapter.falhaProcessamento("abertura", null, new IllegalArgumentException("x".repeat(400)));

        Map<String, Object> atributos = atributosDe("OrdemServicoFalha");
        assertThat(atributos)
                .containsEntry("operacao", "abertura")
                .containsEntry("erro", "IllegalArgumentException")
                .doesNotContainKey("ordemServicoId");
        assertThat((String) atributos.get("mensagem")).hasSize(255);
    }

    @Test
    @DisplayName("falha de integração registra a integração e o erro")
    void falhaIntegracao() {
        adapter.falhaIntegracao("smtp", "envio-email", new RuntimeException());

        assertThat(atributosDe("IntegracaoFalha"))
                .containsEntry("integracao", "smtp")
                .containsEntry("operacao", "envio-email")
                .containsEntry("erro", "RuntimeException")
                .containsEntry("mensagem", "");
    }

    @Test
    @DisplayName("erro do New Relic nunca interrompe a operação de negócio")
    void erroDoAgenteNaoPropaga() {
        doThrow(new IllegalStateException("agente indisponível")).when(insights).recordCustomEvent(any(), anyMap());

        assertThatCode(() -> adapter.ordemServicoCriada(1L, false)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("sem o agente Java, o construtor padrão usa a API no-op do New Relic")
    void construtorPadraoSemAgente() {
        assertThatCode(() -> new NewRelicTelemetriaAdapter().ordemServicoCriada(1L, false)).doesNotThrowAnyException();
    }
}
