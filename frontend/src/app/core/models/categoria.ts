export interface Categoria {
  id: number;
  nome: string;
}

export type CategoriaSlug = 'bebidas' | 'tabacaria' | 'narguile' | 'acessorios';

export interface CategoriaVitrine {
  slug: CategoriaSlug;
  titulo: string;
  descricao: string;
  imagem: string;
}

export const CATEGORIAS_VITRINE: CategoriaVitrine[] = [
  { slug: 'bebidas', titulo: 'Bebidas', descricao: 'Variedade e qualidade para todos os gostos', imagem: 'img/cat-bebidas.jpg' },
  { slug: 'tabacaria', titulo: 'Tabacaria', descricao: 'Tabacos selecionados e acessórios', imagem: 'img/cat-tabacaria.jpg' },
  { slug: 'narguile', titulo: 'Narguilé', descricao: 'Sabores, kits e todos os acessórios', imagem: 'img/cat-narguile.jpg' },
  { slug: 'acessorios', titulo: 'Acessórios', descricao: 'Detalhes que fazem toda a diferença', imagem: 'img/cat-acessorios.jpg' }
];

export function slugCategoria(nome: string): string {
  return nome
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase()
    .trim()
    .replace(/\s+/g, '-');
}
