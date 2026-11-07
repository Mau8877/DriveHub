// Manda cada mensaje a la persona por la API de WAHA (WhatsApp). Cada item trae { mensaje: { para, texto } }.
//
// * Un fallo al enviar se ignora a propósito: no hay nadie a quien avisarle, y un mensaje que no sale no debe
//   tumbar la ejecución. Queda en los registros del contenedor de WAHA.
// * WhatsApp aguanta mensajes largos, pero lo más largo de 1500 caracteres se parte en varios, cortando en un salto
//   de línea. Entre mensaje y mensaje hay una pausa corta: mandar ráfagas sin pausa es lo que más parece un bot.
const base = String($env.WAHA_URL || '').replace(/\/$/, '');
const sesion = $env.WAHA_SESSION || 'default';
const LIMITE = 1500;

if (!base) return [];

function partir(cuerpo) {
  const partes = [];
  let resto = cuerpo;
  while (resto.length > LIMITE) {
    let corte = resto.lastIndexOf('\n', LIMITE);
    if (corte < LIMITE / 2) corte = LIMITE;
    partes.push(resto.slice(0, corte).trimEnd());
    resto = resto.slice(corte).trimStart();
  }
  if (resto) partes.push(resto);
  return partes;
}

const pausa = (ms) => new Promise((resolver) => setTimeout(resolver, ms));

for (const item of $input.all()) {
  const { para, texto: cuerpo } = item.json.mensaje || {};
  if (!para || !cuerpo) continue;
  for (const parte of partir(cuerpo)) {
    try {
      await http({
        method: 'POST',
        url: `${base}/api/sendText`,
        headers: { 'X-Api-Key': $env.WAHA_API_KEY || '' },
        body: { session: sesion, chatId: `${para}@c.us`, text: parte },
        timeout: 15000,
      });
    } catch (e) {
      // * ver arriba
    }
    await pausa(800);
  }
}
return [];
