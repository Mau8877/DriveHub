export const reportesKeys = {
  all: ['reportes'] as const,
  catalogo: () => [...reportesKeys.all, 'catalogo'] as const,
}
