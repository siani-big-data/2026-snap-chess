# ── Stage 1: build con Maven ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src src
RUN ./mvnw package -DskipTests -q

# ── Stage 2: runtime (Debian — Stockfish no está en los repos de Alpine) ──────
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r chessreader && useradd -r -g chessreader chessreader

RUN apt-get update && apt-get install -y --no-install-recommends \
    stockfish wget gosu \
    && ln -sf /usr/games/stockfish /usr/bin/stockfish \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/target/backend-*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN sed -i 's/\r$//' /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh

RUN mkdir -p /data && chown chessreader:chessreader /data

EXPOSE 8080

ENV CHESS_ENGINE_PATH=/usr/bin/stockfish
ENV CHESS_DATA_PATH=/data
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["/docker-entrypoint.sh"]
