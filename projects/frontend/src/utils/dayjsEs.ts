import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/es'

// Configuración global de dayjs para toda la app -- un solo lugar donde se
// registra el plugin de fechas relativas y el locale en español (el
// proyecto no es multi-idioma, ver
// .claude/rules/frontend/FORMULARIOS_ZOD_TANSTACK_FRONTEND.md#mensajes-en-español).
// No vive en src/lib/ -- esa carpeta es exclusiva del output del CLI de
// shadcn (`cn()`, ver ARQUITECTURA_FRONTEND.md), esto es una utilidad
// propia del proyecto y va en utils/. Cualquier otro módulo que necesite
// `dayjs` ya configurado (formatRelativeDate, HeaderClock...) importa
// desde acá en vez de volver a llamar `dayjs.extend`/`dayjs.locale`.
dayjs.extend(relativeTime)
dayjs.locale('es')

export default dayjs
