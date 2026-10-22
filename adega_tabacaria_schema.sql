-- ============================================================
-- Banco de dados — Adega & Tabacaria (comércio para autônomo)
-- MySQL 8+
-- Modelado a partir da tabela de Regras de Negócio (RN01–RN09)
-- ============================================================

CREATE DATABASE IF NOT EXISTS adega_tabacaria
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE adega_tabacaria;

-- ------------------------------------------------------------
-- Pessoas
-- ------------------------------------------------------------

CREATE TABLE cliente (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome              VARCHAR(120) NOT NULL,
    cpf               CHAR(11) NOT NULL UNIQUE,           -- RN06: chave de controle de limite por oferta
    data_nascimento   DATE NOT NULL,                      -- RN01: validação de maioridade
    email             VARCHAR(160) NOT NULL UNIQUE,
    senha_hash        VARCHAR(255) NOT NULL,
    telefone          VARCHAR(20),
    criado_em         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE endereco (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id        BIGINT NOT NULL,
    apelido           VARCHAR(40),                        -- "Casa", "Trabalho"
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

-- ------------------------------------------------------------
-- Catálogo
-- ------------------------------------------------------------

CREATE TABLE fornecedor (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    razao_social      VARCHAR(160) NOT NULL,
    cnpj              CHAR(14) NOT NULL UNIQUE,
    telefone          VARCHAR(20),
    email             VARCHAR(160)
) ENGINE=InnoDB;

CREATE TABLE categoria_produto (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome              VARCHAR(60) NOT NULL UNIQUE          -- Bebidas, Essências, Cigarros, Carvão...
) ENGINE=InnoDB;

CREATE TABLE produto (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome                  VARCHAR(160) NOT NULL,
    descricao             TEXT,
    preco                 DECIMAL(10,2) NOT NULL,
    estoque_quantidade    INT NOT NULL DEFAULT 0,
    categoria_id          BIGINT NOT NULL,
    fornecedor_id         BIGINT,
    restrito_maior_idade  BOOLEAN NOT NULL DEFAULT FALSE,  -- RN01
    oferta_especial       BOOLEAN NOT NULL DEFAULT FALSE,  -- RN06
    limite_diario_oferta  INT,                             -- RN06: ex. 3 unidades/CPF/dia (NULL = sem limite)
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (categoria_id) REFERENCES categoria_produto(id),
    FOREIGN KEY (fornecedor_id) REFERENCES fornecedor(id),
    CHECK (estoque_quantidade >= 0)                        -- RN02
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Promoções / combos (RN03)
-- ------------------------------------------------------------

CREATE TABLE combo (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome                  VARCHAR(120) NOT NULL,
    desconto_percentual   DECIMAL(5,2),                    -- ex. 10.00 = 10%
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

-- ------------------------------------------------------------
-- Funcionamento e entrega (RN04, RN05, RN09)
-- ------------------------------------------------------------

CREATE TABLE horario_funcionamento (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    dia_semana        TINYINT NOT NULL,                    -- 1=segunda ... 7=domingo
    hora_abertura     TIME NOT NULL,                        -- ex. 18:00
    hora_fechamento   TIME NOT NULL,                        -- ex. 02:00 (vira o dia — tratar no backend: fechamento < abertura => intervalo cruza a meia-noite)
    aceita_delivery   BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (dia_semana)
) ENGINE=InnoDB;

CREATE TABLE taxa_entrega_regiao (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    bairro            VARCHAR(80) NOT NULL,
    cep_prefixo       CHAR(5),                              -- alternativa por faixa de CEP
    valor             DECIMAL(10,2) NOT NULL,
    UNIQUE (bairro)
) ENGINE=InnoDB;

-- valor mínimo de pedido para delivery fica em config, não hardcoded
CREATE TABLE configuracao (
    chave             VARCHAR(60) PRIMARY KEY,
    valor             VARCHAR(255) NOT NULL
) ENGINE=InnoDB;
INSERT INTO configuracao (chave, valor) VALUES
    ('valor_minimo_delivery', '35.00'),                     -- RN05
    ('limite_padrao_oferta_dia', '3');                      -- RN06

-- ------------------------------------------------------------
-- Pedido / venda
-- ------------------------------------------------------------

CREATE TABLE pedido (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    cliente_id            BIGINT,                           -- NULL em venda de balcão sem cadastro
    funcionario_id        BIGINT,                           -- quem bateu no PDV / despachou
    tipo_entrega          ENUM('delivery','retirada','balcao') NOT NULL,
    endereco_entrega_id   BIGINT,                            -- obrigatório quando tipo_entrega = delivery
    status                ENUM('aberto','aguardando_pagamento','pago',
                                'em_separacao','em_entrega','concluido','cancelado')
                          NOT NULL DEFAULT 'aberto',          -- RN07, RN08
    subtotal              DECIMAL(10,2) NOT NULL DEFAULT 0,
    taxa_entrega          DECIMAL(10,2) NOT NULL DEFAULT 0,   -- RN09 (não entra no mínimo da RN05)
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
    preco_unitario    DECIMAL(10,2) NOT NULL,                -- snapshot do preço no momento da venda
    desconto_aplicado DECIMAL(10,2) NOT NULL DEFAULT 0,       -- rastreia RN03
    combo_id          BIGINT,                                 -- se o item veio de um combo
    FOREIGN KEY (pedido_id) REFERENCES pedido(id) ON DELETE CASCADE,
    FOREIGN KEY (produto_id) REFERENCES produto(id),
    FOREIGN KEY (combo_id) REFERENCES combo(id),
    CHECK (quantidade > 0)
) ENGINE=InnoDB;

CREATE TABLE pagamento (
    id                    BIGINT PRIMARY KEY AUTO_INCREMENT,
    pedido_id             BIGINT NOT NULL,
    forma                 ENUM('cartao_credito','cartao_debito','pix','dinheiro') NOT NULL,
    status                ENUM('pendente','aprovado','recusado','estornado') NOT NULL DEFAULT 'pendente', -- RN07
    valor                 DECIMAL(10,2) NOT NULL,
    transacao_externa_id  VARCHAR(100),                       -- id retornado pelo gateway
    criado_em             DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (pedido_id) REFERENCES pedido(id)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Controle de limite diário por oferta (RN06, por CPF)
-- ------------------------------------------------------------

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

-- ------------------------------------------------------------
-- Índices de apoio
-- ------------------------------------------------------------

CREATE INDEX idx_produto_categoria     ON produto(categoria_id);
CREATE INDEX idx_produto_fornecedor    ON produto(fornecedor_id);
CREATE INDEX idx_pedido_cliente        ON pedido(cliente_id);
CREATE INDEX idx_pedido_status         ON pedido(status);
CREATE INDEX idx_item_pedido_pedido    ON item_pedido(pedido_id);
CREATE INDEX idx_pagamento_pedido      ON pagamento(pedido_id);
