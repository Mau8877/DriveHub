# Clúster de AKS de DriveHub (Azure): pagos y academico, con su Postgres, Mongo y MinIO.
# Se corre con la sesión de `az login` iniciada:  .\infrastructure\k8s\nubes\azure\crear-cluster.ps1
# Borrar todo (clúster, discos e IP):             az group delete -n drivehub --yes
# Apagar sin borrar (no se cobran los nodos):      az aks stop -g drivehub -n drivehub   (y az aks start para volver)

$grupo = 'drivehub'
$region = 'eastus'

az group create --name $grupo --location $region --tags proyecto=drivehub
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el grupo de recursos' }

# * 2 x Standard_D2s_v4 (2 vCPU, 8 GB) = 4 vCPU: justo la cuota de una suscripción de prueba en eastus.
# * No es D2s_v5 porque una suscripción de prueba no puede usarlo en eastus: Azure responde "The VM size of ... is not
# * allowed in your subscription" con la lista de los tamaños permitidos.
# * Disco del sistema de 30 GB (por defecto son 128 GB y se cobran aunque no se usen).
# * Nivel free: el plano de control no se cobra (sin SLA).
az aks create `
  --resource-group $grupo `
  --name drivehub `
  --location $region `
  --tier free `
  --node-count 2 `
  --node-vm-size Standard_D2s_v4 `
  --node-osdisk-size 30 `
  --network-plugin azure `
  --network-plugin-mode overlay `
  --generate-ssh-keys `
  --tags proyecto=drivehub
if ($LASTEXITCODE -ne 0) { throw 'No se pudo crear el clúster' }

# Agrega el acceso al kubeconfig con el contexto `azure-drivehub`, sin dejarlo como contexto por defecto.
$actual = kubectl config current-context
az aks get-credentials --resource-group $grupo --name drivehub --context azure-drivehub --overwrite-existing
if ($actual) { kubectl config use-context $actual | Out-Null }
