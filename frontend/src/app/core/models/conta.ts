export interface Cliente {
  id: number;
  nome: string;
  email: string;
  telefone: string | null;
  dataNascimento: string;
  maiorDeIdade: boolean;
}

export interface Endereco {
  id: number;
  apelido: string | null;
  logradouro: string;
  numero: string;
  complemento: string | null;
  bairro: string;
  cidade: string;
  uf: string;
  cep: string;
  principal: boolean;
}

export type NovoEndereco = Omit<Endereco, 'id'>;

export interface TaxaEntrega {
  id: number;
  bairro: string;
  cepPrefixo: string | null;
  valor: number;
}

export interface EnderecoViaCep {
  logradouro?: string;
  bairro?: string;
  localidade?: string;
  uf?: string;
  erro?: boolean | string;
}
