/**
 * Lo que el shell expone a todos los micro-frontends.
 */
export interface ContextoShell {
  /** URL base de la API (el API Gateway), ej. http://localhost:8080/api/v1 */
  apiBaseUrl: string
  /** Access token vigente (en memoria del shell), o null si no hay sesión. */
  obtenerToken(): string | null
  /** Navega a una ruta de primer nivel del shell (ej. '/escuela'). */
  navegar(ruta: string): void
}

/**
 * Props que el shell le pasa a un remoto React al montarlo.
 */
export interface MountProps extends ContextoShell {
  /** Ruta base del remoto dentro del shell (ej. '/examenes'): sus subrutas cuelgan de acá. */
  basePath: string
}

/**
 * Lo que expone cada remoto React (módulo './mount' de su Module Federation).
 */
export interface RemotoReact {
  mount(elemento: HTMLElement, props: MountProps): void
  unmount(elemento: HTMLElement): void
}

export declare function registrarContextoShell(contexto: ContextoShell): void
export declare function obtenerContextoShell(): ContextoShell | null
export declare function emitirEvento<T = unknown>(nombre: string, detalle?: T): void
export declare function escucharEvento<T = unknown>(
  nombre: string,
  manejador: (detalle: T) => void
): () => void
