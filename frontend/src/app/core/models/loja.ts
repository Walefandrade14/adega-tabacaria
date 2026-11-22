export type Cargo = 'DONO' | 'CAIXA' | 'ESTOQUE' | 'ENTREGADOR';

export interface Funcionario {
  id: number;
  nome: string;
  cargo: Cargo;
  usuario: string;
  ativo: boolean;
}

export interface FuncionarioCadastro {
  nome: string;
  cpf: string;
  cargo: Cargo;
  usuario: string;
  senha: string;
}

export interface Horario {
  diaSemana: number;
  nomeDia: string;
  horaAbertura: string;
  horaFechamento: string;
  aceitaDelivery: boolean;
}

export interface TaxaEntrega {
  id: number;
  bairro: string;
  cepPrefixo: string | null;
  valor: number;
}

export interface TaxaEntregaCadastro {
  bairro: string;
  cepPrefixo: string | null;
  valor: number;
}

export interface Configuracao {
  chave: string;
  valor: string;
}
