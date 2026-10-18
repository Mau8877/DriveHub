import { QueryClient } from '@tanstack/react-query'

// * Defaults conservadores: todavía no hay ninguna query real que exija algo
// * distinto. Se ajustan por query puntual (`useQuery({ staleTime: ... })`)
// * cuando un caso concreto lo necesite, no se sube la config global sin motivo.
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})
