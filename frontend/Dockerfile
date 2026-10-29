FROM node:22-slim AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps --no-audit --no-fund
COPY . .
ARG API_URL=/api
RUN API_URL=$API_URL node scripts/definir-api.mjs && npm run build

FROM caddy:2.10-alpine
COPY --from=build /app/dist/loud-feliz-web/browser /srv
