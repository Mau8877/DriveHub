import { Directive, ElementRef, forwardRef, inject } from '@angular/core'
import { type ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms'

/** Lo que tienen en común <dh-select> y <dh-fecha> de @drivehub/ui. */
interface ElementoConValor extends HTMLElement {
  valor: string
  deshabilitado: boolean
}

/**
 * Conecta <dh-select> y <dh-fecha> de @drivehub/ui con los formularios reactivos de Angular
 * (`formControlName` / `formControl`): el valor del control es el `valor` del componente, un cambio
 * (`dh-cambio`) actualiza el control y salir del campo (`dh-blur`) lo marca como tocado.
 *
 * * Los dos componentes comparten API (`valor`, `deshabilitado`, `dh-cambio`, `dh-blur`), así que una
 * * sola directiva sirve para ambos.
 */
@Directive({
  selector:
    'dh-select[formControlName], dh-select[formControl], dh-fecha[formControlName], dh-fecha[formControl]',
  providers: [{ provide: NG_VALUE_ACCESSOR, useExisting: forwardRef(() => DhValor), multi: true }],
  host: {
    '(dh-cambio)': 'alCambiar($event)',
    '(dh-blur)': 'alTocar()',
  },
})
export class DhValor implements ControlValueAccessor {
  private readonly elemento = inject<ElementRef<ElementoConValor>>(ElementRef).nativeElement

  private cambio: (valor: string) => void = () => undefined
  private tocado: () => void = () => undefined

  writeValue(valor: string | null): void {
    this.elemento.valor = valor ?? ''
  }

  registerOnChange(fn: (valor: string) => void): void {
    this.cambio = fn
  }

  registerOnTouched(fn: () => void): void {
    this.tocado = fn
  }

  setDisabledState(deshabilitado: boolean): void {
    this.elemento.deshabilitado = deshabilitado
  }

  protected alCambiar(evento: Event): void {
    this.cambio((evento as CustomEvent<{ valor: string }>).detail.valor)
  }

  protected alTocar(): void {
    this.tocado()
  }
}
