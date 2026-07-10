# Stage 1: Build the application
FROM oven/bun:1-alpine AS builder

WORKDIR /app

# Copy dependency files first to leverage Docker caching
COPY package.json bun.lockb ./
RUN bun install --frozen-lockfile

# Copy all source files and build the SvelteKit app
COPY . .
RUN bun run build

# Stage 2: Run the application
FROM oven/bun:1-alpine AS runner

WORKDIR /app

# Set production environment
ENV NODE_ENV=production
ENV PORT=3000
ENV HOST=0.0.0.0
ENV PROTOCOL_HEADER=x-forwarded-proto
ENV HOST_HEADER=x-forwarded-host

# Copy only the compiled build and production dependencies from stage 1
COPY --from=builder /app/build ./build
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/bun.lockb ./bun.lockb

# Install only production dependencies to keep the image lightweight
RUN bun install --production --frozen-lockfile

EXPOSE 3000

CMD ["bun", "run", "./build/index.js"]

