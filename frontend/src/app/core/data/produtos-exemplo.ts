import { Produto } from '../models/produto';

const bebidas = { id: 1, nome: 'Bebidas' };
const tabacaria = { id: 2, nome: 'Tabacaria' };
const narguile = { id: 3, nome: 'Narguilé' };
const acessorios = { id: 4, nome: 'Acessórios' };

export const PRODUTOS_EXEMPLO: Produto[] = [
  { id: 1, nome: 'Whisky 12 anos 1L', descricao: 'Blended escocês, notas de mel e carvalho.', preco: 189.9, estoqueQuantidade: 12, categoria: bebidas, restritoMaiorIdade: true, ofertaEspecial: false, ativo: true },
  { id: 2, nome: 'Fardo de cerveja lata 12un', descricao: 'Pilsen 350ml, bem gelada.', preco: 49.9, estoqueQuantidade: 30, categoria: bebidas, restritoMaiorIdade: true, ofertaEspecial: true, limiteDiarioOferta: 3, ativo: true },
  { id: 3, nome: 'Energético 473ml', descricao: 'Perfeito para o combo com destilado.', preco: 11.5, estoqueQuantidade: 60, categoria: bebidas, restritoMaiorIdade: false, ofertaEspecial: false, ativo: true },
  { id: 4, nome: 'Charuto artesanal', descricao: 'Capa natural, fumo selecionado.', preco: 34.0, estoqueQuantidade: 8, categoria: tabacaria, restritoMaiorIdade: true, ofertaEspecial: false, ativo: true },
  { id: 5, nome: 'Tabaco para cachimbo 50g', descricao: 'Blend aromático suave.', preco: 42.0, estoqueQuantidade: 0, categoria: tabacaria, restritoMaiorIdade: true, ofertaEspecial: false, ativo: true },
  { id: 6, nome: 'Essência 50g', descricao: 'Sabores variados para narguilé.', preco: 22.9, estoqueQuantidade: 40, categoria: narguile, restritoMaiorIdade: true, ofertaEspecial: false, ativo: true },
  { id: 7, nome: 'Kit narguilé completo', descricao: 'Vaso, stem, rosh, mangueira e prato.', preco: 249.0, estoqueQuantidade: 5, categoria: narguile, restritoMaiorIdade: false, ofertaEspecial: false, ativo: true },
  { id: 8, nome: 'Carvão de coco 1kg', descricao: 'Queima limpa e duradoura.', preco: 29.9, estoqueQuantidade: 25, categoria: narguile, restritoMaiorIdade: true, ofertaEspecial: false, ativo: true },
  { id: 9, nome: 'Isqueiro metálico', descricao: 'Recarregável, chama tipo maçarico.', preco: 39.9, estoqueQuantidade: 15, categoria: acessorios, restritoMaiorIdade: false, ofertaEspecial: false, ativo: true },
  { id: 10, nome: 'Dichavador 4 partes', descricao: 'Alumínio anodizado.', preco: 59.9, estoqueQuantidade: 10, categoria: acessorios, restritoMaiorIdade: false, ofertaEspecial: false, ativo: true }
];
