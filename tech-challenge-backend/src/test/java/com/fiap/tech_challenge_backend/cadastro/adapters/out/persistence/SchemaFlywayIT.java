package com.fiap.tech_challenge_backend.cadastro.adapters.out.persistence;

import com.fiap.tech_challenge_backend.cadastro.domain.enums.StatusCliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Valida o schema real: todas as migrations do Flyway aplicadas em PostgreSQL 16 e
 * as entidades JPA conferidas contra ele (ddl-auto: validate), como em produção.
 * Os demais testes usam o schema gerado pelo Hibernate e não rodam as migrations.
 * Fica no contexto cadastro porque usa as classes de persistência de clientes.
 */
@ActiveProfiles("test-integration-auth")
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Schema do banco - Flyway em PostgreSQL real")
class SchemaFlywayIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurar(DynamicPropertyRegistry registry) throws Exception {
        // O papel é criado pelo repositório infra-db na AWS; aqui ele é criado antes
        // das migrations para exercitar o GRANT da V17.
        try (var conexao = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             var sql = conexao.createStatement()) {
            sql.execute("CREATE ROLE leitura_clientes NOLOGIN");
        }
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ClienteJpaRepository clienteRepository;

    @Test
    @DisplayName("aplica todas as migrations, até a V20, sem falhas")
    void aplicaTodasAsMigrations() {
        Integer falhas = jdbc.queryForObject("SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);
        String ultima = jdbc.queryForObject(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL ORDER BY installed_rank DESC LIMIT 1", String.class);
        assertThat(falhas).isZero();
        assertThat(ultima).isEqualTo("20");
    }

    @Nested
    @DisplayName("V17 - status do cliente")
    class StatusCliente_ {

        @Test
        @DisplayName("cliente novo nasce ATIVO e o valor é lido pela entidade")
        void clienteNovoNasceAtivo() {
            ClienteJpaEntity salvo = clienteRepository.save(ClienteJpaEntity.builder()
                    .nome("Cliente Schema IT")
                    .cpfCnpj("86123456776")
                    .build());

            String status = jdbc.queryForObject("SELECT status FROM clientes WHERE id = ?", String.class, salvo.getId());
            assertThat(status).isEqualTo("ATIVO");
            assertThat(clienteRepository.findById(salvo.getId()).orElseThrow().getStatus()).isEqualTo(StatusCliente.ATIVO);
        }

        @Test
        @DisplayName("CHECK recusa status fora de ATIVO, INATIVO e BLOQUEADO")
        void checkRecusaStatusInvalido() {
            assertThatThrownBy(() -> jdbc.update(
                    "UPDATE clientes SET status = 'SUSPENSO' WHERE id = '88888888-8888-4888-8888-888888888880'"))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_cliente_status");
        }

        @Test
        @DisplayName("leitura_clientes lê só id, cpf_cnpj e status")
        void grantRestritoAsColunasDaAutenticacao() {
            assertThat(privilegioColuna("id")).isTrue();
            assertThat(privilegioColuna("cpf_cnpj")).isTrue();
            assertThat(privilegioColuna("status")).isTrue();
            assertThat(privilegioColuna("nome")).isFalse();
            assertThat(privilegioColuna("telefone")).isFalse();
        }

        private boolean privilegioColuna(String coluna) {
            return Boolean.TRUE.equals(jdbc.queryForObject(
                    "SELECT has_column_privilege('leitura_clientes', 'clientes', ?, 'SELECT')", Boolean.class, coluna));
        }
    }

    @Test
    @DisplayName("V18 - aceita movimentação VENDA e RESERVA e recusa tipo desconhecido")
    void checkTipoMovimentacao() {
        String peca = "55555555-5555-4555-8555-555555555550"; // Óleo de Motor (seed V13)
        for (String tipo : List.of("VENDA", "RESERVA")) {
            jdbc.update("INSERT INTO movimentacoes_estoque (peca_insumo_id, tipo_movimentacao, quantidade) VALUES (?::uuid, ?, 1)", peca, tipo);
        }
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO movimentacoes_estoque (peca_insumo_id, tipo_movimentacao, quantidade) VALUES (?::uuid, 'DOACAO', 1)", peca))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_movimentacao_tipo");
    }

    @Test
    @DisplayName("V19 - cria os índices das consultas e remove os redundantes")
    void indices() {
        List<String> indices = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'", String.class);
        assertThat(indices).contains(
                "idx_ordens_servico_status_urgente_criacao",
                "idx_ordens_servico_cliente_criacao",
                "idx_os_historico_status_os_data",
                "idx_ordens_servico_veiculo_id",
                "idx_ordens_servico_mecanico_id",
                "uk_usuario_email",
                "uk_cliente_cpf_cnpj");
        assertThat(indices).doesNotContain(
                "idx_ordens_servico_cliente_id",
                "idx_os_historico_status_ordem_servico_id",
                "idx_usuario_email",
                "idx_usuario_cpf_cnpj",
                "idx_cliente_cpf_cnpj",
                "idx_veiculo_placa");
    }

    @Test
    @DisplayName("V20 - corrige o CPF inválido do seed no usuário e no cliente")
    void cpfDoSeedCorrigido() {
        UUID usuario = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        UUID cliente = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        assertThat(jdbc.queryForObject("SELECT cpf_cnpj FROM usuarios WHERE id = ?", String.class, usuario)).isEqualTo("10387398708");
        assertThat(jdbc.queryForObject("SELECT cpf_cnpj FROM clientes WHERE id = ?", String.class, cliente)).isEqualTo("10387398708");
    }
}
