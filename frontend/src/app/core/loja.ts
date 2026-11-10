export const LOJA = {
  nome: 'Adega Tabacaria LouD Feliz no Simples',
  endereco: 'Av. Hamilton N° 18',
  horario: 'Diurno e Noturno',
  whatsapp: '5511999999999',
  instagram: 'https://instagram.com/',
  mapa: 'https://maps.google.com/?q=Av.+Hamilton+18'
};

export function linkWhatsapp(mensagem = 'Olá! Vim pelo site da LouD Feliz no Simples.'): string {
  return `https://wa.me/${LOJA.whatsapp}?text=${encodeURIComponent(mensagem)}`;
}
