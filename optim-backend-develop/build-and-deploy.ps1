# build-and-deploy.ps1

param(
    [Parameter(Mandatory=$true)]
    [string]$GitBranch,
    
    [Parameter(Mandatory=$true)]
    [string]$DockerUsername
)

# Obtener la fecha y hora actual en formato YYYYMMDDHHMM
$timestamp = Get-Date -Format "yyyyMMddHHmm"
$dockerTag = "backend$timestamp"

Write-Host "Iniciando proceso con rama: $GitBranch, usuario Docker: $DockerUsername, tag: $dockerTag" -ForegroundColor Green

# Ejecutar comandos Git
Write-Host "Ejecutando git fetch..." -ForegroundColor Cyan
git fetch

Write-Host "Cambiando a la rama $GitBranch..." -ForegroundColor Cyan
git checkout $GitBranch

# Ejecutar Maven
Write-Host "Ejecutando mvn clean install..." -ForegroundColor Cyan
mvn clean install

# Construir imagen Docker
Write-Host "Construyendo imagen Docker..." -ForegroundColor Cyan
$imageTag = "$DockerUsername/optim:$dockerTag"
docker build -t $imageTag .

# Listar imágenes Docker
Write-Host "Listando imágenes Docker..." -ForegroundColor Cyan
docker images

# Subir imagen a Docker Hub
Write-Host "Subiendo imagen a Docker Hub: $imageTag..." -ForegroundColor Cyan
docker push $imageTag

Write-Host "Proceso completado exitosamente!" -ForegroundColor Green