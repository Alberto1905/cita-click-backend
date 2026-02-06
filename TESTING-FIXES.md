# 🧪 Guía para Arreglar Tests - Cita Click Backend

Este documento explica cómo arreglar los 88 tests fallando en el backend.

---

## 📊 **Resumen de Problemas Identificados**

### Problema 1: Mockito + Java 23 Incompatibilidad (81 errores)
**Error:**
```
MockitoException: Could not modify all classes [class java.lang.Object, class com.reservas.service.PlanLimitesService]
```

**Causa:** Mockito no puede mockear clases con `@RequiredArgsConstructor` de Lombok en Java 23.

**Solución:** Agregar `mockito-inline` para soportar mocking avanzado.

---

### Problema 2: NullPointerExceptions (7 errores)
**Error:**
```
NullPointerException: Cannot invoke "java.time.LocalDateTime.isAfter(...)"
because the return value of "com.reservas.entity.Cliente.getCreatedAt()" is null
```

**Causa:** Los builders de las entidades no inicializan campos de auditoría (`createdAt`, `updatedAt`).

**Solución:** Inicializar manualmente en los tests o usar `@PrePersist`.

---

### Problema 3: UUIDs como Strings (3 errores)
**Error:**
```
IllegalArgumentException: Invalid UUID string: servicio-123
```

**Causa:** Algunos tests usan Strings donde se esperan UUIDs.

**Solución:** Usar `UUID.randomUUID()` en lugar de strings.

---

## 🔧 **SOLUCIÓN 1: Configurar Mockito Inline**

### Paso 1.1: Agregar dependencia en `pom.xml`

Busca la sección de dependencias de testing y agrega:

```xml
<!-- Mockito Inline para Java 23 -->
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>
```

### Paso 1.2: Ejecutar tests de nuevo

```bash
mvn clean test
```

**Resultado esperado:** Los 81 errores de Mockito deberían desaparecer.

---

## 🔧 **SOLUCIÓN 2: Arreglar NullPointerExceptions**

### Paso 2.1: Actualizar `ReporteServiceTest.java`

**Archivo:** `src/test/java/com/reservas/service/ReporteServiceTest.java`

**Problema:** Los clientes no tienen `createdAt` inicializado.

**Fix:**

Busca donde se crean los clientes mock (líneas 70-100 aprox) y agrega:

```java
clienteMock = Cliente.builder()
        .id(UUID.randomUUID())
        .nombre("María")
        .apellidoPaterno("González")
        .email("maria@test.com")
        .telefono("+525512345678")
        .negocio(negocioMock)
        .createdAt(LocalDateTime.now())  // ← AGREGAR ESTA LÍNEA
        .updatedAt(LocalDateTime.now())  // ← AGREGAR ESTA LÍNEA
        .build();
```

Hacer lo mismo para todos los clientes mock en ese archivo (hay varios).

---

## 🔧 **SOLUCIÓN 3: Arreglar UUIDs**

### Paso 3.1: Actualizar `ServicioServiceTest.java`

**Archivo:** `src/test/java/com/reservas/service/ServicioServiceTest.java`

**Problema:** Usa strings como IDs en lugar de UUIDs.

**Fix:**

Busca líneas como:
```java
when(servicioRepository.findById("servicio-123"))
```

Cambiar por:
```java
UUID servicioId = UUID.randomUUID();
when(servicioRepository.findById(servicioId))
```

Y actualizar todas las referencias a usar `servicioId`.

---

## 🔧 **SOLUCIÓN 4: Arreglar NegocioServiceTest**

### Paso 4.1: Actualizar `NegocioServiceTest.java`

**Archivo:** `src/test/java/com/reservas/service/NegocioServiceTest.java`

**Problema:** Tests de "Success" fallan con `UnauthorizedException: Usuario no encontrado`.

**Fix:**

En todos los tests que fallan, asegurar que el mock del usuario esté correctamente configurado:

```java
@BeforeEach
void setUp() {
    // Usuario mock
    usuarioMock = Usuario.builder()
            .id(UUID.randomUUID())
            .email("test@test.com")
            .passwordHash("$2a$10$hash")
            .nombre("Juan")
            .apellidoPaterno("Pérez")
            .rol("admin")
            .activo(true)
            .negocio(negocioMock)
            .build();

    // IMPORTANTE: Mockear findByEmail
    when(usuarioRepository.findByEmail("test@test.com"))
            .thenReturn(Optional.of(usuarioMock));
}
```

---

## 🔧 **SOLUCIÓN 5: Arreglar ClienteServiceTest**

### Paso 5.1: Agregar mockito-inline ayudará

Con `mockito-inline`, los 20+ errores de `ClienteServiceTest` deberían resolverse automáticamente.

Si persisten, verificar que `@ExtendWith(MockitoExtension.class)` esté presente:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService - Pruebas Unitarias")
class ClienteServiceTest {
    // ...
}
```

---

## 📝 **Script Completo de Fixes**

### Fix Rápido (Recomendado)

1. **Agregar mockito-inline al pom.xml:**

```xml
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-inline</artifactId>
    <scope>test</scope>
</dependency>
```

2. **Ejecutar:**

```bash
mvn clean test
```

3. **Si aún hay errores, aplicar fixes individuales de arriba.**

---

## 🎯 **Tests Prioritarios a Arreglar**

En orden de prioridad:

1. ✅ **AuthServiceTest** (11 tests) - Autenticación crítica
2. ✅ **ClienteServiceTest** (20 tests) - Core business logic
3. ✅ **CitaServiceTest** (15 tests) - Core business logic
4. ⚠️ **ReporteServiceTest** (7 tests con NPE) - Necesita createdAt
5. ⚠️ **ServicioServiceTest** (2 tests con UUID) - Necesita UUIDs reales
6. ⚠️ **NegocioServiceTest** (4 tests) - Necesita mock de usuario

---

## 📊 **Expected Results Después de Fixes**

Con `mockito-inline` agregado:

| Status | Tests | Errores |
|--------|-------|---------|
| **Antes** | 107 | 88 |
| **Después (con mockito-inline)** | 107 | ~10-15 |
| **Después (con todos los fixes)** | 107 | 0 |

---

## 🚀 **Cómo Ejecutar Tests**

```bash
# Todos los tests
mvn test

# Solo un test específico
mvn test -Dtest=ClienteServiceTest

# Con logs detallados
mvn test -X

# Generar reporte de cobertura
mvn clean test jacoco:report

# Ver reporte en browser
open target/site/jacoco/index.html
```

---

## 🐛 **Debugging Tips**

### Ver stack trace completo

```bash
mvn test -e
```

### Ver qué test está fallando exactamente

```bash
mvn test 2>&1 | grep "ERROR"
```

### Ejecutar solo tests que no son de integración

```bash
mvn test -Dtest="*Test"
```

### Ejecutar solo tests de integración

```bash
mvn test -Dtest="*IntegrationTest"
```

---

## 📁 **Archivos a Modificar**

Lista completa de archivos que necesitan fixes:

1. **pom.xml** - Agregar mockito-inline ⭐ (CRÍTICO)
2. **ReporteServiceTest.java** - Agregar createdAt/updatedAt
3. **ServicioServiceTest.java** - Cambiar strings por UUIDs
4. **NegocioServiceTest.java** - Arreglar mocks de usuario
5. **ClienteServiceTest.java** - (Debería funcionar con mockito-inline)

---

## ✅ **Checklist de Validación**

Después de aplicar los fixes, verificar:

- [ ] `mvn clean test` ejecuta sin errores de compilación
- [ ] Al menos 90 de 107 tests pasan (84%)
- [ ] No hay errores de Mockito
- [ ] No hay NullPointerExceptions
- [ ] No hay IllegalArgumentExceptions de UUID
- [ ] Reporte de Jacoco se genera correctamente

---

## 🎓 **Mejores Prácticas para Tests Futuros**

1. **Siempre inicializar campos de auditoría en mocks:**
   ```java
   .createdAt(LocalDateTime.now())
   .updatedAt(LocalDateTime.now())
   ```

2. **Usar UUIDs reales, no strings:**
   ```java
   UUID id = UUID.randomUUID();
   ```

3. **Mockear todas las dependencias:**
   ```java
   when(repository.findById(id)).thenReturn(Optional.of(entity));
   ```

4. **Usar lenient() para stubs no utilizados:**
   ```java
   lenient().when(service.method()).thenReturn(value);
   ```

5. **Tests de integración en archivo separado:**
   ```java
   *IntegrationTest.java
   ```

---

## 🆘 **Si Nada Funciona**

### Opción Nuclear: Recrear Test Configuration

1. Crear archivo: `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
2. Contenido:
   ```
   mock-maker-inline
   ```

3. Recargar proyecto en IDE
4. Ejecutar: `mvn clean test`

---

**Siguiente paso recomendado:** Agregar `mockito-inline` al pom.xml y ejecutar `mvn clean test`.
