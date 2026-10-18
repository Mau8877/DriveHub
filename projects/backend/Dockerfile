# ==========================================
# ETAPA 1: CONSTRUCCIÓN (Builder)
# ==========================================
# Utilizamos una imagen pesada que contiene Maven y el JDK completo
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 1. Copiamos ÚNICAMENTE el pom.xml primero
COPY pom.xml .

# 2. Descargamos las dependencias fuera de línea
# Esto aprovecha el caché de Docker. Si no cambias el pom.xml, 
# Docker no volverá a descargar todo internet en tu próximo build.
RUN mvn dependency:go-offline

# 3. Copiamos el código fuente
COPY src ./src

# 4. Compilamos y empaquetamos el .jar (saltamos los tests para agilizar el build local)
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: EJECUCIÓN (Runtime)
# ==========================================
# Utilizamos una imagen ultra ligera que SOLO contiene el JRE (sin Maven, sin compiladores)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 5. Práctica de Seguridad: Evitar ejecutar la app como ROOT
# Creamos un grupo y un usuario sin privilegios del sistema
RUN addgroup -S springgroup && adduser -S springuser -G springgroup
USER springuser:springgroup

# 6. Copiamos el .jar compilado desde la Etapa 1
# El flag --chown asegura que nuestro usuario sin privilegios sea el dueño del archivo
COPY --from=builder --chown=springuser:springgroup /build/target/*.jar app.jar

# 7. Exponemos el puerto estándar
EXPOSE 8080

# 8. Punto de entrada de la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]