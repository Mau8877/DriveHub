# Unidades de trabajo — CU02-MOB

**Caso de uso:** Inicio de sesión de la app móvil para Estudiante e Instructor, con sesión guardada de forma segura, renovación automática y redirect global del router.
**Aplicación:** `projects/mobile` (Flutter) — ver [MICROSERVICIOS.md](../../../../.claude/rules/MICROSERVICIOS.md)
**Fuente CU:** [`CU02 - Iniciar sesión`](../../../CasosDeUso/CU02%20-%20Iniciar%20sesi%C3%B3n.md)
**Recurso HTTP / contrato:** `POST /api/v1/auth/login` y `POST /api/v1/auth/refresh` con `dispositivo: 'MOVIL'` (ver [el contrato de backend](../UT-BACKEND/README.md#contrato-de-datos--recurso-http))

## Antes de implementar cualquier UT

Leer en [`.claude/rules/`](../../../../.claude/rules/): [ARQUITECTURA_MOBILE.md](../../../../.claude/rules/ARQUITECTURA_MOBILE.md), [CONSUMO_API_MOBILE.md](../../../../.claude/rules/mobile/CONSUMO_API_MOBILE.md) (incluye la convención de DTOs), [ESTADO_GLOBAL_MOBILE.md](../../../../.claude/rules/mobile/ESTADO_GLOBAL_MOBILE.md), [NAVEGACION_MOBILE.md](../../../../.claude/rules/mobile/NAVEGACION_MOBILE.md), [FORMULARIOS_MOBILE.md](../../../../.claude/rules/mobile/FORMULARIOS_MOBILE.md), [TESTING_MOBILE.md](../../../../.claude/rules/mobile/TESTING_MOBILE.md) y [SEGURIDAD_AUTH_BACKEND.md](../../../../.claude/rules/backend/SEGURIDAD_AUTH_BACKEND.md). Estas fichas **no** sustituyen esas reglas: si hay conflicto, ganan las rules del proyecto.

## Decisiones de diseño

1. **Tokens en el teléfono:** access y refresh token (este último viaja en el cuerpo, no hay cookies) se guardan solo en `flutter_secure_storage`. Se recuperan al abrir la app para entrar sin pedir la contraseña.
2. **Quién entra:** solo Estudiante e Instructor. La app no valida roles por su cuenta: si un administrador intenta entrar, el backend responde `ERR_IDE_08` y la pantalla muestra el mensaje de usar la web.
3. **Estado de sesión único:** `sesionProvider` (Riverpod) es el único lugar que sabe si hay sesión; el router y el interceptor de Dio lo leen. `core/network` recibe dos callbacks (token y renovar) para no importar `features/`.
4. **Renovación:** proactiva al 80 % de la vida del token y reactiva ante un 401 (una vez, con una sola renovación en curso). Cada renovación rota el refresh token, así que se guarda el nuevo.
5. **Redirect global:** mientras se recupera la sesión, `/cargando`; sin sesión, solo `/login` y `/register`; con sesión, `/inicio` (`UsuarioShell`). La pantalla de login no navega a mano: el redirect lo hace al cambiar el estado.
6. **Compatibilidad con CU01:** el registro sigue volviendo a `/login?registrado=1` con el aviso de cuenta creada.

## Orden de ejecución

| Código | Nombre | Depende de |
|--------|--------|------------|
| [UT-CU02-MOB-01](./UT-CU02-MOB-01.md) | Modelos y `AuthService` | — |
| [UT-CU02-MOB-02](./UT-CU02-MOB-02.md) | Almacenamiento seguro de la sesión | 01 |
| [UT-CU02-MOB-03](./UT-CU02-MOB-03.md) | `sesionProvider` | 01, 02 |
| [UT-CU02-MOB-04](./UT-CU02-MOB-04.md) | Interceptor de sesión en `ApiClient` | 03 |
| [UT-CU02-MOB-05](./UT-CU02-MOB-05.md) | Router con redirect global y pantalla de inicio | 03 |
| [UT-CU02-MOB-06](./UT-CU02-MOB-06.md) | Pantalla de inicio de sesión | 03, 05 |

**Cadena:** `01 → 02 → 03 → 04, 05 → 06`. Necesita el backend de CU02 solo para la validación manual de punta a punta.

## Cobertura del caso de uso

| Paso / flujo del CU | Descripción | UTs |
|---------------------|-------------|-----|
| CA01 | Login móvil con la sesión guardada | 01, 02, 03, 06 |
| CA03 | Sin sesión lleva al login; con sesión, al inicio | 05 |
| CA04, CA06, CA08, CA09, CA10 | Mensajes de cada rechazo del backend | 06 |
| CA05 | Campos obligatorios sin enviar | 06 |
| CA12 | Renovación en segundo plano | 03 |
| CA13 | Rechazo por sesión vencida: renovar una vez y repetir | 04 |
| CA14 | Abrir la app con sesión guardada la recupera | 02, 03, 05 |
| CA17 | Renovación rechazada lleva al login con el motivo | 03, 06 |
| CA18 | Con sesión vigente, `/login` lleva al inicio | 05 |

## Follow-ups (no se implementan en estas UT)

- Cerrar sesión (CU03), recuperar contraseña (CU04) y las pantallas internas del inicio.
- Biometría o "recordarme".
- Notificaciones push y el registro del dispositivo.

## Qué NO incluyen estas UT

- Registro de estudiantes (CU01), salvo reemplazar el login provisional al que vuelve.
- `AdminShell`: la app móvil no es para administradores.
- Cambios en la paleta o el tema.

## Commits sugeridos

Los cambios en `projects/mobile` van en commits `[MOBILE]` (puede ser uno solo para todo CU02-MOB).
