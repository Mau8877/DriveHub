# Errores — Asistente IA (`IA`)

Códigos del microservicio Asistente IA (ver
[EXCEPCIONES_BACKEND.md](../../../.claude/rules/backend/EXCEPCIONES_BACKEND.md#catálogo-de-códigos-de-error)).
Las constantes viven en `app/common/error_codes.py`.

## `ERR_IA_01`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La miniatura de la ayuda del tablero no es válida (CU40): no es una imagen JPEG o PNG
  (se comprueba por los primeros bytes del archivo, no por el nombre) o pesa más de 500 KB. No se guarda nada.
- **Dónde se lanza:** `AyudaTableroService._validar_miniatura()`, desde `AyudaTableroService.crear()`

## `ERR_IA_02`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** No se pudo guardar la miniatura en MinIO (el almacén de archivos no responde). Se puede reintentar
  más tarde; la ayuda no se guarda para no perder en silencio una foto que la persona mandó (CU40).
- **Dónde se lanza:** `AyudaTableroService.crear()`

## `ERR_IA_03`

- **HTTP:** 404
- **Excepción:** `NotFoundException`
- **Significado:** La ayuda del tablero pedida no existe para quien la pide (CU42). Una ayuda de otra persona o un
  código que no existe se responde igual, para no revelar qué ayudas existen.
- **Dónde se lanza:** `AyudaTableroService._buscar()`, y a través de él `obtener()` y `marcar_util()`

## `ERR_IA_04`

- **HTTP:** 400 (no llega por HTTP: lo ve el modelo como el mensaje de una herramienta MCP)
- **Excepción:** `ValidationException`
- **Significado:** El código de confirmación de `confirmar_reserva` o `confirmar_compra` (CU49) no sirve: no existe, es de otra persona, es de la otra acción, venció (10 minutos) o ya se está procesando. Responde igual en todos los casos para no revelar confirmaciones ajenas. No se ejecuta nada.
- **Dónde se lanza:** `ConfirmacionesService.ejecutar()`

## `ERR_IA_05`

- **HTTP:** 409 (no llega por HTTP: lo ve el modelo como el mensaje de una herramienta MCP)
- **Excepción:** `ConflictException`
- **Significado:** La persona ya ejecutó el máximo de acciones de escritura por chat de ese día (`TOPE_ESCRITURAS_DIARIAS`, 10 por defecto). Limita el daño de un número o un modelo que se equivoque; puede seguir haciéndolo desde la app.
- **Dónde se lanza:** `ConfirmacionesService.ejecutar()`

## `ERR_IA_06`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** El modelo no pudo relacionar el pedido del Administrador con ninguna fuente del catálogo de reportes (CU52): pidió algo que no es clases, pagos, exámenes, notas, estudiantes ni vehículos.
- **Dónde se lanza:** `ReporteService.limpiar()`

## `ERR_IA_07`

- **HTTP:** 400
- **Excepción:** `ValidationException`
- **Significado:** La especificación que mandó la pantalla para generar un reporte (CU52) nombra algo que no existe en el catálogo (fuente, columna, estado o tipo), repite columnas, filtra por fecha una fuente sin fecha u ordena por una columna desconocida, o tiene el rango de fechas invertido.
- **Dónde se lanza:** `ReporteService.validar()`

## `ERR_IA_08`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** El modelo de OpenRouter que interpreta los pedidos de reportes (CU52) no está configurado (`OPENROUTER_API_KEY` vacía), no respondió o respondió algo que no es un objeto JSON. El reporte se puede armar a mano.
- **Dónde se lanza:** `InterpreteOpenRouter.interpretar()`

## `ERR_IA_09`

- **HTTP:** 503
- **Excepción:** `ServicioNoDisponibleException`
- **Significado:** El servicio dueño de los datos de un reporte (CU52: Académico, Pagos, Escuela o Identidad) no respondió o respondió un error que no es de permisos.
- **Dónde se lanza:** `LectorDatosHttp._pagina()`
