import '@drivehub/ui'

import { useEffect, useRef } from 'react'

import type { DhFecha, DhSelect, OpcionSelect } from '@drivehub/ui'

/**
 * <dh-select> y <dh-fecha> de @drivehub/ui desde React. Las propiedades se asignan en el elemento y
 * los eventos `dh-cambio` se escuchan con addEventListener: así no dependen de cómo React traduce los
 * nombres de eventos con guion.
 */
interface SelectProps {
  etiqueta: string
  opciones: OpcionSelect[]
  valor?: string | null
  valores?: string[]
  multiple?: boolean
  placeholder?: string
  sinVacio?: boolean
  onCambio: (valor: string | string[]) => void
}

export function SelectDh({ etiqueta, opciones, valor, valores, multiple, placeholder, sinVacio, onCambio }: SelectProps) {
  const ref = useRef<DhSelect>(null)
  const cambio = useRef(onCambio)
  useEffect(() => {
    cambio.current = onCambio
  })

  useEffect(() => {
    const el = ref.current
    if (!el) return
    el.etiqueta = etiqueta
    el.opciones = opciones
    el.multiple = !!multiple
    el.placeholder = placeholder ?? ''
    el.sinVacio = !!sinVacio
    if (multiple) el.valores = valores ?? []
    else el.valor = valor ?? ''
  }, [etiqueta, opciones, valor, valores, multiple, placeholder, sinVacio])

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const escuchar = (e: Event) => {
      const detalle = (e as CustomEvent<{ valor?: string; valores?: string[] }>).detail
      cambio.current(multiple ? (detalle.valores ?? []) : (detalle.valor ?? ''))
    }
    el.addEventListener('dh-cambio', escuchar)
    return () => el.removeEventListener('dh-cambio', escuchar)
  }, [multiple])

  return <dh-select ref={ref} />
}

interface FechaProps {
  etiqueta: string
  valor?: string | null
  min?: string
  invalido?: boolean
  onCambio: (valor: string) => void
}

export function FechaDh({ etiqueta, valor, min, invalido, onCambio }: FechaProps) {
  const ref = useRef<DhFecha>(null)
  const cambio = useRef(onCambio)
  useEffect(() => {
    cambio.current = onCambio
  })

  useEffect(() => {
    const el = ref.current
    if (!el) return
    el.etiqueta = etiqueta
    el.valor = valor ?? ''
    el.min = min ?? ''
    el.invalido = !!invalido
  }, [etiqueta, valor, min, invalido])

  useEffect(() => {
    const el = ref.current
    if (!el) return
    const escuchar = (e: Event) => cambio.current((e as CustomEvent<{ valor: string }>).detail.valor)
    el.addEventListener('dh-cambio', escuchar)
    return () => el.removeEventListener('dh-cambio', escuchar)
  }, [])

  return <dh-fecha ref={ref} />
}
