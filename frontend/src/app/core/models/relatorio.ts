import { TipoEntrega } from './pedido';

export interface Relatorio {
  de: string;
  ate: string;
  pedidos: number;
  faturamento: number;
  ticketMedio: number;
  cancelados: number;
  canais: { tipo: TipoEntrega; pedidos: number; faturamento: number }[];
  dias: { data: string; pedidos: number; faturamento: number }[];
  maisVendidos: { produtoId: number; nome: string; quantidade: number; faturamento: number }[];
  limiteEstoqueBaixo: number;
  estoqueBaixo: { produtoId: number; nome: string; estoque: number }[];
}
