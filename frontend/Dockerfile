# ── Stage 1: Build Angular app ───────────────────────────────────────────────
FROM node:22-alpine AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build -- --configuration production

# ── Stage 2: Serve with nginx ────────────────────────────────────────────────
FROM nginx:1.27-alpine

# Remove default nginx config
RUN rm /etc/nginx/conf.d/default.conf

COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built Angular app (output folder: dist/itsi-container-frontend/browser)
COPY --from=builder /app/dist/itsi-container-frontend/browser /usr/share/nginx/html

EXPOSE 9090

CMD ["nginx", "-g", "daemon off;"]
