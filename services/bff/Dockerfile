FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json tsconfig.json ./
RUN npm install -g pnpm \
    && pnpm install

FROM node:20-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY package.json tsconfig.json ./ 
COPY src ./src
RUN npm install -g pnpm \
    && pnpm build

FROM node:20-alpine
WORKDIR /app
ENV NODE_ENV=production
COPY --from=deps /app/node_modules ./node_modules
COPY --from=build /app/dist ./dist
COPY package.json ./
CMD ["node", "dist/index.js"]


