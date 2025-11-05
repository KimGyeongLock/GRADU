# ---- Build stage ----
FROM node:20-alpine AS build
WORKDIR /app
COPY gradu-web/package*.json ./
RUN npm ci
COPY gradu-web/ .

# VITE_API_BASE=/api 로 프록시 전제
ARG VITE_API_BASE=/api
ENV VITE_API_BASE=$VITE_API_BASE
RUN npm run build

# ---- Serve stage ----
FROM nginx:alpine
# 기본 nginx.conf 교체 예정
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
