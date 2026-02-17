# ============================================
# Stage 1: Builder
# ============================================
FROM python:3.10-slim as builder

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y \
    build-essential \
    curl \
    git \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN python -m venv /app/.venv
ENV PATH="/app/.venv/bin:$PATH"

COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# ============================================
# Stage 2: Runner
# ============================================
FROM python:3.10-slim as runner

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PATH="/app/.venv/bin:$PATH"

RUN apt-get update && apt-get install -y \
    curl \
    libgomp1 \
    dos2unix \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=builder /app/.venv /app/.venv

COPY . .

RUN mkdir -p logs && \
    dos2unix entrypoint.sh && \
    chmod +x entrypoint.sh

HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8000/health || exit 1

EXPOSE 8000

# CRLF(LF) 정규화 후 entrypoint 실행 (볼륨 마운트 시 Windows CRLF 대응)
CMD ["/bin/bash", "-c", "dos2unix /app/entrypoint.sh 2>/dev/null || true; exec /bin/bash /app/entrypoint.sh"]
