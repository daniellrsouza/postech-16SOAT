package com.fiap.tech_challenge_backend.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CorrelationIdFilter")
class CorrelationIdFilterTest {

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    private String executar(MockHttpServletRequest request, MockHttpServletResponse response) throws Exception {
        AtomicReference<String> noMdc = new AtomicReference<>();
        FilterChain chain = (req, res) -> noMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));
        filtro.doFilter(request, response, chain);
        return noMdc.get();
    }

    @Test
    @DisplayName("usa o X-Correlation-Id do API Gateway no MDC e no header da resposta")
    void usaIdRecebido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "FtKnghQLIAMEcfg=");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String noMdc = executar(request, response);

        assertThat(noMdc).isEqualTo("FtKnghQLIAMEcfg=");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("FtKnghQLIAMEcfg=");
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).as("MDC limpo após a requisição").isNull();
    }

    @Test
    @DisplayName("gera um id quando a requisição não traz o header")
    void geraIdQuandoAusente() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String noMdc = executar(new MockHttpServletRequest(), response);

        assertThat(noMdc).matches("[0-9a-f-]{36}");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo(noMdc);
    }

    @Test
    @DisplayName("descarta id com caracteres inválidos (ex.: quebra de linha) e gera outro")
    void descartaIdInvalido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "abc\nlinha-forjada");

        String noMdc = executar(request, new MockHttpServletResponse());

        assertThat(noMdc).doesNotContain("\n").matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("limpa o MDC mesmo quando a requisição falha")
    void limpaMdcEmErro() {
        FilterChain falha = (req, res) -> {
            throw new IllegalStateException("erro na requisição");
        };

        assertThatThrownBy(() -> filtro.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), falha))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }
}
