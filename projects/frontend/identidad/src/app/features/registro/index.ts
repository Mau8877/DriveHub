/**
 * Lo único de la feature `registro` que otras features pueden importar.
 *
 * * `cuenta` y `perfil` reusan los límites y validadores de los campos para que las reglas de un mismo
 * * campo no se desalineen entre pantallas.
 */
export {
  APELLIDOS_MAX,
  edadMinima,
  EMAIL_MAX,
  fechaNoFutura,
  NOMBRES_MAX,
  obligatorioConTexto,
  PASSWORD_MAX,
  PASSWORD_MIN,
  TELEFONO_MAX,
} from './schemas/registro.reglas'
export { type CampoRegistro, mensajeDeError } from './schemas/registro.mensajes'
