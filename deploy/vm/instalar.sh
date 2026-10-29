#!/usr/bin/env bash
set -euo pipefail

if [ "$(id -u)" -ne 0 ]; then
  exec sudo bash "$0" "$@"
fi

cd "$(dirname "$0")"
ENDERECO="${1:-}"
ENDERECO="${ENDERECO#http://}"
ENDERECO="${ENDERECO#https://}"
ENDERECO="${ENDERECO%%/*}"

gerar() { openssl rand -hex "$1"; }

definir() {
  if grep -q "^$1=" .env; then
    sed -i "s|^$1=.*|$1=$2|" .env
  else
    echo "$1=$2" >> .env
  fi
}

echo "==> Docker"
if ! command -v docker >/dev/null 2>&1; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker >/dev/null 2>&1 || true

memoria=$(awk '/MemTotal/ {print int($2 / 1024)}' /proc/meminfo)
if [ "$memoria" -lt 3500 ] && [ -z "$(swapon --noheadings)" ]; then
  echo "==> ${memoria} MB de RAM: criando 2 GB de swap para o build"
  fallocate -l 2G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile >/dev/null
  swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi

if [ ! -f .env ]; then
  echo "==> Gerando senhas em deploy/vm/.env"
  cat > .env <<EOF
SITE_ADDRESS=:80
CORS_ORIGINS=http://localhost
DB_ROOT_PASSWORD=$(gerar 24)
DB_PASSWORD=$(gerar 24)
JWT_SECRET=$(gerar 32)
ADMIN_USER=dono
ADMIN_PASSWORD=$(gerar 6)
EOF
  chmod 600 .env
fi

if [ -n "$ENDERECO" ]; then
  if [[ "$ENDERECO" =~ ^[0-9.]+$ ]]; then
    definir SITE_ADDRESS ":80"
    definir CORS_ORIGINS "http://$ENDERECO"
  else
    definir SITE_ADDRESS "$ENDERECO"
    definir CORS_ORIGINS "https://$ENDERECO"
  fi
fi

set -a
. ./.env
set +a

echo "==> Construindo e subindo (a primeira vez leva alguns minutos)"
docker compose up -d --build --remove-orphans
docker image prune -f >/dev/null

echo "==> Esperando a API"
pronta=""
for _ in $(seq 1 120); do
  if docker compose exec -T web wget -qO- http://api:8080/actuator/health 2>/dev/null | grep -q UP; then
    pronta=1
    break
  fi
  sleep 5
done
if [ -z "$pronta" ]; then
  docker compose logs --tail 80 api
  echo "A API não subiu em 10 minutos. Veja os logs acima." >&2
  exit 1
fi

if [ "$SITE_ADDRESS" = ":80" ]; then
  ip="${ENDERECO:-$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || echo IP-DA-VM)}"
  url="http://$ip"
else
  url="https://$SITE_ADDRESS"
  echo "==> Esperando o certificado HTTPS"
  certificado=""
  for _ in $(seq 1 24); do
    if curl -fsS --max-time 10 --resolve "$SITE_ADDRESS:443:127.0.0.1" "$url/api/loja/status" >/dev/null 2>&1; then
      certificado=1
      break
    fi
    sleep 5
  done
  if [ -z "$certificado" ]; then
    echo "Aviso: o HTTPS ainda não respondeu. Confira se as portas 80 e 443 estão liberadas no Azure e se o nome $SITE_ADDRESS aponta para esta VM." >&2
    echo "Logs do Caddy: sudo docker compose -f $(pwd)/docker-compose.yml logs web" >&2
  fi
fi

cat <<EOF

Pronto!
  Site:    $url
  Painel:  $url/admin/entrar   (usuário: $ADMIN_USER   senha: $ADMIN_PASSWORD)
  Swagger: $url/swagger-ui.html

Senhas guardadas em $(pwd)/.env
Logs:      sudo docker compose -f $(pwd)/docker-compose.yml logs -f
Atualizar: rode este script de novo depois de trocar os arquivos (ou git pull)
EOF
