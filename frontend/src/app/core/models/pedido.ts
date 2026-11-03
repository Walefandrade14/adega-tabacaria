import { Endereco } from './conta';

export type TipoEntrega = 'DELIVERY' | 'RETIRADA' | 'BALCAO';
export type FormaPagamento = 'CARTAO_CREDITO' | 'CARTAO_DEBITO' | 'PIX' | 'DINHEIRO';
export type StatusPagamento = 'PENDENTE' | 'APROVADO' | 'RECUSADO' | 'ESTORNADO';
export type StatusPedido = 'ABERTO' | 'AGUARDANDO_PAGAMENTO' | 'PAGO' | 'EM_SEPARACAO' | 'EM_ENTREGA' | 'CONCLUIDO' | 'CANCELADO';

export interface ItemRequisicao {
  produtoId: number;
  quantidade: number;
}

export interface CarrinhoRequisicao {
  tipoEntrega: TipoEntrega;
  enderecoId: number | null;
  itens: ItemRequisicao[];
}

export interface ItemCalculado {
  produtoId: number;
  nome: string;
  quantidade: number;
  precoUnitario: number;
  desconto: number;
  combo: string | null;
  total: number;
}

export interface Problema {
  regra: string;
  mensagem: string;
}

export interface Calculo {
  itens: ItemCalculado[];
  subtotal: number;
  descontoTotal: number;
  taxaEntrega: number;
  total: number;
  exigeConferenciaDocumento: boolean;
  podeFinalizar: boolean;
  problemas: Problema[];
}

export interface Pagamento {
  id: number;
  pedidoId: number;
  forma: FormaPagamento;
  status: StatusPagamento;
  valor: number;
  transacaoExternaId: string | null;
  pixCopiaECola: string | null;
  criadoEm: string;
}

export interface ItemPedido {
  produtoId: number;
  produtoNome: string;
  quantidade: number;
  precoUnitario: number;
  descontoAplicado: number;
  combo: string | null;
}

export interface Pedido {
  id: number;
  status: StatusPedido;
  tipoEntrega: TipoEntrega;
  clienteId: number | null;
  clienteNome: string | null;
  funcionarioResponsavel: string | null;
  enderecoEntrega: Endereco | null;
  itens: ItemPedido[];
  subtotal: number;
  descontoTotal: number;
  taxaEntrega: number;
  total: number;
  pagamentos: Pagamento[];
  criadoEm: string;
  atualizadoEm: string | null;
}

export const STATUS_PEDIDO: Record<StatusPedido, string> = {
  ABERTO: 'Aguardando pagamento',
  AGUARDANDO_PAGAMENTO: 'Aguardando Pix',
  PAGO: 'Pago',
  EM_SEPARACAO: 'Em separação',
  EM_ENTREGA: 'Saiu para entrega',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado'
};

export const COR_STATUS: Record<StatusPedido, string> = {
  ABERTO: 'cinza',
  AGUARDANDO_PAGAMENTO: 'cinza',
  PAGO: 'verde',
  EM_SEPARACAO: '',
  EM_ENTREGA: '',
  CONCLUIDO: 'verde',
  CANCELADO: 'vermelho'
};

export const FORMAS_PAGAMENTO: Record<FormaPagamento, string> = {
  CARTAO_CREDITO: 'Cartão de crédito',
  CARTAO_DEBITO: 'Cartão de débito',
  PIX: 'Pix',
  DINHEIRO: 'Dinheiro'
};

export const STATUS_PAGAMENTO: Record<StatusPagamento, string> = {
  PENDENTE: 'Pendente',
  APROVADO: 'Aprovado',
  RECUSADO: 'Recusado',
  ESTORNADO: 'Estornado'
};

export function proximoStatus(pedido: Pedido): StatusPedido | null {
  switch (pedido.status) {
    case 'PAGO':
      return 'EM_SEPARACAO';
    case 'EM_SEPARACAO':
      return pedido.tipoEntrega === 'DELIVERY' ? 'EM_ENTREGA' : 'CONCLUIDO';
    case 'EM_ENTREGA':
      return 'CONCLUIDO';
    default:
      return null;
  }
}

export function clientePodeCancelar(pedido: Pedido): boolean {
  return pedido.status === 'ABERTO' || pedido.status === 'AGUARDANDO_PAGAMENTO' || pedido.status === 'PAGO';
}
