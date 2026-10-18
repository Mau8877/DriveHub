# Scripts

Scripts de automatización del proyecto.

- `hooks/` — scripts que llama `lint-staged` desde el pre-commit (ver `lint-staged` en el `package.json` raíz). `lint-php.sh` y `lint-python.sh` saltean con un aviso si Pint/ruff no están instalados; el CI los valida igual.
