import '@testing-library/jest-dom'

// * jsdom no trae matchMedia y los devtools de TanStack Query (solo en desarrollo) lo piden cuando
// * la prueba dura lo suficiente para que carguen.
if (!window.matchMedia) {
  window.matchMedia = (consulta: string) =>
    ({
      matches: false,
      media: consulta,
      onchange: null,
      addEventListener: () => undefined,
      removeEventListener: () => undefined,
      addListener: () => undefined,
      removeListener: () => undefined,
      dispatchEvent: () => false,
    }) as MediaQueryList
}
