#!/bin/sh
# Pre-commit de Contenido (Laravel): corre Pint si está instalado.
# Si no hay vendor/ (no hay PHP + composer install en esta máquina), avisa
# y no bloquea el commit: el job `contenido` del CI corre Pint igual.
cd projects/backend/contenido || exit 1
if [ -x ./vendor/bin/pint ]; then
  exec ./vendor/bin/pint --test
fi
echo "AVISO: Pint no está instalado (falta 'composer install' en projects/backend/contenido)."
echo "       Se saltea el lint de PHP en este commit; el CI lo valida igual."
