# Construye las 9 imágenes de producción con la etiqueta `local`, que usa infrastructure/k8s/local.
# Se corre desde la raíz del repositorio:  .\infrastructure\k8s\local\construir-imagenes.ps1
# Con un nombre, construye solo esa:        .\infrastructure\k8s\local\construir-imagenes.ps1 pagos
param([string[]]$Solo)

# * Sin $ErrorActionPreference = 'Stop': en PowerShell 5.1 convertiría en error cada línea que docker escribe en
# * stderr (BuildKit escribe ahí todo su progreso). Los fallos se detectan con $LASTEXITCODE.

# nombre -> argumentos de docker build (contexto y Dockerfile como los espera cada proyecto)
$imagenes = [ordered]@{
  'gateway'        = @('projects/backend/gateway')
  'identidad'      = @('projects/backend/identidad')
  'escuela'        = @('projects/backend/escuela')
  'pagos'          = @('projects/backend/pagos')
  'academico'      = @('projects/backend/academico')
  'contenido'      = @('--target', 'prod', 'projects/backend/contenido')
  'asistente-ia'   = @('--target', 'prod', 'projects/backend/asistente-ia')
  'notificaciones' = @('-f', 'projects/backend/notificaciones/Dockerfile', '.')
  'web'            = @('-f', 'projects/frontend/Dockerfile', '.')
}

# * El Kubernetes de Docker Desktop en modo "kind" corre en su propio contenedor (desktop-control-plane) y NO ve
# * las imágenes de Docker: hay que importarlas a su containerd. Se pasa por un archivo y no por una tubería porque
# * PowerShell 5.1 corrompe los datos binarios que pasan por `|`.
$nodoKind = 'desktop-control-plane'
$hayKind = (docker inspect --format '{{.State.Running}}' $nodoKind 2>$null) -eq 'true'
$tar = Join-Path $env:TEMP 'drivehub-imagen.tar'

foreach ($nombre in $imagenes.Keys) {
  if ($Solo -and ($Solo -notcontains $nombre)) { continue }
  Write-Host "==> drivehub-${nombre}:local"
  docker build -t "drivehub-${nombre}:local" @($imagenes[$nombre])
  if ($LASTEXITCODE -ne 0) { throw "Falló la imagen de $nombre" }

  if ($hayKind) {
    docker save -o $tar "drivehub-${nombre}:local"
    docker cp $tar "${nodoKind}:/drivehub-imagen.tar"
    docker exec $nodoKind ctr -n k8s.io images import /drivehub-imagen.tar
    docker exec $nodoKind rm /drivehub-imagen.tar
    Remove-Item $tar
    if ($LASTEXITCODE -ne 0) { throw "No se pudo cargar $nombre en el nodo de Kubernetes" }
  }
}
