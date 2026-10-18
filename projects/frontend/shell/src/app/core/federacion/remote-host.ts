import {
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  inject,
  input,
  signal,
  viewChild,
} from '@angular/core'
import { obtenerContextoShell, type RemotoReact } from '@drivehub/mf-contract'

import { cargarRemotoReact } from './remotos-react'

/**
 * Componente genérico que monta un remoto React dentro del shell.
 *
 * * Se usa como componente de ruta: el nombre del remoto y su ruta base
 * * vienen por `data` de la ruta (withComponentInputBinding los pasa como
 * * inputs). Al entrar llama mount(elemento, props) y al salir unmount.
 * * El remoto maneja sus propias subrutas debajo de basePath: el shell solo
 * * es dueño del primer nivel (/examenes/*, /contenido/*, ...).
 */
@Component({
  selector: 'app-remote-host',
  template: `
    @if (error()) {
      <p class="text-destructive p-6">No se pudo cargar este módulo. Intentá recargar la página.</p>
    }
    <div #contenedor></div>
  `,
})
export class RemoteHost implements OnInit, OnDestroy {
  readonly remoto = input.required<string>()
  readonly basePath = input.required<string>()

  protected readonly error = signal(false)
  private readonly contenedor = viewChild.required<ElementRef<HTMLElement>>('contenedor')
  private readonly host = inject(ElementRef)
  private montado: RemotoReact | null = null

  async ngOnInit(): Promise<void> {
    try {
      const remoto = await cargarRemotoReact(this.remoto())
      const contexto = obtenerContextoShell()
      if (!contexto || !this.host.nativeElement.isConnected) return
      remoto.mount(this.contenedor().nativeElement, { ...contexto, basePath: this.basePath() })
      this.montado = remoto
    } catch (e) {
      console.error(e)
      this.error.set(true)
    }
  }

  ngOnDestroy(): void {
    this.montado?.unmount(this.contenedor().nativeElement)
  }
}
