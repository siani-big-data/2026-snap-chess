# ── Stage 1: build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

WORKDIR /app

# Copia solo los manifiestos primero para aprovechar la caché de capas
COPY package.json package-lock.json ./
RUN npm ci

# Copia el resto del código y construye
COPY . .
RUN npm run build

# ── Stage 2: serve ───────────────────────────────────────────────────────────
FROM nginx:1.25-alpine

# Elimina la configuración por defecto
RUN rm /etc/nginx/conf.d/default.conf

# Configuración personalizada para SPA (Vue Router en modo history)
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copia los artefactos de build
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]