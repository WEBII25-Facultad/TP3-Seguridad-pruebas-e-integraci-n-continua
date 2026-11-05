# TP3 · Seguridad, Pruebas e Integración Continua

[![CI - Integración Continua](https://github.com/TU-USUARIO/NOMBRE-DEL-REPO/actions/workflows/ci.yml/badge.svg)](https://github.com/TU-USUARIO/NOMBRE-DEL-REPO/actions/workflows/ci.yml)

Evolución del sistema de catálogo, favoritos y listas (iniciado en TP1 y extendido a persistencia en TP2). En esta etapa se incorpora **Spring Security con JWT (JSON Web Tokens)**, **control de acceso y autorización por propiedad de recursos y roles**, **suite de pruebas unitarias y de integración con Testcontainers**, y **pipeline de Integración Continua (CI) en GitHub Actions**.

---

## 🚀 Arquitectura y Tecnologías
- **Java 25** (OpenJDK Temurin)
- **Spring Boot 4.1.1** (Spring MVC, Spring Data JPA, Spring Security, Validation)
- **PostgreSQL 16/17** con **Flyway** para migraciones evolutivas versionadas (`V1` a `V6`)
- **JJWT 0.11.5** para firma HMAC-SHA y validación de tokens Bearer
- **Testcontainers 2.x** con contenedor singleton de PostgreSQL para tests de integración
- **Surefire & Failsafe Plugins** para separación estricta entre tests unitarios e integrados
- **SpringDoc OpenAPI 2.7.0** (Swagger UI con soporte Bearer Auth)
- **GitHub Actions** para ejecución automatizada de pruebas y build en cada push a cualquier rama y PR
- **Docker Multi-Stage Build** para despliegue productivo en plataformas como Render

---

## 🗄️ Persistencia y Migraciones Evolutivas con Flyway (`V1` a `V6`)

La base de datos se gestiona exclusivamente a través de migraciones versionadas e incrementales mediante **Flyway**. Hibernate opera en modo `ddl-auto=validate`, asegurando que el esquema físico sea siempre controlado por scripts versionados y nunca generado mágicamente por el ORM.

### Historial Incremental de Migraciones

| Versión | Archivo | Responsabilidad / Cambios |
|---|---|---|
| `V1` | `V1__create_favoritos.sql` | Crea la tabla `favoritos` (id, producto_id, nota, fecha_alta). |
| `V2` | `V2__create_listas.sql` | Crea la tabla `listas` para agrupar favoritos. |
| `V3` | `V3__add_lista_id_a_favoritos.sql` | Agrega columna `lista_id` a `favoritos` vinculándola con clave foránea a `listas`. |
| `V4` | `V4__lista_id_obligatorio.sql` | Convierte `lista_id` en `NOT NULL` tras sanear los datos existentes. |
| **`V5`** | **`V5__create_usuarios.sql`** | **Incorpora la tabla `usuarios`** con columnas: `id` (autoincremental), `email` (único, no nulo), `password_hash` (almacena el hash BCrypt seguro, nunca texto plano) y `rol` (por ejemplo `USER` o `ADMIN`). |
| **`V6`** | **`V6__add_usuario_id_a_listas.sql`** | **Relaciona listas con usuarios:** Agrega la columna `usuario_id BIGINT` a `listas` junto con la restricción de clave foránea `fk_listas_usuario` referenciando a `usuarios(id)`, permitiendo asociar de manera estricta cada lista a su usuario creador. |

### Ejecución y Verificación Incremental
Al iniciar la aplicación o ejecutarse las pruebas de integración con Testcontainers:
1. Flyway consulta la tabla interna `flyway_schema_history`.
2. Aplica en orden ascendente y de manera transaccional únicamente las migraciones pendientes.
3. Si la base está vacía (como en cada test de integración), ejecuta secuencialmente desde `V1` hasta `V6`, dejando el esquema en el estado requerido de manera determinista e idempotente.

---

## 📌 Requisitos Previos
- **Java 25** instalado (o compatible).
- **Docker Desktop** (necesario para levantar PostgreSQL en local y para ejecutar las pruebas de integración con Testcontainers).
- Maven Wrapper incluido (`./mvnw` o `.\mvnw.cmd`).

---

## ⚙️ Configuración y Variables de Entorno

La aplicación soporta configuración externalizada mediante variables de entorno con fallbacks locales (`${VARIABLE:valorPorDefecto}`) en `application.properties`:

| Variable de Entorno | Propiedad de Spring | Valor por Defecto Local (docker-compose) | Descripción |
|---|---|---|---|
| `PORT` | `server.port` | `8080` | Puerto HTTP en el que escucha el servidor (Render inyecta `PORT` dinámicamente) |
| `DB_URL` | `spring.datasource.url` | `jdbc:postgresql://localhost:5432/tp2db` | URL JDBC de la base de datos PostgreSQL |
| `DB_USER` | `spring.datasource.username` | `tp2user` | Usuario de conexión a la BD |
| `DB_PASSWORD` | `spring.datasource.password` | `tp2pass` | Contraseña de conexión a la BD |
| `JWT_SECRET` | `jwt.secret` / `app.jwt.secret` | *(clave local mínima de 256 bits)* | Clave secreta para firmar tokens JWT con HMAC-SHA |
| `JWT_EXPIRATION` | `jwt.expiration` / `app.jwt.expiration-ms` | `86400000` (24 horas) | Tiempo de validez del token en milisegundos |

---

## 🛠️ Cómo Levantar el Proyecto en Desarrollo Local

### 1. Iniciar PostgreSQL con Docker
```bash
docker compose up -d
```
Los datos de conexión configurados por defecto coinciden directamente con `application.properties`:
- **Host / Puerto:** `localhost:5432`
- **Base:** `tp2db`
- **Usuario:** `tp2user`
- **Password:** `tp2pass`

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
Prueban la lógica de negocio y autorización de forma aislada mediante **Mockito** sin levantar contexto web ni bases de datos:
```bash
# Linux / Mac:
./mvnw test

# Windows:
.\mvnw.cmd test
```
*Clases incluidas:*
- `FavoritoServiceTest`: Verifica la lógica de autorización en favoritos (crear/actualizar validando propiedad de la lista, listar favoritos propios vs todos si es ADMIN, obtenerPorId y eliminar validando propiedad o ADMIN, y manejo de errores 403 y 404).
- `ListaServiceImplTest`: Verifica que al crear una lista se le asocie automáticamente el `usuarioId` del usuario autenticado, y que el acceso a listas ajenas arroje `AccesoDenegadoException` (403) salvo para `ADMIN`.
- `UsuarioServiceImplTest`: Comprueba que la contraseña se hashee mediante `PasswordEncoder` (BCrypt), sin almacenarse en texto plano, y que rechace registros con email duplicado.
- `Tp1FavoritosApplicationTests`: Verifica la carga del contexto base de Spring.

### Pruebas de Integración (`*IT.java` vía Maven Failsafe con Testcontainers)
Levantan un contenedor real de PostgreSQL descartable mediante **Testcontainers**, aplican todas las migraciones de Flyway contra esa base limpia y prueban el flujo completo HTTP con `MockMvc` y Spring Security:
> **Patrón Contenedor Singleton:** Para optimizar recursos y tiempos de ejecución, `BaseIntegrationTest` inicia una única instancia de contenedor PostgreSQL en un bloque `static`, compartida por todas las suites de integración y gestionada en su ciclo de vida por Testcontainers (Ryuk).

```bash
# Linux / Mac:
./mvnw verify

# Windows:
.\mvnw.cmd verify
```
*Clases incluidas:*
- `HealthControllerIT`:
  - `GET /health` sin token JWT → `200 OK` con `{"status":"UP"}`.
- `AuthControllerIT`:
  - `POST /api/auth/registro` con datos válidos → `201 Created`.
  - `POST /api/auth/registro` con email duplicado → `409 Conflict`.
  - `POST /api/auth/registro` con email inválido o password menor a 6 caracteres → `400 Bad Request`.
- `ListaControllerIT`:
  - `POST /api/auth/login` con credenciales válidas → `200 OK` con token JWT.
  - `POST /api/auth/login` con credenciales inválidas → `401 Unauthorized`.
  - Acceso a lista propia con token → `200 OK`.
  - Acceso a lista ajena (de otro usuario) → `403 Forbidden`.
  - Acceso a `/api/listas` sin token → `401 Unauthorized`.
  - Usuario con rol `ADMIN` accediendo a lista ajena → `200 OK`.
- `FavoritoControllerIT`:
  - Crear favorito en lista propia → `201 Created`.
  - Crear favorito en lista ajena → `403 Forbidden`.
  - Crear favorito en lista ajena con rol `ADMIN` → `201 Created`.
  - Obtener favorito en lista propia → `200 OK`.
  - Obtener favorito en lista ajena → `403 Forbidden`.
  - Obtener favorito en lista ajena con rol `ADMIN` → `200 OK`.
  - Listar favoritos: usuario común recibe únicamente favoritos de sus listas, mientras que `ADMIN` recibe todos.
  - Eliminar favorito en lista ajena → `403 Forbidden`.
  - Eliminar favorito en lista propia → `204 No Content`.

### Cómo interpretar el resultado en GitHub Actions (Pestaña Actions)
1. Ingresá a la pestaña **Actions** en tu repositorio de GitHub.
2. Seleccioná el workflow más reciente disparado por tu `push` o `pull_request`.
3. Al hacer clic en el job **Build & Verify (Unit + Integration Tests)** verás los pasos:
   - `Checkout del código fuente`
   - `Configurar Java JDK 25 (Temurin)`
   - `Dar permisos de ejecución al Maven Wrapper`
   - `Ejecutar pruebas unitarias y de integración (Surefire + Failsafe)`
4. Si todas las pruebas pasaron, el step mostrará un tilde verde (`BUILD SUCCESS`).
5. Desplegando el log del step podés ver el resumen de pruebas ejecutadas:
   - `Results: Tests run: 20, Failures: 0, Errors: 0, Skipped: 0` (Surefire unitarios).
   - `Results: Tests run: 21, Failures: 0, Errors: 0, Skipped: 0` (Failsafe integración).

---

## 🔄 Integración Continua (CI) con GitHub Actions

El repositorio cuenta con un pipeline automatizado definido en [`.github/workflows/ci.yml`](.github/workflows/ci.yml):

- **Triggers:** Se dispara automáticamente en **cada push a cualquier rama** (`on: push` sin filtros de rama) y en cada `pull_request`.
- **Ambiente:** Máquina virtual `ubuntu-latest` con soporte nativo para contenedores Docker (donde Testcontainers arranca PostgreSQL automáticamente).
- **Java:** Configura Eclipse Temurin JDK 25 con caché de dependencias Maven.
- **Validación:** Ejecuta `./mvnw -B verify`, corriendo compilación, pruebas unitarias y pruebas de integración de punta a punta.

---

## 📖 Documentación Interactiva (Swagger / OpenAPI)

Con la aplicación en ejecución, ingresá a:
👉 [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) (o la especificación OpenAPI en `/v3/api-docs`).

Tanto `FavoritoController` como `ListaController` están debidamente anotados con sus descripciones indicando que **requieren autenticación obligatoria con JWT**.

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
| GET | `/health` | 200 OK | Health check de la aplicación: responde `{"status":"UP"}` |
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
| POST | `/api/favoritos` | 201 Created | Valida que la lista indicada pertenece al usuario (o es `ADMIN`), si no 403. Si la lista no existe, 404 |
| GET | `/api/favoritos` | 200 OK | Devuelve solo los favoritos pertenecientes a las listas del usuario (todos si es `ADMIN`) |
| GET | `/api/favoritos/{id}` | 200 OK | Busca el favorito (404 si no existe), busca su lista y valida propiedad, si no 403 (salvo `ADMIN`) |
| PUT | `/api/favoritos/{id}` | 200 OK | Valida lista destino (404/403) y que el favorito existente pertenezca a sus listas (403 si ajeno, 404 si no existe) |
| DELETE | `/api/favoritos/{id}` | 204 No Content | Busca favorito (404), busca su lista y valida propiedad (403 si no es dueño ni `ADMIN`) |

---

## 🏛️ Justificación de la Autorización en la Arquitectura (Consigna 4.5)

### ¿Dónde vive la lógica de autorización?

En este proyecto, **toda** la validación de permisos y propiedad de recursos reside **en la capa de Servicio / Aplicación** — centralizada en [`UsuarioActualService`](src/main/java/ar/edu/unvime/tp1favoritos/usuarios/service/UsuarioActualService.java) y reutilizada en [`ListaService`](src/main/java/ar/edu/unvime/tp1favoritos/listas/ListaService.java) y [`FavoritoService`](src/main/java/ar/edu/unvime/tp1favoritos/favoritos/FavoritoService.java) — y **nunca en los Controllers**.

El flujo de una petición protegida es el siguiente:

```
[ HTTP Request: GET /api/listas/{id} o GET /api/favoritos/{id} ]
       │
       ▼
┌──────────────────────────────────────────┐
│          JwtAuthenticationFilter          │
│  Lee el header "Authorization: Bearer …" │  ← Responsabilidad: autenticación de TRANSPORTE
│  Valida firma JWT, extrae email + rol.   │    ¿Es la petición de quien dice ser? (AuthN)
│  Puebla SecurityContextHolder.            │
└──────────────────┬───────────────────────┘
                   │  El filtro solo dice "quién sos", no "qué podés hacer"
                   ▼
┌──────────────────────────────────────────┐
│     ListaController / FavoritoController │
│  @GetMapping("/{id}")                    │  ← Responsabilidad: adaptador HTTP de entrada
│  Deserializa path variable / body, llama  │    Traduce HTTP → llamada Java → HTTP
│  al Service y formatea la respuesta JSON. │    SIN lógica de negocio ni de autorización
└──────────────────┬───────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────┐
│      ListaService / FavoritoService      │
│  1. usuarioActualService.obtenerUsuarioActual() │  ← Responsabilidad: lógica de NEGOCIO (AuthZ)
│     → extrae el usuario autenticado      │    ¿Tiene permiso para hacer esta operación?
│  2. usuarioActualService.validarPropiedadOAdmin(...)
│     → si recurso.usuarioId ≠ usuario.id   │
│        Y rol ≠ ADMIN                     │
│        → lanza AccesoDenegadoException    │
│  3. Ejecuta la operación si está OK.      │
└──────────────────┬───────────────────────┘
                   │
                   ▼
┌──────────────────────────────────────────┐
│   Puertos de Repositorio & Adaptadores   │  ← Responsabilidad: persistencia
└──────────────────────────────────────────┘
```

> **Regla de oro aplicada:** `JwtAuthenticationFilter` resuelve *autenticación* (¿quién sos?). `ListaService` y `FavoritoService` resuelven *autorización* (¿qué podés hacer?). Los controladores no resuelven ninguna de las dos.

---

### ¿Por qué en el Service y no en el Controller?

#### 1. Principio de Responsabilidad Única y Arquitectura Hexagonal

La arquitectura hexagonal define con rigor las responsabilidades de cada capa:

| Capa | Rol en la arquitectura | ¿Autorización aquí? |
|------|------------------------|----------------------|
| **Filtro de Seguridad** (`JwtAuthenticationFilter`) | Autenticación de transporte: valida el token y establece la identidad | ✅ Solo autenticación (`AuthN`) |
| **Controller** (`ListaController` / `FavoritoController`) | Adaptador de entrada HTTP: traduce peticiones HTTP a llamadas Java y serializa respuestas | ❌ **No** — violaría SRP |
| **Service** (`ListaService` / `FavoritoService`) | Lógica de aplicación y de negocio: orquesta casos de uso y aplica invariantes | ✅ **Aquí vive la autorización** (`AuthZ`) |
| **Repository / Adapter** | Persistencia e infraestructura | ❌ No |

Si se pusiera la validación de propiedad en los controladores, estos dejarían de ser simples adaptadores de transporte y pasarían a conocer reglas de negocio (quién puede operar sobre qué recurso). La validación fue centralizada en `UsuarioActualService.validarPropiedadOAdmin`:

```java
// ar.edu.unvime.tp1favoritos.usuarios.service.UsuarioActualService
public void validarPropiedadOAdmin(Usuario usuario, Long propietarioId, Long listaId) {
    if (esAdmin(usuario)) {
        return;
    }
    if (propietarioId == null || !propietarioId.equals(usuario.id())) {
        throw new AccesoDenegadoException("No tiene permisos para operar sobre la lista con id " + listaId);
    }
}
```

Tanto `ListaService` como `FavoritoService` invocan esta misma regla asegurando una sola fuente de verdad:
- `ListaService`: valida que el usuario sea dueño de la lista o ADMIN para leerla, agregarle favoritos, vaciarla o mover sus elementos.
- `FavoritoService`: al crear o actualizar, valida que la lista destino le pertenezca; al consultar o eliminar un favorito, busca su lista asociada y valida idéntica regla de pertenencia o rol ADMIN.

#### 2. Desacoplamiento del Framework Web e Independencia del Medio de Entrada

En la arquitectura hexagonal, **el núcleo de dominio y aplicación (Services) es agnóstico del protocolo de transporte**. 

Si el día de mañana se agregan nuevos puntos de entrada:
- 🔌 Un **consumidor de eventos/mensajes** (RabbitMQ / Kafka).
- ⏰ Un **job programado** (`@Scheduled`).
- 🖥️ Un **CLI** de línea de comandos para tareas administrativas.
- 🌐 Un canal **gRPC** o WebSocket.

Si la autorización residiera en el `Controller`, ninguno de estos canales heredaría las reglas de seguridad y cada uno debería reimplementar chequeos duplicados con alto riesgo de vulnerabilidades. Al ubicarse en el `Service`, **la regla de autorización se aplica invariablemente a cualquier llamada al caso de uso**.

#### 3. Atomicidad Transaccional y Prevención de BOLA / IDOR

Operaciones complejas como `moverFavoritos(origenId, request)` involucran dos listas diferentes (`origen` y `destino`). En `ListaService.moverFavoritos`:

```java
@Transactional
public void moverFavoritos(Long origenId, MoverFavoritosRequest request) {
    if (origenId.equals(request.listaDestinoId())) {
        throw new OperacionInvalidaException("La lista origen y la lista destino no pueden ser la misma");
    }

    Lista origen = buscarOFallar(origenId);
    Lista destino = buscarOFallar(request.listaDestinoId());

    Usuario usuarioActual = usuarioActualService.obtenerUsuarioActual();
    validarPropiedadOAdmin(origen, usuarioActual);
    validarPropiedadOAdmin(destino, usuarioActual);

    List<Favorito> favoritos = favoritoRepository.findByListaId(origenId);
    for (Favorito f : favoritos) {
        favoritoRepository.save(new Favorito(f.id(), f.productoId(), f.nota(), f.fechaAgregado(), destino.id()));
    }

    listaRepository.deleteById(origenId);
}
```

La autorización sobre ambas listas se valida de forma atómica dentro de la transacción (`@Transactional`) antes de persistir cualquier mutación. Esto previene de raíz vulnerabilidades de tipo **BOLA** (*Broken Object Level Authorization*) e **IDOR** (*Insecure Direct Object Reference*), catalogadas como críticas en el OWASP API Security Top 10.

#### 4. Testabilidad Determinista con Pruebas Unitarias Puras

Al desacoplar la autorización de Spring MVC y de los filtros de transporte, la lógica de permisos se puede testear al 100% mediante **Mockito puro**, sin levantar servidores web ni contextos de Spring:

```java
// Ejemplo real en ListaServiceImplTest:
@Test
@DisplayName("obtenerPorId: lanza AccesoDenegadoException si la lista pertenece a otro usuario y no es ADMIN")
void obtenerPorId_listaAjenaYNoAdmin_lanzaAccesoDenegado() {
    Long listaId = 5L;
    Lista listaDeOtro = new Lista(listaId, "Lista Ajena", 99L);
    when(listaRepository.findById(listaId)).thenReturn(Optional.of(listaDeOtro));
    when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
    when(usuarioActualService.esAdmin(usuarioAutenticado)).thenReturn(false);

    assertThatThrownBy(() -> listaService.obtenerPorId(listaId))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No tiene permisos");
}
```

Y en `FavoritoServiceTest`:
```java
@Test
@DisplayName("obtenerPorId_favoritoEnListaAjenaYNoAdmin_lanzaAccesoDenegado")
void obtenerPorId_favoritoEnListaAjenaYNoAdmin_lanzaAccesoDenegado() {
    Favorito fav = new Favorito(2L, 102L, "Nota 2", LocalDateTime.now(), 2L);
    when(repository.findById(2L)).thenReturn(Optional.of(fav));
    when(listaRepository.findById(2L)).thenReturn(Optional.of(listaAjena));
    when(usuarioActualService.obtenerUsuarioActual()).thenReturn(usuarioAutenticado);
    doThrow(new AccesoDenegadoException("No tiene permisos para operar sobre la lista con id 2"))
            .when(usuarioActualService).validarPropiedadOAdmin(usuarioAutenticado, 20L, 2L);

    assertThatThrownBy(() -> favoritoService.obtenerPorId(2L))
            .isInstanceOf(AccesoDenegadoException.class)
            .hasMessageContaining("No tiene permisos");
}
```

---

## 🌐 Despliegue en Render con Docker

El repositorio incluye un `Dockerfile` multi-stage optimizado que compila y empaqueta la aplicación con JDK 25 y genera una imagen liviana con JRE 25.

### 1. Crear la Base de Datos PostgreSQL en Render
1. En el dashboard de [Render](https://dashboard.render.com), hacé clic en **New +** y seleccioná **PostgreSQL**.
2. Asignale un nombre y completá la creación.
3. Copiá la URL de conexión interna o los datos de conexión individuales (`Host`, `Database`, `User`, `Password`).

> ⚠️ **Conversión importante para la URL de Base de Datos:**
> Render entrega las URLs de PostgreSQL con el esquema `postgresql://usuario:password@host:port/database`.
> El driver JDBC de Spring Boot requiere estrictamente el prefijo **`jdbc:postgresql://`**.
> Por ejemplo, si Render te da:
> `postgresql://tp2user:secreto@dpg-abc-a.oregon-postgres.render.com/tp2db`
> Debes configurarla en la variable `DB_URL` como:
> `jdbc:postgresql://dpg-abc-a.oregon-postgres.render.com:5432/tp2db` (o anteponer `jdbc:` a la URL completa).

### 2. Crear el Web Service en Render
1. Hacé clic en **New +** → **Web Service** y seleccioná tu repositorio.
2. Render detectará automáticamente el archivo `Dockerfile` en la raíz. Seleccioná el runtime **Docker**.
3. En la sección **Health Check Path**, configurá:
   `/health`
4. En la pestaña **Environment Variables**, configurá las siguientes variables obligatorias:

| Variable | Valor de Ejemplo | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://dpg-xxx.oregon-postgres.render.com:5432/tp2db` | URL JDBC de la base de datos de Render |
| `DB_USER` | `tp2user` | Usuario de PostgreSQL |
| `DB_PASSWORD` | `tuPasswordDeRender` | Contraseña de PostgreSQL |
| `JWT_SECRET` | `cadena_secreta_segura_de_produccion_al_menos_256_bits_1234567890` | Clave HMAC-SHA de al menos 256 bits |
| `JWT_EXPIRATION` | `86400000` | Expiración del token (opcional, default 24h) |

> ℹ️ Render inyecta automáticamente la variable `PORT` en el contenedor. La propiedad `server.port=${PORT:8080}` configurada en `application.properties` hace que Spring Boot enlace dinámicamente al puerto asignado por Render.

### 3. Verificación
Al finalizar el deploy, Render consultará `GET /health` (responde `200 OK` con `{"status":"UP"}` sin token). Las migraciones de Flyway se aplicarán automáticamente y el servicio quedará 100% operativo bajo HTTPS.
