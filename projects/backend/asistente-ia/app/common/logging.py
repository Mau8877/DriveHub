"""Logs en JSON estructurado, una línea por evento.

Mismo formato que logstash-logback-encoder en los servicios Spring (ver
.claude/rules/backend/LOGGING_BACKEND.md). Los campos contextuales se pasan
con `extra={...}` (ej. `logger.info("Sesión creada", extra={"modulo": "sesiones"})`),
nunca concatenados al mensaje.

* El campo del JSON se llama `module` (igual que en los demás servicios), pero `module` es un
* atributo reservado de `LogRecord` y `extra` no puede pisarlo: se pasa como `modulo` y el
* formatter lo escribe como `module`.
"""

import json
import logging
from datetime import UTC, datetime

# color_message lo agrega uvicorn (el mismo mensaje con códigos ANSI de color).
_CAMPOS_ESTANDAR = set(logging.makeLogRecord({}).__dict__) | {"message", "asctime", "color_message"}


class JsonFormatter(logging.Formatter):
    def format(self, record: logging.LogRecord) -> str:
        entrada = {
            "@timestamp": datetime.fromtimestamp(record.created, UTC).isoformat(),
            "level": record.levelname,
            "logger_name": record.name,
            "message": record.getMessage(),
        }
        entrada.update({k: v for k, v in record.__dict__.items() if k not in _CAMPOS_ESTANDAR})
        if "modulo" in entrada:
            entrada["module"] = entrada.pop("modulo")
        if record.exc_info:
            entrada["stack_trace"] = self.formatException(record.exc_info)
        return json.dumps(entrada, ensure_ascii=False, default=str)


def configurar_logging() -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter())
    raiz = logging.getLogger()
    raiz.handlers = [handler]
    raiz.setLevel(logging.INFO)
    # uvicorn trae sus propios handlers de texto plano: se reemplazan por el JSON.
    for nombre in ("uvicorn", "uvicorn.error", "uvicorn.access"):
        logging.getLogger(nombre).handlers = []
        logging.getLogger(nombre).propagate = True
