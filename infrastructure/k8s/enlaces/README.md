# Enlaces de Tailscale

Un `Service` por cada servicio que puede correr en **otra** nube, con el **mismo nombre y puerto** que su `Service`
real. Dentro del clúster se sigue llamando igual que siempre (`http://pagos:8083`), y el operador de Tailscale lleva
la llamada, cifrada, al clúster donde ese servicio corre de verdad. Ahí está publicado en la red privada como
`dh-<servicio>` (anotación `tailscale.com/expose` en el overlay de su nube).

> **Por qué `dh-` y no `drivehub-`:** el operador llama a cada proxy que crea sin nombre explícito
> `<namespace>-<servicio>`, y el namespace es `drivehub`. Si un servicio se publicara como `drivehub-pagos`, el
> proxy de *enlace* hacia `pagos` de otra nube tomaría ese mismo nombre, el servicio real quedaría como
> `drivehub-pagos-1` y el enlace terminaría apuntándose a sí mismo (pasó en la primera prueba).

- Cada nube incluye solo los enlaces de lo que **no** corre en ella (ver `nubes/{nube}/kustomization.yaml`). Cada enlace es una carpeta (`enlaces/pagos/`) porque Kustomize solo acepta carpetas fuera del overlay.
- `tailscale.com/tailnet-fqdn` lleva el nombre de la red de Tailscale del proyecto (`tail9b7a35.ts.net`).
- El operador reemplaza `externalName` por su propio proxy: el valor del archivo no se usa.
