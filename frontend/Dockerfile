FROM node:26-alpine AS deps
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

FROM node:26-alpine AS deps-prod
WORKDIR /app

COPY package.json package-lock.json ./
COPY --from=deps /app/node_modules ./node_modules/
RUN npm prune --omit=dev

FROM node:26-alpine AS builder
WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:26-alpine AS runner
WORKDIR /app

RUN addgroup -S -g 1001 sveltekit
RUN adduser -S -u 1001 -G sveltekit sveltekit

COPY --from=builder --chown=sveltekit:sveltekit /app/build ./build
COPY --from=deps-prod --chown=sveltekit:sveltekit /app/node_modules ./node_modules
COPY --chown=sveltekit:sveltekit package.json .

USER sveltekit
EXPOSE 3000
ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0
ENV PROTOCOL_HEADER=x-forwarded-proto
ENV HOST_HEADER=x-forwarded-host

HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://127.0.0.1:3000/api/health || exit 1

CMD ["node", "build/index.js"]
