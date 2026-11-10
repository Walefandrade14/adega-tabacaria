CREATE TABLE cliente (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome              VARCHAR(120) NOT NULL,
    cpf               CHAR(11) NOT NULL UNIQUE,
    data_nascimento   DATE NOT NULL,
    email             VARCHAR(160) NOT NULL UNIQUE,
    senha_hash        VARCHAR(255) NOT NULL,
    telefone          VARCHAR(20),
    criado_em         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE endereco (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id        BIGINT NOT NULL,
    apelido           VARCHAR(40),
    logradouro        VARCHAR(160) NOT NULL,
    numero            VARCHAR(10) NOT NULL,
    complemento       VARCHAR(80),
    bairro            VARCHAR(80) NOT NULL,
    cidade            VARCHAR(80) NOT NULL,
    uf                CHAR(2) NOT NULL,
    cep               CHAR(8) NOT NULL,
    principal         BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE funcionario (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome              VARCHAR(120) NOT NULL,
    cpf               CHAR(11) NOT NULL UNIQUE,
    cargo             ENUM('dono','caixa','entregador','estoque') NOT NULL,
    usuario           VARCHAR(60) NOT NULL UNIQUE,
    senha_hash        VARCHAR(255) NOT NULL,
    ativo             BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE fornecedor (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    razao_social      VARCHAR(160) NOT NULL,
    cnpj              CHAR(14) NOT NULL UNIQUE,
    telefone          VARCHAR(20),
    email             VARCHAR(160)
) ENGINE=InnoDB;

CREATE TABLE categoria_produto (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome              VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE produto (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome                  VARCHAR(160) NOT NULL,
    descricao             TEXT,
    preco                 DECIMAL(10,2) NOT NULL,
    estoque_quantidade    INT NOT NULL DEFAULT 0,
    categoria_id          BIGINT NOT NULL,
    fornecedor_id         BIGINT,
    restrito_maior_idade  BOOLEAN NOT NULL DEFAULT FALSE,
    oferta_especial       BOOLEAN NOT NULL DEFAULT FALSE,
    limite_diario_oferta  INT,
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (categoria_id) REFERENCES categoria_produto(id),
    FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
    CHECK (estoque_quantidade >= 0)
) ENGINE=InnoDB;

CREATE TABLE combo (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome                  VARCHAR(120) NOT NULL,
    desconto_percentual   DECIMAL(5,2),
    desconto_valor_fixo   DECIMAL(10,2),
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (desconto_percentual IS NOT NULL OR desconto_valor_fixo IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE combo_item (
    combo_id              BIGINT NOT NULL,
    produto_id            BIGINT NOT NULL,
    quantidade            INT NOT NULL DEFAULT 1,
    PRIMARY KEY (combo_id, produto_id),
    FOREIGN KEY (combo_id) REFERENCES combo(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produto(id)
) ENGINE=InnoDB;

CREATE TABLE horario_funcionamento (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    dia_semana        TINYINT NOT NULL,
    hora_abertura     TIME NOT NULL,
    hora_fechamento   TIME NOT NULL,
    aceita_delivery   BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (dia_semana)
) ENGINE=InnoDB;

CREATE TABLE taxa_entrega_regiao (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    bairro            VARCHAR(80) NOT NULL,
    cep_prefixo       CHAR(5),
    valor             DECIMAL(10,2) NOT NULL,
    UNIQUE (bairro)
) ENGINE=InnoDB;

CREATE TABLE configuracao (
    chave             VARCHAR(60) PRIMARY KEY,
    valor             VARCHAR(255) NOT NULL
) ENGINE=InnoDB;
INSERT INTO configuracao (chave, valor) VALUES
    ('valor_minimo_delivery', '35.00'),
    ('limite_padrao_oferta_dia', '3');

CREATE TABLE pedido (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id            BIGINT,
    funcionario_id        BIGINT,
    tipo_entrega          ENUM('delivery','retirada','balcao') NOT NULL,
    endereco_entrega_id   BIGINT,
    status                ENUM('aberto','aguardando_pagamento','pago',
                                'em_separacao','em_entrega','concluido','cancelado')
                          NOT NULL DEFAULT 'aberto',
    subtotal              DECIMAL(10,2) NOT NULL DEFAULT 0,
    taxa_entrega          DECIMAL(10,2) NOT NULL DEFAULT 0,
    desconto_total        DECIMAL(10,2) NOT NULL DEFAULT 0,
    total                 DECIMAL(10,2) NOT NULL DEFAULT 0,
    criado_em             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    FOREIGN KEY (funcionario_id) REFERENCES funcionario(id),
    FOREIGN KEY (endereco_entrega_id) REFERENCES endereco(id)
) ENGINE=InnoDB;

CREATE TABLE item_pedido (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    pedido_id         BIGINT NOT NULL,
    produto_id        BIGINT NOT NULL,
    quantidade        INT NOT NULL,
    preco_unitario    DECIMAL(10,2) NOT NULL,
    desconto_aplicado DECIMAL(10,2) NOT NULL DEFAULT 0,
    combo_id          BIGINT,
    FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produto(id),
    FOREIGN KEY (combo_id) REFERENCES combo(id),
    CHECK (quantidade > 0)
) ENGINE=InnoDB;

CREATE TABLE pagamento (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    pedido_id             BIGINT NOT NULL,
    forma                 ENUM('cartao_credito','cartao_debito','pix','dinheiro') NOT NULL,
    status                ENUM('pendente','aprovado','recusado','estornado') NOT NULL DEFAULT 'pendente',
    valor                 DECIMAL(10,2) NOT NULL,
    transacao_externa_id  VARCHAR(100),
    criado_em             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pedido_id) REFERENCES pedido(id)
) ENGINE=InnoDB;

CREATE TABLE compra_oferta_diaria (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id        BIGINT NOT NULL,
    produto_id        BIGINT NOT NULL,
    data_referencia   DATE NOT NULL,
    quantidade        INT NOT NULL DEFAULT 0,
    FOREIGN KEY (cliente_id) REFERENCES cliente(id),
    FOREIGN KEY (produto_id) REFERENCES produto(id),
    UNIQUE (cliente_id, produto_id, data_referencia)
) ENGINE=InnoDB;

CREATE INDEX idx_produto_categoria     ON produto(categoria_id);
CREATE INDEX idx_produto_fornecedor    ON produto(fornecedor_id);
CREATE INDEX idx_pedido_cliente        ON pedido(cliente_id);
CREATE INDEX idx_pedido_status         ON pedido(status);
CREATE INDEX idx_item_pedido_pedido    ON item_pedido(pedido_id);
CREATE INDEX idx_pagamento_pedido      ON pagamento(pedido_id);
