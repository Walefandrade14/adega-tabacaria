ALTER TABLE cliente ADD COLUMN senha_alterada_em DATETIME NULL;

ALTER TABLE endereco ADD COLUMN arquivado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_pedido_status_criado ON pedido (status, criado_em);
CREATE INDEX idx_pedido_criado ON pedido (criado_em);

INSERT INTO configuracao (chave, valor) VALUES
    ('minutos_expiracao_pedido', '30'),
    ('estoque_baixo', '5');
