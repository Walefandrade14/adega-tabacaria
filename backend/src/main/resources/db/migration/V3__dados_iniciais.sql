INSERT INTO categoria_produto (id, nome) VALUES
    (1, 'Bebidas'),
    (2, 'Tabacaria'),
    (3, 'Narguilé'),
    (4, 'Acessórios');

INSERT INTO fornecedor (id, razao_social, cnpj, telefone, email) VALUES
    (1, 'Distribuidora Exemplo Ltda', '11222333000181', '1130000000', 'contato@distribuidora.exemplo');

INSERT INTO produto (id, nome, descricao, preco, estoque_quantidade, categoria_id, fornecedor_id, restrito_maior_idade, oferta_especial, limite_diario_oferta, ativo) VALUES
    (1, 'Whisky 12 anos 1L', 'Blended escocês, notas de mel e carvalho.', 189.90, 12, 1, 1, TRUE, FALSE, NULL, TRUE),
    (2, 'Fardo de cerveja lata 12un', 'Pilsen 350ml, bem gelada.', 49.90, 30, 1, 1, TRUE, TRUE, 3, TRUE),
    (3, 'Energético 473ml', 'Perfeito para o combo com destilado.', 11.50, 60, 1, 1, FALSE, FALSE, NULL, TRUE),
    (4, 'Charuto artesanal', 'Capa natural, fumo selecionado.', 34.00, 8, 2, NULL, TRUE, FALSE, NULL, TRUE),
    (5, 'Tabaco para cachimbo 50g', 'Blend aromático suave.', 42.00, 0, 2, NULL, TRUE, FALSE, NULL, TRUE),
    (6, 'Essência 50g', 'Sabores variados para narguilé.', 22.90, 40, 3, NULL, TRUE, FALSE, NULL, TRUE),
    (7, 'Kit narguilé completo', 'Vaso, stem, rosh, mangueira e prato.', 249.00, 5, 3, NULL, FALSE, FALSE, NULL, TRUE),
    (8, 'Carvão de coco 1kg', 'Queima limpa e duradoura.', 29.90, 25, 3, NULL, TRUE, FALSE, NULL, TRUE),
    (9, 'Isqueiro metálico', 'Recarregável, chama tipo maçarico.', 39.90, 15, 4, NULL, FALSE, FALSE, NULL, TRUE),
    (10, 'Dichavador 4 partes', 'Alumínio anodizado.', 59.90, 10, 4, NULL, FALSE, FALSE, NULL, TRUE);

INSERT INTO combo (id, nome, desconto_percentual, desconto_valor_fixo, ativo) VALUES
    (1, 'Whisky + 2 energéticos', 10.00, NULL, TRUE);

INSERT INTO combo_item (combo_id, produto_id, quantidade) VALUES
    (1, 1, 1),
    (1, 3, 2);

INSERT INTO horario_funcionamento (dia_semana, hora_abertura, hora_fechamento, aceita_delivery) VALUES
    (1, '10:00:00', '02:00:00', TRUE),
    (2, '10:00:00', '02:00:00', TRUE),
    (3, '10:00:00', '02:00:00', TRUE),
    (4, '10:00:00', '02:00:00', TRUE),
    (5, '10:00:00', '02:00:00', TRUE),
    (6, '10:00:00', '02:00:00', TRUE),
    (7, '10:00:00', '02:00:00', TRUE);

INSERT INTO taxa_entrega_regiao (bairro, cep_prefixo, valor) VALUES
    ('Centro', NULL, 5.00),
    ('Vila Nova', NULL, 7.00),
    ('Jardim América', NULL, 9.00);
