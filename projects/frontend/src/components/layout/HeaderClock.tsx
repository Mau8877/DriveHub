import { useEffect, useState } from 'react'

import dayjs from '@/utils/dayjsEs'

interface HeaderClockProps {
  className?: string
}

// * Cada 30s, no cada segundo -- el formato no muestra segundos, así que
// * actualizar más seguido solo gastaría ciclos sin cambiar lo que se ve.
const UPDATE_INTERVAL_MS = 30_000

function formatNow(): string {
  return dayjs().format('dddd, D [de] MMMM YYYY - HH:mm')
}

/** Fecha + hora actual en español (ej. "martes, 7 de marzo 2026 - 17:08"), se refresca sola. */
export function HeaderClock({ className }: HeaderClockProps) {
  const [now, setNow] = useState(formatNow)

  useEffect(() => {
    const id = window.setInterval(() => setNow(formatNow()), UPDATE_INTERVAL_MS)
    return () => window.clearInterval(id)
  }, [])

  return <span className={className}>{now}</span>
}
