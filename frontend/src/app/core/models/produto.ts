import { Categoria } from './categoria';

export interface Produto {
  id: number;
  nome: string;
  descricao?: string;
  preco: number;
  estoqueQuantidade: number;
  categoria: Categoria;
  fornecedorId?: number | null;
  restritoMaiorIdade: boolean;
  ofertaEspecial: boolean;
  limiteDiarioOferta?: number | null;
  ativo: boolean;
  imagemUrl?: string | null;
}

export type OrigemDados = 'api' | 'exemplo';

export interface ResultadoProdutos {
  produtos: Produto[];
  origem: OrigemDados;
}

export interface ProdutoCadastro {
  nome: string;
  descricao: string | null;
  preco: number;
  estoqueQuantidade: number;
  categoriaId: number;
  fornecedorId: number | null;
  restritoMaiorIdade: boolean;
  ofertaEspecial: boolean;
  limiteDiarioOferta: number | null;
  ativo: boolean;
  imagemUrl: string | null;
}
