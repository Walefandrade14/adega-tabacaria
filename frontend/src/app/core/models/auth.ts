export type Perfil = 'CLIENTE' | 'DONO' | 'CAIXA' | 'ENTREGADOR' | 'ESTOQUE';

export interface Sessao {
  token: string;
  tipo: string;
  expiraEm: string;
  nome: string;
  perfil: Perfil;
}

export interface CadastroCliente {
  nome: string;
  cpf: string;
  dataNascimento: string;
  email: string;
  senha: string;
  telefone: string | null;
}
