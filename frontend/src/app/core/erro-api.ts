import { HttpErrorResponse } from '@angular/common/http';

const ROTULOS: Record<string, string> = {
  nome: 'Nome',
  cpf: 'CPF',
  dataNascimento: 'Data de nascimento',
  email: 'E-mail',
  senha: 'Senha',
  telefone: 'Telefone',
  cep: 'CEP',
  uf: 'UF',
  preco: 'Preço',
  estoqueQuantidade: 'Estoque',
  categoriaId: 'Categoria',
  limiteDiarioOferta: 'Limite diário',
  imagemUrl: 'URL da imagem'
};

export function mensagemDeErro(erro: unknown, padrao = 'Não foi possível concluir agora. Tente de novo.'): string {
  if (!(erro instanceof HttpErrorResponse)) return padrao;
  if (erro.status === 0) return 'Não foi possível falar com o servidor. Confira se a API está no ar e tente de novo.';
  const corpo = erro.error as { detail?: string; campos?: Record<string, string> } | null;
  const campos = corpo?.campos ? Object.entries(corpo.campos) : [];
  if (campos.length) {
    const [campo, msg] = campos[0];
    return `${ROTULOS[campo] ?? campo}: ${msg}`;
  }
  if (erro.status === 403) return 'Seu perfil não tem acesso a esta área.';
  if (typeof corpo?.detail === 'string' && corpo.detail) return corpo.detail;
  return padrao;
}
