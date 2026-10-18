---
globs: projects/frontend/{examenes,contenido,asistente-ia}/**/*
---

# Estado Global — Frontend

> **Alcance**: remotos React (`examenes`, `contenido`, `asistente-ia`).
> Cada remoto es una app independiente: las rutas `src/...` de esta rule
> son relativas a la carpeta del remoto. El shell y los remotos Angular
> tienen su propia rule: [ANGULAR_FRONTEND.md](ANGULAR_FRONTEND.md).

## `src/store/` no es "solo Zustand"

La carpeta `src/store/` es donde vive **todo lo relacionado a estado y
datos** del lado del cliente: los stores de Zustand descritos acá, y
también el cliente HTTP + `QueryClient` de TanStack Query (`createBaseApi.ts`,
`apiClient.ts`, `queryClient.ts`, `QueryProvider.tsx` — ver
[TANSTACK_QUERY_FRONTEND.md](TANSTACK_QUERY_FRONTEND.md#cliente-http-centralizado-ya-implementado)).
Es una decisión de **organización de carpetas**, no una relajación de la
separación conceptual de abajo — TanStack Query sigue siendo el único que
cachea datos de servidor, Zustand sigue siendo solo UI; ambos simplemente
viven bajo el mismo directorio de nivel superior.

## Separación estricta: Zustand vs TanStack Query

**Zustand maneja únicamente estado de UI.** Nunca cache de datos que vienen
del backend — eso es responsabilidad exclusiva de TanStack Query (ver
[TANSTACK_QUERY_FRONTEND.md](TANSTACK_QUERY_FRONTEND.md)). Ejemplos de lo que
sí va en Zustand:

- Modales abiertos/cerrados.
- Sidebar colapsado/expandido.
- Tema (claro/oscuro), si el proyecto lo soporta.
- Estado de un wizard multi-paso (paso actual, datos acumulados antes de
  enviar el formulario final).

Ejemplos de lo que **no** va en Zustand: la lista de usuarios traída del
backend, el resultado de un `GET`, cualquier dato cuya fuente de verdad es
el servidor. Si un componente necesita ese dato, lo pide con un hook de
`api/` (TanStack Query), no lo copia a un store.

La razón de separarlo así: TanStack Query ya resuelve cache, invalidación,
refetch y estados de carga/error para datos de servidor. Duplicar eso en
Zustand (guardar la respuesta de un fetch en un store manual) significa
mantener a mano una sincronización que la librería correcta ya hace sola, y
tarde o temprano el store y el servidor quedan desincronizados.

## Un store global + stores por feature

- **Estado de UI de toda la web** (sidebar, tema, layout): es del **shell**,
  no de un remoto. Un remoto no guarda estado que otro remoto necesite
  leer; si hace falta avisar algo, usa los eventos de `@drivehub/mf-contract`.
- **Store global del remoto** (`src/store/useUiStore.ts`, se crea con el
  primer caso que lo necesite) para estado de UI transversal a varias
  features **de ese remoto**. Un solo store, no uno por cada pedacito.
- **Stores de una sola feature** viven **dentro** de esa feature
  (`src/features/{feature}/store/use{Feature}Store.ts`), no en el store
  global — salvo que de verdad se necesite leer ese estado desde otra
  feature (no "por si acaso en el futuro").
- Antes de crear un store para algo, confirmar que no alcanza con
  `useState`/`useReducer` local al componente. Zustand es para estado que
  **cruza componentes que no son padre-hijo directo** — un wizard de 3 pasos
  contenido en un solo componente padre no necesita Zustand, un `useState`
  ahí alcanza.

## Estructura de un store

Store plano mientras el estado sea simple:

```ts
// src/store/useUiStore.ts
import { create } from 'zustand'

interface UiState {
  sidebarOpen: boolean
  toggleSidebar: () => void
}

export const useUiStore = create<UiState>((set) => ({
  sidebarOpen: true,
  toggleSidebar: () => set((s) => ({ sidebarOpen: !s.sidebarOpen })),
}))
```

Se pasa a **slice pattern** (dividir el store en varios `StateCreator` que se
combinan) solo cuando un store plano se vuelve difícil de navegar — en la
práctica, cuando junta 3 o más responsabilidades no relacionadas entre sí, o
supera ~150-200 líneas. No se arranca con slices "por prolijidad" en un
store que hoy tiene 2 campos.

## Nombres

- Hook del store: `use{Nombre}Store` (`useUiStore`, `useAuthUiStore`).
- Si un store necesita persistir entre sesiones (ej. tema elegido), se usa el
  middleware `persist` de Zustand sobre `localStorage` — no se implementa
  persistencia a mano con `localStorage.getItem`/`setItem` sueltos en el
  store.
