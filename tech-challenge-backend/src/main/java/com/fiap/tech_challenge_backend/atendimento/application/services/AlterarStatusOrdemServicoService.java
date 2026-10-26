package com.fiap.tech_challenge_backend.atendimento.application.services;

import com.fiap.tech_challenge_backend.acesso.application.ports.out.UsuarioRepositoryPort;
import com.fiap.tech_challenge_backend.acesso.domain.entities.Usuario;
import com.fiap.tech_challenge_backend.atendimento.application.dto.OrdemServicoResponseDTO;
import com.fiap.tech_challenge_backend.atendimento.application.exceptions.OrdemServicoNaoEncontradaException;
import com.fiap.tech_challenge_backend.atendimento.application.ports.in.AlterarStatusOrdemServicoUseCase;
import com.fiap.tech_challenge_backend.atendimento.application.ports.out.OrdemServicoRepositoryPort;
import com.fiap.tech_challenge_backend.atendimento.application.ports.out.OsHistoricoStatusRepositoryPort;
import com.fiap.tech_challenge_backend.atendimento.application.ports.out.TelemetriaAtendimentoPort;
import com.fiap.tech_challenge_backend.atendimento.domain.entities.OrdemServico;
import com.fiap.tech_challenge_backend.atendimento.domain.entities.OsHistoricoStatus;
import com.fiap.tech_challenge_backend.atendimento.domain.enums.StatusOrdemServico;
import com.fiap.tech_challenge_backend.shared.domain.valueobjects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: alterar o status de uma Ordem de Serviço, registrando o histórico
 * e notificando o cliente quando o status realmente muda.
 * Contexto Delimitado: atendimento
 */
@Service
public class AlterarStatusOrdemServicoService implements AlterarStatusOrdemServicoUseCase {

    private static final Logger log = LoggerFactory.getLogger(AlterarStatusOrdemServicoService.class);

    private final OrdemServicoRepositoryPort ordemServicoRepository;
    private final OsHistoricoStatusRepositoryPort osHistoricoStatusRepository;
    private final UsuarioRepositoryPort usuarioRepository;
    private final OrdemServicoNotificacaoService notificacaoService;
    private final OrdemServicoEnriquecimentoService enriquecimentoService;
    private final TelemetriaAtendimentoPort telemetria;

    public AlterarStatusOrdemServicoService(OrdemServicoRepositoryPort ordemServicoRepository,
                                             OsHistoricoStatusRepositoryPort osHistoricoStatusRepository,
                                             UsuarioRepositoryPort usuarioRepository,
                                             OrdemServicoNotificacaoService notificacaoService,
                                             OrdemServicoEnriquecimentoService enriquecimentoService,
                                             TelemetriaAtendimentoPort telemetria) {
        this.ordemServicoRepository = ordemServicoRepository;
        this.osHistoricoStatusRepository = osHistoricoStatusRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacaoService = notificacaoService;
        this.enriquecimentoService = enriquecimentoService;
        this.telemetria = telemetria;
    }

    @Override
    @Transactional
    public OrdemServicoResponseDTO alterarStatus(Long ordemServicoId,
                                                  StatusOrdemServico novoStatus,
                                                  UUID mecanicoId,
                                                  String usuarioEmail) {
        try {
            return alterar(ordemServicoId, novoStatus, mecanicoId, usuarioEmail);
        } catch (RuntimeException e) {
            telemetria.falhaProcessamento("alteracao-status", ordemServicoId, e);
            throw e;
        }
    }

    private OrdemServicoResponseDTO alterar(Long ordemServicoId,
                                            StatusOrdemServico novoStatus,
                                            UUID mecanicoId,
                                            String usuarioEmail) {
        log.info("Alterando status da OS: {} | Novo Status: {} | Mecanico ID: {} | Usuario: {}",
                ordemServicoId, novoStatus, mecanicoId, usuarioEmail);

        OrdemServico os = buscarEntidade(ordemServicoId);

        StatusOrdemServico statusAtual = os.getStatus();
        if (statusAtual == novoStatus && mecanicoId == null) {
            log.debug("Status e mecânico não foram alterados. Retornando OS atual.");
            return enriquecimentoService.montar(os);
        }

        Usuario usuario = usuarioEmail != null
                ? usuarioRepository.procuraPorEmail(new Email(usuarioEmail)).orElse(null)
                : null;

        Usuario novoMecanico = null;
        if (mecanicoId != null) {
            novoMecanico = enriquecimentoService.resolverMecanicoObrigatorio(mecanicoId);
            log.debug("Mecânico localizado: {} | Nome: {}", novoMecanico.getId(), novoMecanico.getNome());
        }

        Duration tempoNoStatusAtual = statusAtual != novoStatus ? tempoNoStatus(os, statusAtual) : null;

        os.alterarStatus(novoStatus, novoMecanico);
        OrdemServico salva = ordemServicoRepository.salvar(os);

        log.info("Status alterado com sucesso | OS: {} | De: {} | Para: {} | Mecanico: {}",
                ordemServicoId, statusAtual, novoStatus,
                novoMecanico != null ? novoMecanico.getId() : "nenhum");

        OsHistoricoStatus historico = OsHistoricoStatus.builder()
                .ordemServico(salva)
                .statusOrigem(statusAtual)
                .statusDestino(novoStatus)
                .usuarioId(usuario != null ? usuario.getId() : null)
                .build();

        osHistoricoStatusRepository.salvar(historico);
        salva.getHistoricoStatus().add(historico);

        if (statusAtual != novoStatus) {
            telemetria.statusAlterado(ordemServicoId, statusAtual, novoStatus, tempoNoStatusAtual);
            notificacaoService.notificarMudancaStatus(salva, statusAtual);
        }

        return enriquecimentoService.montar(salva);
    }

    /**
     * Tempo que a OS está no status atual: desde a última entrada do histórico que a
     * levou a esse status ou, sem histórico, desde a criação. Base do dashboard de
     * tempo médio por status.
     */
    private Duration tempoNoStatus(OrdemServico os, StatusOrdemServico statusAtual) {
        LocalDateTime inicio = osHistoricoStatusRepository.buscarPorOrdensServicoOrdenado(List.of(os.getId())).stream()
                .filter(h -> h.getStatusDestino() == statusAtual && h.getDataMudanca() != null)
                .map(OsHistoricoStatus::getDataMudanca)
                .max(Comparator.naturalOrder())
                .orElse(os.getDataCriacao());
        return inicio != null ? Duration.between(inicio, LocalDateTime.now()) : null;
    }

    private OrdemServico buscarEntidade(Long id) {
        return ordemServicoRepository.buscarPorId(id)
                .orElseThrow(() -> new OrdemServicoNaoEncontradaException(
                        "Ordem de serviço não encontrada: " + id));
    }
}
