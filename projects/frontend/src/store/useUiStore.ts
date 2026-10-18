import { create } from 'zustand'
import { persist } from 'zustand/middleware'

export type Theme = 'light' | 'dark'

interface UiState {
  sidebarCollapsed: boolean
  toggleSidebarCollapsed: () => void
  mobileSidebarOpen: boolean
  setMobileSidebarOpen: (open: boolean) => void
  theme: Theme
  toggleTheme: () => void
}

export const useUiStore = create<UiState>()(
  persist(
    (set) => ({
      sidebarCollapsed: false,
      toggleSidebarCollapsed: () =>
        set((s) => ({ sidebarCollapsed: !s.sidebarCollapsed })),
      mobileSidebarOpen: false,
      setMobileSidebarOpen: (open) => set({ mobileSidebarOpen: open }),
      theme: 'light',
      toggleTheme: () =>
        set((s) => ({ theme: s.theme === 'light' ? 'dark' : 'light' })),
    }),
    {
      name: 'ui-storage',
      // * Solo el tema se persiste entre sesiones -- sidebarCollapsed y
      // * mobileSidebarOpen son estado transitorio de la sesión actual.
      partialize: (s) => ({ theme: s.theme }),
    }
  )
)
