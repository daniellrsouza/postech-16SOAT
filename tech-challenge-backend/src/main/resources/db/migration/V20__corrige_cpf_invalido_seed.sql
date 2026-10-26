-- V20__corrige_cpf_invalido_seed.sql
-- Fase 3 · ajuste G do modelo de dados (consistência dos dados).
--
-- O seed da V13 cadastrou a cliente de demonstração com o CPF 10387398790, cujos
-- dígitos verificadores estão errados (os corretos são 0 e 8). A aplicação só valida o
-- formato e aceitava o valor; a Lambda de autenticação valida os dígitos e o
-- recusava. Corrige os dígitos verificadores (10387398708) no usuário e no
-- cliente. Não altera migrations já aplicadas; afeta só bancos que têm o seed.
-- Contexto: acesso, cadastro

UPDATE usuarios SET cpf_cnpj = '10387398708'
WHERE id = '550e8400-e29b-41d4-a716-446655440001' AND cpf_cnpj = '10387398790';

UPDATE clientes SET cpf_cnpj = '10387398708'
WHERE id = '550e8400-e29b-41d4-a716-446655440002' AND cpf_cnpj = '10387398790';
