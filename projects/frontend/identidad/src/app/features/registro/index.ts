/**
 * Lo único de la feature `registro` que otras features pueden importar.
 *
 * * `cuenta` reusa los límites de correo y contraseña para que las reglas de un mismo
 * * campo no se desalineen entre pantallas.
 */
export { EMAIL_MAX, PASSWORD_MAX, PASSWORD_MIN } from './schemas/registro.reglas'
