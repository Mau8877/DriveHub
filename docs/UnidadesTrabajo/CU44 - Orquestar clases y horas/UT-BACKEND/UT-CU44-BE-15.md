# UT-CU44-BE-15 — Académico: el punto de extensión para CU15, demostrado

| Campo | Valor |
|-------|-------|
| **Código** | `UT-CU44-BE-15` |
| **Nombre** | Académico: el punto de extensión para CU15, demostrado |
| **Caso de uso** | CU44-BE |
| **Microservicio** | `academico` |
| **Depende de** | `UT-CU44-BE-11` |

## Antes de implementar

La decisión 15 del [README](./README.md#decisiones-de-diseño) y su «Contrato para CU15». Leer `OrquestadorClases`, `ManejadorDeOperacion`, `ManejadorDeReserva` y el job (UT 09 a 11). [TESTING_BACKEND.md](../../../../.claude/rules/backend/TESTING_BACKEND.md).

## Solicitud de negocio

Que cuando Leonel haga cancelar y reprogramar, solo tenga que agregar sus estados y un manejador, y que el sistema que retoma las operaciones a medias ya los trate sin tocar nada más. Y que eso no sea una promesa escrita, sino algo que **un test demuestra**.

## Objetivo

Un test de integración prueba que un manejador nuevo, registrado como bean, queda cubierto por el job, el contador de intentos y la revisión manual **sin editar `OrquestadorClases` ni el job**, y que la guía para CU15 quedó escrita en el código.

## Alcance

- Test `OrquestadorExtensionTest` (`@SpringBootTest` con MongoDB real, patrón `assumeTrue`) que registra, solo dentro del test, un `ManejadorDeOperacionDePrueba` para un estado que **ningún manejador real** atiende hoy (se usa un valor existente del enum `EstadoReserva` solo como pretexto: representa «el estado nuevo de CU15»). Comprueba:
  1. Sin el manejador de prueba registrado, una reserva en ese estado **no** se toca (el orquestador solo mira los estados que declaran los manejadores).
  2. Con el manejador registrado, el job la encuentra, el orquestador la entrega a **ese** manejador y a ningún otro, y el resultado queda en el estado final que el manejador decide.
  3. Si el manejador devuelve `PAGOS_NO_RESPONDE`, no suma intentos y se reintenta en la siguiente pasada.
  4. Si el manejador falla por una causa propia `maximo-intentos` veces, la reserva se marca `requiereRevision` y deja de procesarse, igual que las reservas normales.
  5. El manejador recibe un `ClienteDePagos` (el de servicio, en el job) y puede llamar a `OrquestadorClases.deshacerReserva` con él.
  6. Las reservas `PENDIENTE_PAGO` siguen resolviéndose con `ManejadorDeReserva` mientras el de prueba está registrado.
- Comentarios para quien implemente CU15 (formato Better Comments de [ESTANDARES_CODIGO.md](../../../../.claude/rules/ESTANDARES_CODIGO.md)) en el Javadoc de `ManejadorDeOperacion`: qué hay que hacer para enchufar una operación nueva (declarar los estados, escribir `resolver`, registrar el bean), con un ejemplo mínimo, y un `* ! No volver a escribir la devolución de la hora: usar deshacerReserva y el ClienteDePagos recibido.` Una referencia a la [guía del README](./README.md#contrato-para-cu15-cómo-se-enchufa-guía-para-leonel).
- El test sirve de **plantilla**: su nombre y su estructura se mencionan en la guía, para que CU15 agregue uno igual con su manejador real.

Archivos esperados (crear/tocar):

- `projects/backend/academico/src/test/java/com/drivehub/academico/features/reservas/service/OrquestadorExtensionTest.java` (crear)
- `projects/backend/academico/src/main/java/com/drivehub/academico/features/reservas/service/ManejadorDeOperacion.java` (solo el Javadoc)

## Fuera de alcance (no tocar)

- Los manejadores reales de CU15 (UT 17 y 18) y sus estados.
- Cambios de comportamiento en `OrquestadorClases`, el job o los repositorios: si el test obliga a cambiarlos, la UT que corresponde (08 a 11) estaba incompleta y se corrige **allí**.
- Producción: el manejador de prueba vive solo en las pruebas.

## Resultado esperado

Existe una prueba que demuestra que agregar una operación nueva es registrar un manejador, y la guía en el código lo explica.

## Validación

1. `./mvnw test` en `academico` pasa (los de MongoDB real quedan `SKIPPED` sin Docker; verificarlos contra el Mongo del compose).
2. `./mvnw checkstyle:check` sin errores.
3. `git diff` de `OrquestadorClases.java` y `OrquestadorClasesJob.java` está vacío en esta UT.
