# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY gradu-web/package*.json ./
RUN npm ci
COPY gradu-web/ .

ARG VITE_API_BASE=/api
ENV VITE_API_BASE=$VITE_API_BASE
RUN npm run build

# ---- Serve stage ----
FROM nginxinc/nginx-unprivileged:1.27-alpine

COPY --chown=101:101 nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build --chown=101:101 /app/dist /usr/share/nginx/html

EXPOSE 8080

# ✅ Trivy AVD-DS-0002 해결: non-root 명시
USER 101
