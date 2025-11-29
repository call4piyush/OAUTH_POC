FROM node:20-alpine AS build
WORKDIR /app
COPY package.json angular.json tsconfig.json tsconfig.app.json ./
COPY src ./src
RUN npm install -g pnpm \
    && pnpm install \
    && pnpm build

FROM nginx:1.27-alpine
COPY --from=build /app/dist/auth-ui /usr/share/nginx/html
EXPOSE 80


