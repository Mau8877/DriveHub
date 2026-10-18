# ==========================================
# BASE: Python 3.14 (trae uuid.uuid7() nativo)
# ==========================================
FROM python:3.14-slim AS base
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ==========================================
# DEV: lo usa docker-compose. El código se monta por volumen y uvicorn
# recarga solo al cambiar un archivo.
# ==========================================
FROM base AS dev
COPY requirements-dev.txt .
RUN pip install --no-cache-dir -r requirements-dev.txt
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8087", "--reload"]

# ==========================================
# PROD: imagen final, sin dependencias de desarrollo ni root.
# ==========================================
FROM base AS prod
COPY app ./app
RUN useradd --system --no-create-home appuser
USER appuser
EXPOSE 8087
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8087"]
