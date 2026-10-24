# UT-CU07-BE-06 — Foto del auto en Cloudinary y seed de ejemplo

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU07-BE-06` |
| **Nombre** | Foto del auto en Cloudinary y seed de ejemplo |
| **Caso de uso** | CU07-BE |
| **Microservicio** | `escuela` |
| **Depende de** | UT-CU07-BE-05 |

## Antes de implementar

[ARQUITECTURA_CLEAN_BACKEND.md](../../../../.claude/rules/backend/ARQUITECTURA_CLEAN_BACKEND.md) (el dominio define el puerto, la infraestructura lo implementa), [PERSISTENCIA_BD_BACKEND.md](../../../../.claude/rules/backend/PERSISTENCIA_BD_BACKEND.md), [LOGGING_BACKEND.md](../../../../.claude/rules/backend/LOGGING_BACKEND.md) (nunca registrar secretos).

## Solicitud de negocio

Los autos se ven mejor con foto, y el equipo necesita datos de ejemplo para desarrollo.

## Objetivo

Cada auto guarda la URL de su foto en Cloudinary, el backend autoriza la subida sin exponer el API secret, y un seed deja 8 autos de ejemplo.

## Alcance

- Migración `V20261024110000__vehiculo_imagen_url.sql` (`imagen_url VARCHAR(300)`), `VehiculoModel.imagenUrl`, `Vehiculo.imagenUrl` y su paso por DTOs, requests, response y repositorio. `ReglasVehiculo.imagenUrl`: solo URLs `https://res.cloudinary.com/...` de hasta 300 caracteres (`ERR_SYS_01`); al editar, sin foto nueva queda la actual.
- Puerto `domain/gateway/ImagenesGateway` + `FirmaSubidaImagen`; adaptador `CloudinaryImagenesGateway` (SHA-1 de `folder=...&timestamp=...` + secret). Sin variables configuradas lanza `IllegalStateException` (responde 500 `ERR_SYS_00`).
- `FirmarSubidaImagenVehiculoUseCase` (carpeta `drivehub/{escuela}/vehiculos`) y `POST /api/v1/vehiculos/firma-imagen` → `{ cloudName, apiKey, timestamp, folder, signature }`.
- Configuración: `app.cloudinary.*` en `application.properties`, variables `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` y `CLOUDINARY_API_SECRET` en `docker-compose.yml` (servicio `escuela`) y documentadas en `.env.example`.
- `VehiculoSeeder` (perfil `dev`): 8 autos en `escuelaexample`, en los tres estados, con la foto de ejemplo `car` de la cuenta pública `demo` de Cloudinary con un tono distinto por auto. Se siembra por placa (`SEED-001` a `SEED-008`).

Archivos esperados (crear/tocar): la migración, el Model, la entidad, `ReglasVehiculo`, los casos de uso de registrar y editar, los DTOs, el controller y sus requests, el puerto, el adaptador, el caso de uso de firma, el seeder, `application.properties`, `docker-compose.yml`, `.env.example` y sus tests.

## Fuera de alcance (no tocar)

- Borrar de Cloudinary la foto reemplazada: queda en la cuenta (follow-up).
- Quitar la foto de un auto (editar sin foto la deja igual).
- Transformaciones o varias fotos por auto.

## Resultado esperado

Con el stack levantado hay 8 autos con foto; con las tres variables de Cloudinary en `.env`, el Administrador puede subir fotos propias.

## Validación

1. `./mvnw test` en `escuela`: `CloudinaryImagenesGatewayTest` verifica la firma con un secret de prueba, `VehiculoSeederTest` los 8 autos y `VehiculoControllerTest` el endpoint sin `apiSecret` en la respuesta.
2. `GET /api/v1/vehiculos` como `adminexample@gmail.com` devuelve `totalItems = 8` con `imagenUrl` en cada uno.
