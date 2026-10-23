export function destinoSeguro(destino: string | null | undefined, padrao: string): string {
  return destino && destino.startsWith('/') && !destino.startsWith('//') ? destino : padrao;
}
