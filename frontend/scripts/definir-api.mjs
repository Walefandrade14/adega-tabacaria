import { writeFileSync } from 'node:fs';

const apiUrl = (process.env.API_URL || '').trim().replace(/\/+$/, '');

if (!apiUrl) {
  console.log('API_URL não definida: mantendo src/environments/environment.prod.ts');
  process.exit(0);
}

const conteudo = `export const environment = {
  apiUrl: '${apiUrl}',
  usarDadosExemploSeApiOffline: true,
  timeoutMs: 75000
};
`;

writeFileSync(new URL('../src/environments/environment.prod.ts', import.meta.url), conteudo);
console.log(`API de produção: ${apiUrl}`);
