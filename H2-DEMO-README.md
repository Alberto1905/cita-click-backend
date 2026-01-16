# Base de Datos H2 para Demo - Cita Click

## ⚠️ TEMPORAL - Solo para Capturas de Pantalla

Esta integración de H2 es **temporal** y se usa únicamente para generar capturas de pantalla con datos realistas para la landing page.

## Configuración

### 1. Activar el perfil H2

Para levantar el proyecto con H2 en lugar de PostgreSQL:

```bash
# Opción 1: Variable de entorno
export SPRING_PROFILES_ACTIVE=h2
mvn spring-boot:run

# Opción 2: Argumento de línea de comandos
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=h2

# Opción 3: IDE (IntelliJ/Eclipse)
# Configurar en Run Configuration: Active Profiles = h2
```

### 2. Credenciales de Acceso

#### Aplicación
- **Email**: demo@citaclick.mx
- **Password**: Demo1234!

#### Consola H2 (para inspección)
- **URL**: http://localhost:8080/h2-console
- **JDBC URL**: jdbc:h2:mem:citaclick
- **Username**: sa
- **Password**: (vacío)

## Datos Incluidos

La base de datos H2 se inicializa automáticamente con:

### Negocio
- **Nombre**: Estética Premium
- **Plan**: PROFESIONAL
- **Estado**: ACTIVO
- **Ubicación**: Ciudad de México

### Usuarios
- 1 Owner/Admin (demo@citaclick.mx)

### Servicios (15)
1. Corte de Cabello Dama - $350
2. Corte de Cabello Caballero - $250
3. Tinte Completo - $850
4. Mechas/Rayitos - $1,200
5. Peinado - $450
6. Maquillaje - $550
7. Manicure - $250
8. Pedicure - $300
9. Uñas Acrílicas - $500
10. Uñas de Gel - $550
11. Tratamiento Capilar - $650
12. Keratina - $1,500
13. Depilación Facial - $200
14. Extensiones - $2,000
15. Masaje Facial - $400

### Clientes
- **Total**: 142 clientes
- **Distribución**: 71 mujeres + 71 hombres
- **Estado**: 80% activos, 20% inactivos
- **Registros**: Entre septiembre 2025 y enero 2026

### Citas (30)
- **Pasadas**: 6 citas (completadas y canceladas)
- **Hoy (16 enero)**: 6 citas (en diferentes estados)
- **Futuras**: 18 citas (próximos 7 días)

### Horarios
- **Días**: Lunes a Sábado
- **Horario**: 9:00 AM - 7:00 PM

## Uso para Capturas

### Vistas Recomendadas para Capturar

1. **Dashboard**
   - Métricas generales con 142 clientes
   - Gráficas de citas
   - Ingresos del mes

2. **Lista de Clientes**
   - Vista completa con 142 registros
   - Filtros y búsqueda
   - Estados activos/inactivos

3. **Calendario de Citas**
   - Vista semanal con citas distribuidas
   - Diferentes colores por servicio
   - Estados: confirmada, pendiente, completada

4. **Lista de Servicios**
   - 15 servicios con precios variados
   - Colores distintivos
   - Duraciones diferentes

5. **Vista de Horarios**
   - Configuración Lun-Sáb
   - Horario 9:00-19:00

6. **Reportes**
   - Servicios más solicitados
   - Clientes frecuentes
   - Ingresos por período

## Reiniciar Datos

Los datos se regeneran automáticamente cada vez que inicias la aplicación con el perfil H2 (debido a `ddl-auto: create-drop`).

```bash
# Simplemente reinicia la aplicación
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=h2
```

## Volver a PostgreSQL

Para volver a usar PostgreSQL:

```bash
# Opción 1: Sin perfil (default)
mvn spring-boot:run

# Opción 2: Perfil explícito
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

## Archivos Relacionados

- `pom.xml` - Dependencia H2 agregada
- `src/main/resources/application-h2.yml` - Configuración H2
- `src/main/resources/data-h2.sql` - Datos de prueba (142 clientes)
- `src/main/resources/schema-h2.sql` - Schema (vacío, Hibernate lo crea)

## ⚠️ IMPORTANTE

1. **NO** usar H2 en producción
2. **NO** commit de credenciales reales
3. **ELIMINAR** H2 después de tomar las capturas
4. Los datos son **volátiles** (se pierden al cerrar la app)

## Eliminación Post-Capturas

Cuando termines de tomar las capturas:

```bash
# 1. Eliminar archivos H2
rm src/main/resources/application-h2.yml
rm src/main/resources/data-h2.sql
rm src/main/resources/schema-h2.sql
rm H2-DEMO-README.md

# 2. Eliminar dependencia de pom.xml
# Buscar y eliminar:
# <!-- H2 Database (Temporal para demo con datos de prueba) -->
# <dependency>
#     <groupId>com.h2database</groupId>
#     <artifactId>h2</artifactId>
#     <scope>runtime</scope>
# </dependency>

# 3. Commit cambios
git add .
git commit -m "chore: remove H2 temporary integration after screenshots"
git push
```
