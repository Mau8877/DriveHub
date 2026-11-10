import { createFileRoute } from '@tanstack/react-router'

import { ReportesScreen } from '@/features/reportes'

export const Route = createFileRoute('/reportes')({
  component: ReportesScreen,
})
