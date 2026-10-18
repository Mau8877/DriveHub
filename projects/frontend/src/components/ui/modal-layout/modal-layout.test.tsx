import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ModalLayout } from './modal-layout'

describe('ModalLayout', () => {
  it('renderiza el título y el subtítulo', () => {
    render(
      <ModalLayout open onOpenChange={vi.fn()} title="Crear usuario" subtitle="Datos del nuevo usuario" formId="f1">
        <p>contenido</p>
      </ModalLayout>,
    )

    expect(screen.getByText('Crear usuario')).toBeInTheDocument()
    expect(screen.getByText('Datos del nuevo usuario')).toBeInTheDocument()
  })

  it('el botón de confirmar apunta al form recibido por formId', () => {
    render(
      <ModalLayout open onOpenChange={vi.fn()} title="Crear usuario" formId="usuario-form" confirmLabel="Crear">
        <p>contenido</p>
      </ModalLayout>,
    )

    expect(screen.getByRole('button', { name: 'Crear' })).toHaveAttribute('form', 'usuario-form')
  })

  it('al hacer click en cancelar, llama a onOpenChange(false) por default', async () => {
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    render(
      <ModalLayout open onOpenChange={onOpenChange} title="Crear usuario" formId="f1">
        <p>contenido</p>
      </ModalLayout>,
    )

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onOpenChange).toHaveBeenCalledWith(false)
  })

  it('usa onCancel en vez del default cuando se lo pasan', async () => {
    const onCancel = vi.fn()
    const onOpenChange = vi.fn()
    const user = userEvent.setup()
    render(
      <ModalLayout open onOpenChange={onOpenChange} onCancel={onCancel} title="Crear usuario" formId="f1">
        <p>contenido</p>
      </ModalLayout>,
    )

    await user.click(screen.getByRole('button', { name: 'Cancelar' }))

    expect(onCancel).toHaveBeenCalledTimes(1)
    expect(onOpenChange).not.toHaveBeenCalled()
  })

  it('con isSubmitting, deshabilita cancelar y confirmar', () => {
    render(
      <ModalLayout open onOpenChange={vi.fn()} title="Crear usuario" formId="f1" isSubmitting confirmLabel="Crear">
        <p>contenido</p>
      </ModalLayout>,
    )

    expect(screen.getByRole('button', { name: 'Cancelar' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Crear' })).toBeDisabled()
  })

  it('con isSubmitting, no cierra el modal al presionar Escape', () => {
    const onOpenChange = vi.fn()
    render(
      <ModalLayout open onOpenChange={onOpenChange} title="Crear usuario" formId="f1" isSubmitting>
        <p>contenido</p>
      </ModalLayout>,
    )

    fireEvent.keyDown(document, { key: 'Escape', code: 'Escape' })

    expect(onOpenChange).not.toHaveBeenCalled()
  })
})
