# ============================================
# MULTI-STAGE DOCKERFILE PARA SPRING BOOT
# ============================================

# ============================================
# STAGE 1: BUILD
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS build

# Instalar Maven
RUN apk add --no-cache maven

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de configuración de Maven
COPY pom.xml .

# Descargar dependencias (aprovecha caché de Docker)
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src ./src

# Compilar la aplicación (saltando tests para build más rápido)
RUN mvn clean package -DskipTests

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jre-alpine

# Metadata
LABEL maintainer="reservas-backend"
LABEL version="1.0.0"
LABEL description="Sistema de Reservas y Citas - Backend API"

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Establecer directorio de trabajo
WORKDIR /app

# Copiar JAR desde stage de build (nombre exacto para evitar copiar también el plain JAR)
COPY --from=build /app/target/citaclick-backend-1.0.0.jar app.jar

# Instalar curl para el healthcheck
RUN apk add --no-cache curl

# Cambiar ownership del JAR
RUN chown spring:spring app.jar

# Cambiar a usuario no-root
USER spring:spring

# Exponer puerto
EXPOSE 8080

# Configurar JVM para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# Health check (usa curl con actuator/health — más fiable que wget en Alpine JRE)
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
