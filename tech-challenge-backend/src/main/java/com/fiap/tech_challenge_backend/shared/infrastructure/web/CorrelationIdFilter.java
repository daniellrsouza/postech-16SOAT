package com.fiap.tech_challenge_backend.shared.infrastructure.web;

import com.newrelic.api.agent.NewRelic;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Correlação entre requisições (RFC-005): usa o X-Correlation-Id recebido do API
 * Gateway (= $context.requestId, o mesmo do access log e dos logs das Lambdas) ou gera
 * um novo. O id vai para o MDC (aparece em todo log JSON da requisição), para o
 * header da resposta e para a transação do New Relic.
 * Contexto: shared (Infraestrutura compartilhada)
 * Camada: Infrastructure
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    // Aceita só ids curtos e sem caracteres de controle (evita injeção em logs/headers).
    private static final Pattern ID_VALIDO = Pattern.compile("[A-Za-z0-9=_.:+/-]{1,128}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String recebido = request.getHeader(HEADER);
        String correlationId = recebido != null && ID_VALIDO.matcher(recebido).matches()
                ? recebido
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        NewRelic.addCustomParameter(MDC_KEY, correlationId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
