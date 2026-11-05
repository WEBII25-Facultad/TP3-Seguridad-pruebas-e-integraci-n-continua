# TP3 · Seguridad, Pruebas e Integración Continua

[![CI - Integración Continua](https://github.com/tu-usuario/TP2-Persistencia-migraciones-y-arquitectura-hexagonal/actions/workflows/ci.yml/badge.svg)](https://github.com/tu-usuario/TP2-Persistencia-migraciones-y-arquitectura-hexagonal/actions/workflows/ci.yml)

Evolución del sistema de catálogo, favoritos y listas (iniciado en TP1 y extendido a persistencia en TP2). En esta etapa se incorpora **Spring Security con JWT (JSON Web Tokens)**, **control de acceso y autorización por propiedad de recursos y roles**, **suite de pruebas unitarias y de integración con Testcontainers**, y **pipeline de Integración Continua (CI) en GitHub Actions**.

---

## 🚀 Arquitectura y Tecnologías
- **Java 25** (OpenJDK Temurin)
- **Spring Boot 4.1.1** (Spring MVC, Spring Data JPA, Spring Security, Validation)
- **PostgreSQL 16** con **Flyway** para migraciones evolutivas versionadas
- **JJWT 0.11.5** para firma HMAC-SHA y validación de tokens Bearer
- **Testcontainers 1.20.4** con contenedores descartables de PostgreSQL para tests de integración
- **Surefire & Failsafe Plugins** para separación estricta entre tests unitarios e integrados
- **SpringDoc OpenAPI 2.7.0** (Swagger UI con soporte Bearer Auth)
- **GitHub Actions** para ejecución automatizada de pruebas y build en cada push / PR

---

## 📌 Requisitos Previos
- **Java 25** instalado (o compatible).
- **Docker Desktop** (necesario para levantar PostgreSQL en local y para ejecutar las pruebas con Testcontainers).
- Maven Wrapper incluido (`./mvnw` o `.\mvnw.cmd`).

---

## ⚙️ Configuración y Variables de Entorno

La aplicación soporta configuración externalizada mediante variables de entorno con fallbacks locales (`${VARIABLE:valorPorDefecto}`) en `application.properties`:

| Variable de Entorno | Propiedad de Spring | Valor por Defecto Local | Descripción |
|---|---|---|---|
| `DB_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/tu_base` | URL JDBC de la base de datos PostgreSQL |
| `DB_USER` | `spring.datasource.username` | `postgres` | Usuario de conexión a la BD |
| `DB_PASSWORD` | `spring.datasource.password` | `postgres` | Contraseña de conexión a la BD |
| `JWT_SECRET` | `jwt.secret` / `app.jwt.secret` | *(clave local mínima de 256 bits)* | Clave secreta para firmar tokens JWT con HMAC-SHA |
| `JWT_EXPIRATION` | `jwt.expiration` / `app.jwt.expiration-ms` | `86400000` (24 horas) | Tiempo de validez del token en milisegundos |

---

## 🛠️ Cómo Levantar el Proyecto en Desarrollo Local

### 1. Iniciar PostgreSQL con Docker
```bash
docker compose up -d
```
Los datos de conexión configurados por defecto en el `docker-compose.yml` son:
- **Host / Puerto:** `localhost:5432`
- **Base:** `tp2db`
- **Usuario:** `tp2user`
- **Password:** `tp2pass`

*(Podés pasar `DB_URL=jdbc:postgresql://localhost:5432/tp2db DB_USER=tp2user DB_PASSWORD=tp2pass` si querés usar los datos del compose).*

### 2. Ejecutar la Aplicación
```bash
# En Linux / Mac:
./mvnw spring-boot:run

# En Windows:
.\mvnw.cmd spring-boot:run
```
La aplicación iniciará en `http://localhost:8080`. Flyway aplicará automáticamente las migraciones (`V1` a `V6`).

---

## 🧪 Instrucciones para Ejecutar las Pruebas

El proyecto implementa la convención estándar de Maven para separar pruebas unitarias de pruebas de integración:

### Pruebas Unitarias (`*Test.java` vía Maven Surefire)
Prueban la lógica de negocio aislada mediante **Mockito** sin levantar contexto web ni bases de datos.
```bash
# Linux / Mac:
./mvnw test

# Windows:
.\mvnw.cmd test
```
*Clases incluidas:*
- `ListaServiceImplTest`: Verifica que al crear una lista se le asocie automáticamente el `usuarioId` del usuario autenticado, y que el acceso a listas ajenas arroje `AccesoDenegadoException` salvo para `ADMIN`.
- `UsuarioServiceImplTest`: Comprueba que la contraseña se hashee mediante `PasswordEncoder` (BCrypt), sin almacenarse en texto plano.

### Pruebas de Integración (`*IT.java` vía Maven Failsafe con Testcontainers)
Levantan un contenedor real de PostgreSQL descartable mediante **Testcontainers**, aplican todas las migraciones de Flyway contra esa base limpia y prueban el flujo completo HTTP con `MockMvc` y Spring Security:
```bash
# Linux / Mac:
./mvnw verify

# Windows:
.\mvnw.cmd verify
```
*Clases incluidas:*
- `ListaControllerIT`:
  - `POST /api/auth/login` con credenciales válidas → `200 OK` con token JWT.
  - `POST /api/auth/login` con credenciales inválidas → `401 Unauthorized`.
  - Acceso a lista propia con token → `200 OK`.
  - Acceso a lista ajena (de otro usuario) → `403 Forbidden`.
  - Acceso a `/api/listas` sin token → `401 Unauthorized`.
  - Usuario con rol `ADMIN` accediendo a lista ajena → `200 OK`.

---

## 🔄 Integración Continua (CI) con GitHub Actions

El repositorio cuenta con un pipeline automatizado definido en [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

- **Triggers:** Se dispara automáticamente en cada `push` y `pull_request` contra las ramas `main`, `master` y `tp3`.
- **Ambiente:** Máquina virtual `ubuntu-latest` con soporte nativo para contenedores Docker.
- **Java:** Configura Eclipse Temurin JDK 25 con caché de dependencias Maven.
- **Validación:** Ejecuta `./mvnw -B verify`, corriendo tanto la compilación como las pruebas unitarias y las pruebas de integración con Testcontainers de manera desatendida.

---

## 📖 Documentación Interactiva (Swagger / OpenAPI)

Con la aplicación en ejecución, ingresá a:
👉 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

### Autenticación en Swagger UI:
1. Registrate en `POST /api/auth/registro` o hacé login en `POST /api/auth/login`.
2. Copiá el token retornado en la respuesta.
3. Hacé clic en el botón verde **Authorize** (arriba a la derecha) y pegá el token (Swagger agregará automáticamente el prefijo `Bearer`).
4. Ahora podés consumir los endpoints protegidos de `/api/favoritos/**` y `/api/listas/**`.

---

## 🔒 Catálogo de Endpoints y Reglas de Seguridad

### Endpoints Públicos (No requieren token)
| Método | Endpoint | Código Éxito | Descripción |
|---|---|---|---|
| POST | `/api/auth/registro` | 201 Created | Registra un nuevo usuario con rol `USER` y contraseña hasheada (409 si el email ya existe) |
| POST | `/api/auth/login` | 200 OK | Autentica contra la BD y devuelve token JWT Bearer (401 si falla) |
| GET | `/api/productos` | 200 OK | Catálogo de productos externos (DummyJSON) |
| GET | `/api/productos/{id}` | 200 OK | Detalle de un producto externo |

### Endpoints Protegidos (Requieren `Authorization: Bearer <token>`)
| Método | Endpoint | Código Éxito | Regla de Autorización / Propiedad |
|---|---|---|---|
| POST | `/api/listas` | 201 Created | Asigna como dueño al usuario autenticado automáticamente |
| GET | `/api/listas` | 200 OK | Devuelve solo las listas del usuario (o todas si es `ADMIN`) |
| GET | `/api/listas/{id}` | 200 OK | Solo dueño o `ADMIN` (403 Forbidden si es ajena) |
| GET | `/api/listas/{id}/favoritos`| 200 OK | Solo dueño o `ADMIN` (403 Forbidden si es ajena) |
| DELETE | `/api/listas/{id}` | 204 No Content | Solo dueño o `ADMIN` y si está vacía (403 / 409) |
| POST | `/api/listas/{origenId}/mover-favoritos` | 204 No Content | Requiere permiso sobre origen y destino (403 si alguna es ajena) |
| GET / POST / PUT / DELETE | `/api/favoritos/**` | 200 / 201 / 204 | Requieren token JWT válido (401 si falta o expira) |

---

## 🏛️ Justificación de la Autorización en la Arquitectura (Consigna 4.5)

### ¿Dónde vive la lógica de autorización?

En este proyecto, **toda** la validación de propiedad de recursos y el control de acceso por roles reside **en la capa de Servicio / Aplicación** — concretamente en [`ListaService`](src/main/java/ar/edu/unvime/tp1favoritos/listas/ListaService.java), auxiliada por [`UsuarioActualService`](src/main/java/ar/edu/unvime/tp1favoritos/usuarios/UsuarioActualService.java) — y **nunca en los Controllers**.

El flujo de una petición protegida es el siguiente:

```
[ HTTP Request: GET /api/listas/{id} ]
       │
       ▼
┌──────────────────────────────────────────┐
│          JwtAuthenticationFilter          │
│  Lee el header "Authorization: Bearer …" │  ← Responsabilidad: autenticación de TRANSPORTE
│  Valida firma JWT, extrae id + rol.       │    ¿Es la petición de quien dice ser? (AuthN)
│  Puebla SecurityContextHolder.            │
└──────────────────┬───────────────────────┘
                   │  El filtro solo dice "quién sos", no "qué podés hacer"
                   ▼
┌──────────────────────────────────────────┐
│              ListaController              │
│  @GetMapping("/{id}")                    │  ← Responsabilidad: adaptador HTTP
│  Deserializa el path variable, llama al  │    Traduce HTTP → llamada Java → HTTP
│  Service y formatea la respuesta JSON.   │    SIN lógica de negocio ni de autorización
└──────────────────┬───────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────┐
│              ListaService                 │
│  1. usuarioActualService.obtener()        │  ← Responsabilidad: lógica de NEGOCIO (AuthZ)
│     → extrae el usuario del contexto     │    ¿Tiene permiso para hacer esta operación?
│  2. validarPropiedadOAdmin(lista, usuario)│
│     → si lista.usuarioId ≠ usuario.id    │
│        Y rol ≠ ADMIN                     │
│        → lanza AccesoDenegadoException   │
│  3. Ejecuta la operación si está OK.      │
└──────────────────┬───────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────┐
│   ListaRepository (Puerto)               │
│   ListaRepositoryAdapter (Adaptador JPA) │  ← Responsabilidad: persistencia
└──────────────────────────────────────────┘
```

> **Regla de oro aplicada:** `JwtAuthenticationFilter` resuelve *autenticación* (¿quién sos?). `ListaService` resuelve *autorización* (¿qué podés hacer?). `ListaController` no resuelve ninguna de las dos.

---

### ¿Por qué en el Service y no en el Controller?

#### 1. Principio de Responsabilidad Única y Arquitectura Hexagonal

La arquitectura hexagonal define con precisión qué hace cada capa:

| Capa | Rol en la arquitectura | ¿Autorización aquí? |
|------|------------------------|----------------------|
| **Filtro de Seguridad** (`JwtAuthenticationFilter`) | Autenticación de transporte: valida el token y establece la identidad | ✅ Solo autenticación (`AuthN`) |
| **Controller** (`ListaController`) | Adaptador de entrada HTTP: traduce peticiones HTTP a llamadas Java y viceversa | ❌ **No** — violaría SRP |
| **Service** (`ListaService`) | Lógica de aplicación y de negocio: orquesta casos de uso | ✅ **Aquí vive la autorización** (`AuthZ`) |
| **Repository / Adapter** | Persistencia e infraestructura | ❌ No |

Si se pusiera la validación de propiedad en `ListaController`, el controlador dejaría de ser un simple adaptador de transporte y pasaría a conocer reglas de negocio (quién puede acceder a qué). Eso viola el **Principio de Responsabilidad Única (SRP)** y la separación de capas que es el corazón de la arquitectura hexagonal.

#### 2. Independencia del Medio de Entrada — el argumento más fuerte

Este es el argumento clave de la arquitectura hexagonal: **el núcleo de la aplicación (Service) no debe saber ni importarle por qué "puerto" entró la solicitud**.

Imaginemos que mañana se agrega:
- 🔌 Un **consumer de Kafka** que procesa comandos de listas desde mensajes.
- ⏰ Un **job programado** (`@Scheduled`) que ejecuta operaciones sobre listas automáticamente.
- 🖥️ Una **interfaz CLI** de administración interna.
- 🌐 Un **endpoint gRPC** para clientes móviles.

Si la autorización estuviera en `ListaController`, **ninguno de estos nuevos puntos de entrada heredaría las protecciones**. Cada uno debería reimplementar las reglas de acceso por separado, con alto riesgo de inconsistencias y brechas de seguridad.

Al estar en `ListaService`, **la misma regla de negocio protege la operación sin importar desde dónde se invoque**.

#### 3. Atomicidad en Operaciones Complejas (Prevención de BOLA/IDOR)

La operación `moverFavoritos(origenId, destinoId)` involucra **dos** listas. La autorización debe validarse para ambas *antes* de mutar cualquier dato, y todo dentro del mismo contexto transaccional (`@Transactional`).

Si esta validación estuviera en el Controller:
```java
// ❌ Patrón incorrecto (en Controller):
// Validar lista origen → validar lista destino → llamar Service
// Si el Service falla a mitad, el Controller ya "tomó decisiones" fuera de la transacción
```

En el Service, la secuencia completa ocurre de forma atómica:
```java
// ✅ Patrón correcto (en Service):
@Transactional
public void moverFavoritos(Long origenId, Long destinoId) {
    validarPropiedadOAdmin(listaOrigen, usuarioActual); // ← AuthZ dentro de la tx
    validarPropiedadOAdmin(listaDestino, usuarioActual); // ← AuthZ dentro de la tx
    // ... muta datos solo si ambas validaciones pasan
}
```

Esto previene vulnerabilidades **BOLA** (Broken Object Level Authorization) / **IDOR** (Insecure Direct Object Reference), que son las vulnerabilidades de API más comunes según OWASP API Security Top 10.

#### 4. Testabilidad Determinista sin Framework Web

Al ubicar la autorización en el `Service`, se puede probar **toda la lógica de acceso con tests unitarios puros** (sin levantar Spring MVC, sin MockMvc, sin contexto de seguridad real):

```java
// En ListaServiceImplTest — prueba de autorización SIN Spring ni HTTP:
@Test
void obtener_usuarioNoEsDueno_lanzaAccesoDenegadoException() {
    when(listaRepository.buscarPorId(1L)).thenReturn(Optional.of(listaDeOtroUsuario));
    when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioActual);

    assertThrows(AccesoDenegadoException.class, () -> listaService.obtener(1L));
}
```

Si la autorización estuviera en el Controller, testearla requeriría:
- Levantar todo el contexto de Spring MVC (`@WebMvcTest`)
- Mockear Spring Security completo
- Usar `MockMvc` para simular peticiones HTTP
- Tests más frágiles, lentos y acoplados al framework

**La lógica en el Service permite tests rápidos, deterministas y sin dependencias de infraestructura.**

#### 5. Consistencia Garantizada — Una Sola Fuente de Verdad

Con la autorización en el Service, es **imposible** agregar un nuevo endpoint en `ListaController` y olvidarse de poner el chequeo de autorización, porque **el chequeo está en el Service, que siempre se invoca**. El riesgo de que un nuevo desarrollador agregue un endpoint "olvidando" la validación de propiedad queda eliminado por diseño.

---

### Resumen ejecutivo

| Criterio | ❌ Autorización en Controller | ✅ Autorización en Service |
|----------|------------------------------|---------------------------|
| **SRP / Hexagonal** | Viola: el Controller hace dos cosas | Cumple: cada capa tiene una sola razón de cambiar |
| **Independencia de entrada** | No protege canales no-HTTP (Kafka, CLI, gRPC) | Protege cualquier canal que llame al Service |
| **Atomicidad transaccional** | Validaciones fuera de la transacción | Validaciones dentro de `@Transactional` |
| **Testabilidad** | Requiere contexto HTTP + Spring Security completo | Tests unitarios con Mockito puro, sin framework |
| **Riesgo de olvido** | Alto: cada nuevo endpoint debe recordar validar | Bajo: la validación es inherente al caso de uso |
| **Vulnerabilidades OWASP** | BOLA/IDOR posible en operaciones multi-entidad | Prevenido por atomicidad en Service |

---

## 🌐 Pasos para Despliegue en Ambiente Productivo (Render / Cloud)

Para desplegar la aplicación en una plataforma como **Render** con PostgreSQL administrado:

1. **Crear la Base de Datos PostgreSQL en Render:**
   - Desde el dashboard de Render, crear una nueva instancia de PostgreSQL (`New PostgreSQL`).
   - Copiar los datos de conexión interna/externa (Host, Database, Username, Password).

2. **Crear el Web Service para la Aplicación Spring Boot:**
   - Crear un nuevo `Web Service` apuntando al repositorio de GitHub.
   - Configurar el entorno como **Docker** o **Java**.
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -jar target/tp1-favoritos-0.0.1-SNAPSHOT.jar`

3. **Configurar las Variables de Entorno en Render:**
   En la pestaña *Environment* del Web Service, añadir:
   ```env
   DB_URL=jdbc:postgresql://<RENDER_PG_HOST>:5432/<RENDER_PG_DATABASE>
   DB_USER=<RENDER_PG_USER>
   DB_PASSWORD=<RENDER_PG_PASSWORD>
   JWT_SECRET=<CADENA_ALEATORIA_SEGURA_DE_AL_MENOS_256_BITS_GENERADA_PARA_PROD>
   JWT_EXPIRATION=86400000
   ```

4. **Verificación:**
   Al desplegarse, Spring Boot se conectará a la base de Render, Flyway ejecutará automáticamente las migraciones `V1` a `V6`, y los endpoints estarán listos y securizados bajo HTTPS.
