import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      globals: globals.browser,
    },
  },
  {
    // Los archivos de src/routes/ exportan `Route` (config de TanStack
    // Router) además del componente de la página -- es el patrón esperado
    // del file-based routing, no un archivo mal armado. React Fast Refresh
    // igual funciona bien acá porque TanStack Router maneja su propio HMR.
    files: ['src/routes/**/*.tsx'],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
  {
    // src/components/ui/ es output del CLI de shadcn (ver
    // .claude/rules/frontend/TAILWIND_STYLES_FRONTEND.md -- no se edita a
    // mano). Varios de esos componentes exportan también un helper de
    // variantes (ej. `buttonVariants`) junto al componente, a propósito.
    files: ['src/components/ui/**/*.tsx'],
    rules: {
      'react-refresh/only-export-components': 'off',
    },
  },
])
