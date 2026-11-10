import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Dictado por voz con el reconocimiento del navegador (Web Speech API, en Chrome y Edge), en español.
 *
 * * Solo convierte la voz en texto: el texto se puede corregir antes de interpretarlo. Donde el
 * * navegador no lo tiene, `soportado` es falso y el pedido se escribe.
 * * El audio lo procesa el navegador (en Chrome, el servicio de voz de Google); la app no lo guarda.
 */
interface ResultadoVoz {
  0: { transcript: string }
  isFinal: boolean
}

interface EventoVoz {
  resultIndex: number
  results: ArrayLike<ResultadoVoz>
}

export interface ReconocimientoVoz {
  lang: string
  continuous: boolean
  interimResults: boolean
  onresult: ((evento: EventoVoz) => void) | null
  onend: (() => void) | null
  onerror: ((evento: { error: string }) => void) | null
  start: () => void
  stop: () => void
}

type FabricaReconocimiento = new () => ReconocimientoVoz

function fabricaDelNavegador(): FabricaReconocimiento | undefined {
  const w = globalThis as unknown as {
    SpeechRecognition?: FabricaReconocimiento
    webkitSpeechRecognition?: FabricaReconocimiento
  }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition
}

export function useDictado(alTerminarFrase: (texto: string) => void, fabrica = fabricaDelNavegador()) {
  const [escuchando, setEscuchando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const reconocimiento = useRef<ReconocimientoVoz | null>(null)
  const callback = useRef(alTerminarFrase)
  useEffect(() => {
    callback.current = alTerminarFrase
  })

  const detener = useCallback(() => {
    reconocimiento.current?.stop()
  }, [])

  const iniciar = useCallback(() => {
    if (!fabrica) return
    setError(null)
    const r = new fabrica()
    r.lang = 'es-BO'
    r.continuous = false
    r.interimResults = false
    r.onresult = (evento) => {
      for (let i = evento.resultIndex; i < evento.results.length; i++) {
        const resultado = evento.results[i]
        if (resultado.isFinal) callback.current(resultado[0].transcript.trim())
      }
    }
    r.onerror = (evento) => {
      setError(
        evento.error === 'not-allowed'
          ? 'El navegador no tiene permiso para usar el micrófono.'
          : 'No te escuché bien. Inténtalo de nuevo o escribe el pedido.',
      )
    }
    r.onend = () => setEscuchando(false)
    reconocimiento.current = r
    setEscuchando(true)
    r.start()
  }, [fabrica])

  useEffect(() => () => reconocimiento.current?.stop(), [])

  return { soportado: !!fabrica, escuchando, error, iniciar, detener }
}
