ALTER TABLE pedido ADD COLUMN estoque_reservado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE pedido ADD COLUMN conferir_documento BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE pedido p
SET p.conferir_documento = TRUE
WHERE p.tipo_entrega <> 'balcao'
  AND EXISTS (
      SELECT 1
      FROM item_pedido i
      JOIN produto pr ON pr.id = i.produto_id
      WHERE i.pedido_id = p.id AND pr.restrito_maior_idade = TRUE
  );
