# Build stage
FROM node:24-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Production stage
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
# Writes /config.js from environment variables at start-up (API_BASE_URL, KEYCLOAK_URL, ...), so the image is not
# tied to one environment. Files checked out on Windows may have CRLF line endings, which break the script.
COPY docker/40-runtime-config.sh /docker-entrypoint.d/40-runtime-config.sh
RUN sed -i 's/\r$//' /docker-entrypoint.d/40-runtime-config.sh && chmod +x /docker-entrypoint.d/40-runtime-config.sh
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
