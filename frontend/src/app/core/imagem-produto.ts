import { CATEGORIAS_VITRINE, slugCategoria } from './models/categoria';
import { Produto } from './models/produto';

export function imagemDoProduto(produto: Produto): string {
  if (produto.imagemUrl) return produto.imagemUrl;
  const slug = slugCategoria(produto.categoria?.nome ?? '');
  return CATEGORIAS_VITRINE.find(c => c.slug === slug)?.imagem ?? 'img/cat-bebidas.jpg';
}
