# ChessDigitizer

*Desarrollo del Trabajo de Fin de Grado en Ingeniería Informática — Universidad de Las Palmas de Gran Canaria (ULPGC)*

**Digitalización inteligente de diagramas de ajedrez en libros PDF: detección automática de tableros mediante visión por computador, reconstrucción de la posición en FEN, y análisis interactivo con motor de ajedrez integrado.**

---

## Introducción

Los libros de ajedrez en PDF contienen diagramas de tablero como imágenes estáticas: no se pueden mover piezas, analizar la posición ni consultar un motor sobre ellas. ChessDigitizer resuelve ese problema permitiendo importar un libro en PDF, localizar automáticamente los diagramas de cada página, reconstruir la posición como notación FEN y, a partir de ahí, interactuar con ella sobre un tablero real: jugar variantes, anotar comentarios, construir un árbol de análisis y evaluar cualquier posición con el motor Stockfish, corrigiendo manualmente cualquier error de reconocimiento cuando sea necesario.

El sistema está compuesto por tres partes que se despliegan y ejecutan de forma independiente:

- Un **backend** en Java 21 / Spring Boot que expone la API REST, orquesta el pipeline de reconocimiento, gestiona la persistencia y se comunica con Stockfish.
- Un **frontend** en Vue 3 + TypeScript (SPA) que renderiza el PDF, superpone los tableros detectados y aloja el editor de análisis.
- Tres **microservicios de visión por computador** en Python/FastAPI, cada uno responsable de una etapa del reconocimiento: detección del tablero en la página, segmentación de sus 64 casillas y clasificación de la pieza en cada casilla.

Este repositorio forma parte del Trabajo de Fin de Grado:

*Sistema inteligente de digitalización e interacción de diagramas de ajedrez en documentos PDF*

**Autor:** Daniel Moreno López
**Tutores:** José Juan Hernández Cabrera y José Juan Hernández Gálvez
**Titulación:** Grado en Ingeniería Informática
**Fecha:** Junio de 2026

---

## Tabla de contenidos

- [Características](#características)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Variables de entorno](#variables-de-entorno)
- [Ejecución](#ejecución)
- [Flujo de la aplicación](#flujo-de-la-aplicación)
- [Base de datos](#base-de-datos)
- [API](#api)
- [Estructura del código](#estructura-del-código)
- [Dependencias importantes](#dependencias-importantes)
- [Despliegue](#despliegue)

---

## Características

- **Importación de libros PDF** con almacenamiento por usuario y renderizado de páginas a imagen (`BookController`, `LocalPdfRenderer`, Apache PDFBox).
- **Detección automática de tableros** en la imagen de cada página mediante un modelo YOLOv8 (`ms-board-detector`).
- **Segmentación de cada tablero en sus 64 casillas** mediante un pipeline clásico de visión (Sobel + transformada de Hough), con un *fallback* a rejilla uniforme si la detección de líneas falla (`ms-board-segmenter`).
- **Clasificación de la pieza presente en cada casilla** (tipo y color) mediante una ResNet-18 (`ms-piece-classifier`), con un heurístico de brillo del núcleo de la pieza para determinar el color.
- **Reconstrucción automática del FEN** de la posición a partir de las 64 clasificaciones (`AnalyzeBoardService`).
- **Validación de legalidad ajedrecística** del FEN generado (número de reyes, exceso de piezas/peones, peones en filas imposibles, reyes adyacentes, doble jaque), implementada tanto en el backend (Java, fuente de verdad) como en el frontend (TypeScript, para *feedback* instantáneo).
- **Flujo de corrección manual** cuando la posición detectada es ilegal: el usuario reubica piezas sobre el tablero y el sistema revalida en tiempo real antes de persistir.
- **Tablero interactivo** (`vue3-chessboard` + `chess.js`) con modo libre/reglado, giro de tablero y flecha indicativa de la mejor jugada.
- **Análisis con motor Stockfish** vía protocolo UCI, tanto sobre la posición detectada como sobre cualquier posición alcanzada navegando el árbol de variantes.
- **Árbol de análisis con variantes y comentarios** por jugada, persistido junto al tablero y navegable en el panel lateral (`AnalysisPanel`, `AnalysisLine`).
- **Gestión de biblioteca**: renombrado, categorización (Aperturas, Finales, Táctica, Estrategia, General) y borrado de libros.
- **Autenticación con usuario/contraseña** (registro y login), contraseñas con hash BCrypt y sesión basada en token portador.
- **Despliegue completo en contenedores** con `docker-compose` (5 servicios: frontend, backend y 3 microservicios de IA) con *healthchecks* encadenados.

---

## Stack tecnológico

| Capa | Tecnología | Detalle verificado en el código |
|---|---|---|
| Backend | Java 21 + Spring Boot 4.0.3, gestionado con Maven | `pom.xml` (`<java.version>21</java.version>`) |
| Backend – Web | `spring-boot-starter-webmvc` | Controladores REST clásicos (`@RestController`) |
| Backend – Persistencia | `spring-boot-starter-data-jpa` + `hibernate-community-dialects` + `sqlite-jdbc` 3.53.2.0 | Solo la entidad `User` está en JPA/SQLite; ver [Base de datos](#base-de-datos) |
| Backend – Identidad | `spring-security-crypto` (solo `BCryptPasswordEncoder`) + filtro de token propio | No se usa Spring Security completo; ver [Arquitectura](#arquitectura) |
| Backend – PDF | Apache PDFBox 3.0.6 + `jbig2-imageio` 3.0.4 | Renderizado de páginas a PNG; `jbig2-imageio` habilita la decodificación de imágenes comprimidas en formato JBIG2 |
| Backend – Utilidades | Lombok, `spring-boot-starter-actuator` | `@Slf4j`, `@Data`; endpoint `/actuator/health` |
| Backend – Testing | JUnit 5, Mockito, OkHttp `mockwebserver` 4.12.0 | Suite de pruebas unitarias y de integración en `backend/src/test/java` |
| Motor de ajedrez | Stockfish (binario externo, protocolo UCI) | `StockfishEngineService` lanza un `Process` y se comunica por stdin/stdout con comandos UCI |
| Frontend | Vue 3.5 (Composition API, `<script setup>`) + TypeScript 5.9 + Vite 8 | `frontend/package.json` |
| Frontend – Ajedrez | `chess.js` 1.4, `chessground` 9.2, `vue3-chessboard` 1.3 | `vue3-chessboard` maneja el renderizado/interacción; `chess.js` se importa también de forma independiente para la lógica de reglas |
| Frontend – HTTP | `axios` 1.13 (vía `apiClient`) y `fetch` nativo (vía `authFetch`) | `httpClient.ts` / `authFetch.ts`; ambos inyectan el token de sesión en cada petición |
| Frontend – Iconografía | FontAwesome (`@fortawesome/*`) | `main.ts` |
| IA – Detección de tablero (MS1) | Python 3.11, FastAPI, Ultralytics YOLOv8, PyTorch/torchvision (build CPU) | `ai-service/ms-board-detector` |
| IA – Segmentación (MS2) | Python 3.11, FastAPI, OpenCV (Sobel + `HoughLinesP`), con *fallback* a rejilla 8×8 | `ai-service/ms-board-segmenter` |
| IA – Clasificación de piezas (MS3) | Python 3.11, FastAPI, PyTorch (ResNet-18, `weights=None`) + heurístico de brillo para el color | `ai-service/ms-piece-classifier` |
| Infraestructura | Docker (multi-stage), `docker-compose`, Nginx (reverse proxy + servidor estático SPA) | `docker-compose.yml`, `frontend/Dockerfile`, `frontend/nginx.conf` |

---

## Arquitectura

### Tres bloques comunicados por API REST/HTTP

El sistema se organiza en tres bloques independientes — **frontend**, **backend** y **microservicios de IA** — que se comunican exclusivamente mediante API REST/HTTP, cada uno desplegado en su propio contenedor Docker.

### Backend: arquitectura hexagonal (Ports & Adapters)

El backend separa estrictamente tres capas, visibles literalmente en los paquetes Java, con una regla de dependencias estricta: la infraestructura depende de la aplicación, y la aplicación depende del dominio, nunca al revés.

```
domain            → Entidades y reglas de negocio puras (sin Spring, sin frameworks)
  ├─ model         Book, ChessFile, ChessBoard, Fen, User, AnalysisNode, BoundingBox...
  ├─ port/in       Casos de uso que el dominio ofrece (interfaces: LoadBookUseCase, AnalyzeBoardUseCase...)
  ├─ port/out      Puertos que el dominio necesita del exterior (BookRepository, EngineService, VisionService...)
  └─ exception     Excepciones de negocio (IllegalPositionException, UnauthenticatedException...)

application       → Implementación de los casos de uso (orquestación, sin detalles de infraestructura)
  ├─ service       BookService, AnalyzeBoardService, EngineAnalysisService, UserService...
  └─ config        Propiedades de configuración tipadas (@ConfigurationProperties)

infrastructure    → Adaptadores concretos (Spring, HTTP, ficheros, procesos externos)
  ├─ adapter/in    Controladores REST, filtro de autenticación
  └─ adapter/out   Persistencia en ficheros/SQLite, cliente Stockfish, clientes HTTP a los MS de IA
```

El dominio se mantiene libre de cualquier dependencia de Spring: las entidades se definen como `record` de Java puro cuyas invariantes se validan en el propio constructor (p. ej. `Fen`, `BoundingBox`). Esto se confirma en `DomainConfig`, que registra manualmente como *bean* de Spring una clase de dominio (`FenLegalityValidator`) sin ninguna anotación de framework, precisamente para mantener el dominio ajeno a Spring.

Algunos patrones de diseño identificables directamente en el código:

| Patrón | Dónde se aplica |
|---|---|
| Ports & Adapters (Hexagonal) | Arquitectura general del backend: dominio, aplicación e infraestructura en paquetes separados |
| Value Object | `BoundingBox` y `Fen`: objetos inmutables que validan sus invariantes en el constructor |
| Repository | `BookRepository` como puerto de dominio, implementado por `ChessFileRepository` en infraestructura |
| DTO | DTOs de persistencia (formato del fichero `.chess`) independientes de los DTOs de respuesta HTTP |
| Proxy | El servidor de desarrollo de Vite redirige `/api/*` hacia el backend, evitando problemas de CORS en desarrollo |
| Composite | `AnalysisNode` (dominio) y `AnalysisLine.vue` (frontend): un nodo y un árbol de nodos comparten interfaz y comportamiento recursivo |
| Singleton | El proceso de Stockfish se mantiene único y compartido por toda la aplicación (`@PostConstruct`/`@PreDestroy`), en vez de arrancar un proceso por petición |

### Pipeline de visión por computador

El reconocimiento de un diagrama de ajedrez se modela como un pipeline de 3 microservicios independientes, cada uno con su propia técnica, contenedor Docker y puerto:

```
Página del PDF (PNG, 150 DPI)
        │
        ▼
┌────────────────────┐   detecta N tableros    ┌──────────────────────┐   64 casillas c/u   ┌────────────────────────┐
│  MS1 · :8001         │ ───────────────────────▶ │  MS2 · :8002           │ ───────────────────▶ │  MS3 · :8003             │
│  board-detector       │                          │  board-segmenter        │                       │  piece-classifier          │
│  YOLOv8                │                          │  Sobel + Hough           │                       │  ResNet-18 + heurístico     │
└────────────────────┘                          └──────────────────────┘                       └────────────────────────┘
                                                                                                          │
                                                                                                          ▼
                                                                                          Backend reconstruye el FEN
```

El backend actúa como orquestador (`AnalyzeBoardService` + `HttpVisionService` + `BoardDetectorClient`/`BoardSegmenterClient`/`PieceClassifierClient`), llamando a cada microservicio vía HTTP multipart y recomponiendo el resultado en notación FEN.

### Frontend: SPA sin enrutador

El frontend es una aplicación Vue de página única que consume exclusivamente la API REST del backend (`/api/**`). No incorpora ningún enrutador (`vue-router` no está entre las dependencias): la navegación entre la pantalla de login/registro y la aplicación principal se gestiona con estado reactivo simple en `App.vue`. Los componentes se agrupan por dominio funcional (`Auth`, `BookList`, `ChessBoard`, `PdfViewer`) en lugar de por tipo técnico. La comunicación con el backend se centraliza en una capa de cliente HTTP (`frontend/src/api`), y el estado de sesión se gestiona mediante un objeto reactivo único expuesto a través de la Composition API (`frontend/src/auth/authState.ts`).

### Gestión de sesión y aislamiento por usuario

El backend no utiliza Spring Security completo; implementa su propio esquema de token portador:

```
Cliente ──Authorization: Bearer <token>──▶ TokenAuthenticationFilter (OncePerRequestFilter)
                                                    │
                                                    ▼ resuelve token → userId
                                          CurrentUserContextHolder (ThreadLocal)
                                                    │
                                                    ▼ leído en cada request
                                              CurrentUserAdapter (implementa CurrentUserPort)
                                                    │
                                                    ▼
                                    Servicios de aplicación (BookService, ChessFileRepository...)
```

El token es un `UUID` aleatorio opaco almacenado en un mapa en memoria (`InMemoryTokenStore`): las sesiones se pierden al reiniciar el backend y ningún token caduca salvo revocación explícita. Cada libro y cada fichero de análisis se resuelven siempre a partir del `userId` extraído del token en el propio backend (`CurrentUserContextHolder`), nunca a partir de un identificador de propietario enviado por el cliente, lo que aísla los datos entre usuarios.

---

## Requisitos

> Deducido de los `Dockerfile`, `pom.xml` y `package.json` del repositorio.

| Componente | Versión requerida | Origen |
|---|---|---|
| Java | 21 | `pom.xml` (`<java.version>21</java.version>`), `Dockerfile` (`eclipse-temurin:21-jdk-alpine`) |
| Maven | 3.9.x (vía *wrapper*, no requiere instalación) | `backend/.mvn/wrapper/maven-wrapper.properties` |
| Node.js | 20.x | `frontend/Dockerfile` (`node:20-alpine`) |
| Python | 3.11 | Dockerfiles de los 3 microservicios (`python:3.11-slim`) |
| Stockfish | Binario UCI compatible | `backend/Dockerfile` (`apt-get install stockfish`) |
| Docker y Docker Compose | Necesario para el despliegue orquestado | `docker-compose.yml` |

**Nota:** para desarrollo local sin Docker en Windows es necesario colocar manualmente un ejecutable de Stockfish en `backend/engines/stockfish.exe` (ruta por defecto en `application.properties`); esa carpeta está excluida de git (`.gitignore`). El binario no se distribuye en el repositorio.

---

## Instalación

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd chessdigitizer
```

### 2. Backend (Java/Maven)

```bash
cd backend
./mvnw dependency:go-offline   # descarga dependencias (usa mvnw.cmd en Windows)
```

Coloca un binario de Stockfish en `backend/engines/stockfish.exe` (Windows) o ajusta la propiedad `app.engine.stockfish-path` en `application.properties` para que apunte a tu instalación de Stockfish.

### 3. Frontend (Node/npm)

```bash
cd frontend
npm ci
```

### 4. Microservicios de IA (Python)

Cada microservicio tiene su propio `requirements.txt` y debe instalarse en un entorno aislado (venv/conda):

```bash
cd ai-service/ms-board-detector && python -m venv .venv && .venv/bin/pip install -r requirements.txt
cd ai-service/ms-board-segmenter && python -m venv .venv && .venv/bin/pip install -r requirements.txt
cd ai-service/ms-piece-classifier && python -m venv .venv && .venv/bin/pip install -r requirements.txt
```

Los pesos de los modelos (`models/chessboard_detection_model.pt`, `models/*.pth`) ya están incluidos en el repositorio como binarios versionados; no requieren descarga ni entrenamiento adicional.

### 5. Alternativa: todo con Docker

Si solo quieres levantar el sistema completo sin instalar nada localmente, ve directamente a la sección [Despliegue](#despliegue).

---

## Variables de entorno

> Tabla construida a partir de `docker-compose.yml`, `application.properties`, `application-prod.properties` y `frontend/.env.production`. No existe ningún archivo `.env.example` en el repositorio.

### Backend

| Variable | Descripción | Valor por defecto (dev) | Valor por defecto (prod/Docker) | Dónde se usa |
|---|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring activo | *(ninguno → `application.properties`)* | `prod` | `docker-compose.yml` |
| `CHESS_ENGINE_PATH` | Ruta al binario de Stockfish | `./engines/stockfish.exe` | `/usr/bin/stockfish` | `application-prod.properties`, `StockfishEngineService` |
| `CHESS_DATA_PATH` | Directorio raíz de datos persistentes (PDFs, `.chess`, SQLite) | `./data` (rutas relativas) | `/data` | `application-prod.properties`, montado como volumen Docker |
| `MS_BOARD_DETECTOR_URL` | URL base del microservicio MS1 | `http://localhost:8001` | `http://ms-board-detector:8001` | `BoardDetectorClient` |
| `MS_BOARD_SEGMENTER_URL` | URL base del microservicio MS2 | `http://localhost:8002` | `http://ms-board-segmenter:8002` | `BoardSegmenterClient` |
| `MS_PIECE_CLASSIFIER_URL` | URL base del microservicio MS3 | `http://localhost:8003` | `http://ms-piece-classifier:8003` | `PieceClassifierClient` |

### Propiedades internas relevantes (configuran comportamiento, no son variables de entorno)

| Propiedad | Valor | Descripción |
|---|---|---|
| `app.render.default-dpi` | `150` | Resolución de renderizado de páginas PDF a imagen |
| `spring.servlet.multipart.max-file-size` / `max-request-size` | `50MB` | Límite de tamaño de subida de PDFs |
| `spring.jpa.hibernate.ddl-auto` | `update` | El esquema de la tabla `users` se autogenera al arrancar |
| `management.endpoints.web.exposure.include` | `health` (solo en prod) | Único endpoint de Actuator expuesto |

### Frontend

| Variable | Descripción | Valor | Dónde se usa |
|---|---|---|---|
| `VITE_API_BASE_URL` | URL base de la API. Vacía en producción porque Nginx hace *proxy* de `/api/` al backend en el mismo origen | `""` (`frontend/.env.production`) → *fallback* a `/api` en código | `httpClient.ts`, `analysisApi.ts` |

---

## Ejecución

### Desarrollo

Se necesitan hasta 5 procesos simultáneos (backend, frontend y los microservicios de IA que se quieran probar):

```bash
# Terminal 1 — Backend (puerto 8080)
cd backend
./mvnw spring-boot:run

# Terminal 2 — Frontend (puerto 5173, con proxy /api → localhost:8080)
cd frontend
npm run dev

# Terminal 3, 4, 5 — Microservicios de IA
cd ai-service/ms-board-detector   && uvicorn app.main:app --port 8001
cd ai-service/ms-board-segmenter  && uvicorn app.main:app --port 8002
cd ai-service/ms-piece-classifier && uvicorn app.main:app --port 8003
```

En desarrollo, el servidor de Vite actúa como *proxy* transparente de `/api/*` hacia `http://localhost:8080` (`vite.config.ts`), evitando problemas de CORS sin configurarlo explícitamente.

### Producción (manual, sin Docker)

```bash
# Backend: empaquetar y ejecutar el JAR con el perfil "prod"
cd backend
./mvnw package -DskipTests
SPRING_PROFILES_ACTIVE=prod CHESS_ENGINE_PATH=/usr/bin/stockfish CHESS_DATA_PATH=/data \
  java -jar target/backend-*.jar

# Frontend: build estático servido por cualquier servidor web
cd frontend
npm run build     # genera frontend/dist/
# servir dist/ con Nginx/Apache, con proxy /api/ hacia el backend
```

### Docker

```bash
docker compose up --build
```

Levanta 5 contenedores (frontend, backend y los 3 microservicios de IA) con *healthchecks* encadenados. Ver [Despliegue](#despliegue) para más detalle.

---

## Flujo de la aplicación

Descripción de extremo a extremo, basada en el código de componentes y controladores, de lo que ocurre desde que un usuario abre la aplicación hasta que analiza una posición:

1. **Autenticación.** `App.vue` muestra `LoginView` o `RegisterView` si no hay sesión. El registro (`POST /api/auth/register`) hashea la contraseña con BCrypt. El login (`POST /api/auth/login`) devuelve un token `UUID` opaco que el frontend guarda en `localStorage`.

2. **Importación de un libro.** Desde `BookList`, el usuario abre `ImportBookModal`, sube un PDF y le da un título. `POST /api/books` guarda el PDF, cuenta sus páginas con PDFBox, y crea un registro `Book` y un fichero `.chess` asociado, identificados por UUID, sin modificar nunca el PDF original.

3. **Navegación de páginas.** `PdfViewer` pide el `ChessFile` completo (`GET /api/books/{id}/chess`) y la imagen de la página actual (`GET /api/books/{id}/pages/{n}`, rasterizada a PNG por `LocalPdfRenderer`).

4. **Análisis de página (detección de tableros).** Al pulsar "Analizar página", `POST /api/books/{id}/pages/{n}/analyze` dispara en `AnalyzeBoardService`:
   - Renderizado de la página a imagen.
   - Envío a **MS1** (`/detect`), que devuelve las cajas delimitadoras de cada tablero detectado (YOLOv8).
   - Por cada tablero, recorte y envío a **MS2** (`/segment`), que devuelve 64 imágenes de casilla, ordenadas de `a8` a `h1` (con *fallback* a rejilla uniforme si la detección de líneas falla).
   - Por cada casilla, envío a **MS3** (`/classify`), que devuelve tipo de pieza y color.
   - Reconstrucción del FEN recorriendo el array de 64 clasificaciones en ese mismo orden, contando casillas vacías consecutivas por fila.
   - Persistencia del `ChessFile` actualizado.
   - El frontend dibuja un recuadro clicable (`BoardOverlay`) sobre cada tablero detectado, posicionado proporcionalmente sobre la imagen de la página.

5. **Selección y visualización de un tablero.** Al hacer clic en un `BoardOverlay`, `ChessBoardPanel` valida la legalidad del FEN (`validateFenLegality`, réplica en TypeScript de `FenLegalityValidator`) antes de intentar renderizarlo con `chessground`, ya que el pipeline de reconocimiento puede producir un FEN sintácticamente válido pero ajedrecísticamente imposible (por ejemplo, dos reyes del mismo color).

6. **Corrección manual (si aplica).** Si la posición es ilegal, el usuario pulsa "Comenzar revisión"; el FEN se sanea (`sanitizeFenForLoading`, garantiza exactamente un rey por color, precondición interna de `chessground`) y se activa la selección de casilla. Cada cambio revalida en vivo; "Comprobar" llama a `PATCH /api/books/{bookId}/boards/{boardId}/fen`, que reconstruye y valida el `Fen` en el backend (fuente de verdad) antes de persistirlo.

7. **Análisis con motor.** Si la posición es legal, `POST /api/books/{bookId}/boards/{boardId}/engine/analyze` delega en `StockfishEngineService`, que reutiliza un único proceso Stockfish vivo durante toda la ejecución del backend y persiste la evaluación resultante. El resultado alimenta la barra de evaluación y, si el usuario lo activa, una flecha con la mejor jugada.

8. **Juego de variantes y comentarios.** Cada jugada dispara en paralelo `POST .../analysis/moves` (que añade el nodo al árbol `AnalysisNode`, persistido en el fichero `.chess` del libro) y un nuevo análisis de motor sobre la posición resultante. Navegar por el árbol reconstruye la posición con `chess.js` en el propio cliente, sin llamada adicional al backend. Los comentarios se editan con `CommentPopUp` y se persisten con `PATCH .../analysis/comment`.

9. **Gestión de biblioteca.** El usuario puede renombrar, recategorizar o eliminar un libro desde `BookList` en cualquier momento.

---

## Base de datos

El sistema combina **dos mecanismos de persistencia distintos** según la naturaleza de cada dato:

### 1. SQLite + JPA/Hibernate — únicamente para credenciales de usuario

- **Motor:** SQLite (`sqlite-jdbc` + `hibernate-community-dialects`).
- **Fichero:** `data/users.db` (dev) / `${CHESS_DATA_PATH}/users.db` (prod).
- **Esquema generado automáticamente** por Hibernate (`spring.jpa.hibernate.ddl-auto=update`).

```
Tabla: users
┌────────────────┬──────────┬──────────────────────────────┐
│ Columna         │ Tipo      │ Restricciones                 │
├────────────────┼──────────┼──────────────────────────────┤
│ id              │ UUID (PK) │ NOT NULL                      │
│ username        │ VARCHAR   │ NOT NULL, UNIQUE               │
│ password_hash   │ VARCHAR   │ NOT NULL (BCrypt)              │
└────────────────┴──────────┴──────────────────────────────┘
```
(`UserEntity`, `UserJpaRepository`)

### 2. Ficheros JSON — libros, tableros y árboles de análisis

No existen tablas relacionales para `Book`, `ChessBoard` ni `AnalysisNode`. `ChessFileRepository` (adaptador de `BookRepository`) serializa/deserializa cada libro como un fichero JSON independiente usando Jackson:

```
data/
├── users.db
├── books/<ownerId>/<bookId>.pdf        # PDF original subido por el usuario (nunca se modifica)
└── chess/<ownerId>/<bookId>.chess      # JSON serializado de ChessFileDTO
```

Estructura conceptual de un fichero `.chess` (`ChessFileDTO` / `ChessBoardDTO` / `AnalysisNodeDTO`):

```json
{
  "id": "uuid",
  "title": "My System",
  "originalFilename": "my-system.pdf",
  "totalPages": 320,
  "category": "ESTRATEGIA",
  "ownerId": "uuid-del-usuario",
  "boards": [
    {
      "id": "board-p12-1",
      "page": 12,
      "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w - - 0 1",
      "boundingBox": { "x": 120.5, "y": 340.2, "width": 200, "height": 200 },
      "analysis": {
        "move": null, "comment": "", "evalCp": 45,
        "children": [ { "move": "e4", "comment": "Apertura española", "evalCp": 30, "children": [] } ]
      }
    }
  ]
}
```

Esta separación evita modelar en una base relacional una estructura arbórea de profundidad variable (el árbol de análisis), mientras reserva las garantías de integridad referencial de una base de datos relacional (unicidad de `username`) para los datos de usuario. El aislamiento entre usuarios se resuelve siempre con el `userId` extraído del token de sesión (`CurrentUserContextHolder`), nunca con un identificador proporcionado por el cliente.

---

## API

Todas las rutas cuelgan de `/api` salvo el *health check* de Actuator.

### Autenticación (`AuthController`)

| Método | Ruta | Body | Respuesta éxito | Errores |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | `{ "username": string, "password": string }` | `201` `{ id, username }` | `409` username duplicado · `400` contraseña inválida |
| `POST` | `/api/auth/login` | `{ "username": string, "password": string }` | `200` `{ "token": string }` | `401` credenciales inválidas |

### Libros (`BookController`)

| Método | Ruta | Body/Params | Respuesta | Notas |
|---|---|---|---|---|
| `POST` | `/api/books` | multipart `file` + `title` | `201` `Book` | Sube el PDF y crea el libro |
| `GET` | `/api/books` | — | `200` `Book[]` | Solo libros del usuario autenticado |
| `GET` | `/api/books/{id}` | — | `200` `Book` / `404` | |
| `DELETE` | `/api/books/{id}` | — | `204` | Borra PDF + registro |
| `GET` | `/api/books/{id}/chess` | — | `200` `ChessFileResponse` / `404` | Incluye todos los tableros detectados |
| `PATCH` | `/api/books/{id}/title` | `{ "title": string }` | `200` `Book` | |
| `PATCH` | `/api/books/{id}/category` | `{ "category": BookCategory }` | `200` `Book` | `APERTURAS \| FINALES \| TACTICA \| ESTRATEGIA \| GENERAL` |
| `PATCH` | `/api/books/{bookId}/boards/{boardId}/fen` | `{ "fen": string }` | `204` / `422` | Valida legalidad antes de persistir |

### Páginas y análisis de tableros (`PdfController`, `AnalyzeController`)

| Método | Ruta | Respuesta | Notas |
|---|---|---|---|
| `GET` | `/api/books/{id}/pages/{pageNumber}` | `200` `image/png` | Renderizado con PDFBox |
| `POST` | `/api/books/{id}/pages/{pageNumber}/analyze` | `200` `ChessFileResponse` | Ejecuta el pipeline MS1→MS2→MS3 completo |

### Árbol de análisis (`AnalysisController`)

| Método | Ruta | Body | Respuesta | Notas |
|---|---|---|---|---|
| `POST` | `/api/books/{bookId}/boards/{boardId}/analysis/moves` | `{ "path": string[], "move": string }` | `200` `AnalysisNodeResponse` | `path` es la secuencia SAN hasta el nodo padre |
| `PATCH` | `/api/books/{bookId}/boards/{boardId}/analysis/comment` | `{ "path": string[], "comment": string }` | `200` `AnalysisNodeResponse` | |

### Motor de ajedrez (`EngineController`)

| Método | Ruta | Body/Params | Respuesta |
|---|---|---|---|
| `POST` | `/api/books/{bookId}/boards/{boardId}/engine/analyze?moveTimeMs=1000` | — | `200` `{ evalCp, formattedEval, bestMove }` |
| `POST` | `/api/engine/analyze-fen?moveTimeMs=1000` | `{ "fen": string }` | `200` `{ evalCp, formattedEval, bestMove }` / `422` si el FEN es ilegal |

### Observabilidad

| Método | Ruta | Notas |
|---|---|---|
| `GET` | `/actuator/health` | Único endpoint de Actuator expuesto en producción; usado por el `healthcheck` de `docker-compose.yml` |

### Microservicios de IA (uso interno)

| Servicio | Endpoint | Entrada | Salida |
|---|---|---|---|
| MS1 (`:8001`) | `GET /health` | — | `{ "status": "ok" }` |
| MS1 (`:8001`) | `POST /detect` | multipart `file` (imagen) | `{ "detections": [{ x, y, w, h, confidence }] }` |
| MS2 (`:8002`) | `GET /health` | — | `{ "status": "ok", "service": "ms-board-segmenter" }` |
| MS2 (`:8002`) | `POST /segment` | multipart `file` (imagen de tablero) | `{ "method": "hough"\|"fallback", "cells": [{ square, row, col, image_base64 }] }` |
| MS3 (`:8003`) | `GET /health` | — | `{ "status": "ok" }` |
| MS3 (`:8003`) | `POST /classify` | multipart `file` (imagen de casilla) | `{ "piece", "color", "confidence" }` |

---

## Estructura del código

```
chessdigitizer/
├── backend/                                    # API REST — Spring Boot 4 (Java 21), arquitectura hexagonal
│   ├── src/main/java/com/chessdigitizer/backend/
│   │   ├── domain/
│   │   │   ├── model/                          # Book, ChessFile, ChessBoard, Fen, BoundingBox, User,
│   │   │   │                                    #   AnalysisNode, EngineAnalysis, FenLegalityValidator...
│   │   │   ├── port/in/                        # UseCases: LoadBookUseCase, AnalyzeBoardUseCase,
│   │   │   │                                    #   UpdateBoardFenUseCase, AnalyzePositionUseCase...
│   │   │   ├── port/out/                       # BookRepository, VisionService, EngineService,
│   │   │   │                                    #   PdfRenderer, UserRepository, TokenIssuer, CurrentUserPort...
│   │   │   └── exception/                      # IllegalPositionException, UnauthenticatedException...
│   │   ├── application/
│   │   │   ├── service/                        # Implementaciones de los casos de uso
│   │   │   └── config/                         # GlobalProperties (render/storage/vision/engine)
│   │   ├── infrastructure/
│   │   │   ├── adapter/in/                     # BookController, AnalyzeController, AnalysisController,
│   │   │   │   ├── security/                    #   EngineController, AuthController, PdfController
│   │   │   │   └── response/                    # TokenAuthenticationFilter / DTOs de respuesta
│   │   │   ├── adapter/out/                     # ChessFileRepository (persistencia JSON), LocalPdfRenderer
│   │   │   │   ├── DTO/                          #   DTOs de serialización de los ficheros .chess
│   │   │   │   ├── engine/                       #   StockfishEngineService (proceso UCI)
│   │   │   │   ├── persistence/                  #   UserEntity/UserJpaRepository/UserRepositoryAdapter (SQLite)
│   │   │   │   ├── security/                     #   BCrypt, InMemoryTokenStore, CurrentUserContextHolder
│   │   │   │   └── vision/                       #   Clientes HTTP a los 3 microservicios de IA
│   │   │   └── config/                          # CorsConfig, AppConfig (crea directorios), DomainConfig
│   │   └── BackendApplication.java              # Punto de entrada
│   ├── src/test/java/...                        # Pruebas unitarias y de integración (JUnit 5, Mockito, MockMvc)
│   ├── src/main/resources/
│   │   ├── application.properties               # Perfil de desarrollo (rutas relativas, puertos localhost)
│   │   └── application-prod.properties          # Perfil "prod" activado en Docker (variables de entorno)
│   ├── data/                                     # [runtime, no versionado] PDFs, .chess, users.db
│   ├── engines/                                  # [runtime, no versionado] binario local de Stockfish
│   ├── Dockerfile                                # Multi-stage: Maven (build) → JRE 21 + Stockfish (apt) + gosu
│   ├── docker-entrypoint.sh                      # Crea /data, ajusta permisos, baja privilegios
│   └── pom.xml
│
├── frontend/                                     # SPA — Vue 3 + TypeScript + Vite
│   ├── src/
│   │   ├── api/                                  # httpClient.ts (axios), authFetch.ts (fetch),
│   │   │                                          #   authApi.ts, bookApi.ts, analysisApi.ts
│   │   ├── auth/authState.ts                     # Estado de sesión reactivo (token/usuario en localStorage)
│   │   ├── chess/fenLegality.ts                  # Réplica TS del validador de legalidad FEN del backend
│   │   ├── components/
│   │   │   ├── Auth/                             # LoginView.vue, RegisterView.vue
│   │   │   ├── BookList/                         # BookList.vue, ImportBookModal.vue
│   │   │   ├── ChessBoard/                        # ChessBoardPanel.vue, AnalysisPanel.vue,
│   │   │   │                                      #   AnalysisLine.vue, CommentPopUp.vue
│   │   │   └── PdfViewer/                         # PdfViewer.vue, BoardOverlay.vue
│   │   ├── types/chess.types.ts                  # Contrato de tipos con la API
│   │   ├── App.vue                                # Layout raíz (sidebars + resizer + enrutado manual)
│   │   └── main.ts                                # Bootstrap de la app y registro de iconos FontAwesome
│   ├── public/icons.svg
│   ├── Dockerfile                                 # Multi-stage: Node 20 (build) → Nginx 1.25 (serve)
│   ├── nginx.conf                                 # Proxy /api/ → backend:8080, fallback SPA, caché estáticos
│   ├── vite.config.ts                             # Proxy /api → localhost:8080 en dev
│   └── tsconfig*.json
│
├── ai-service/                                    # Microservicios de visión por computador (Python + FastAPI)
│   ├── ms-board-detector/                          # MS1 · puerto 8001 · YOLOv8
│   │   ├── app/{main.py,detector.py}
│   │   ├── models/chessboard_detection_model.pt    # [binario]
│   │   └── Dockerfile
│   ├── ms-board-segmenter/                         # MS2 · puerto 8002 · OpenCV (Sobel + Hough)
│   │   ├── app/{main.py,segmenter.py}
│   │   ├── tests/test_health.py                    # Test automatizado del endpoint /health
│   │   ├── pytest.ini
│   │   └── Dockerfile
│   └── ms-piece-classifier/                        # MS3 · puerto 8003 · ResNet-18 + heurístico de color
│       ├── app/{main.py,classifier.py,model.py,color_detector.py}
│       ├── models/*.pth                            # [binarios]
│       └── Dockerfile
│
├── docker-compose.yml                              # Orquesta los 5 servicios con healthchecks encadenados
├── .gitignore
└── README.md
```

### Responsabilidad de cada carpeta

| Carpeta | Responsabilidad |
|---|---|
| `backend/.../domain/model` | Entidades y *Value Objects* inmutables (en su mayoría `record` de Java) que validan sus invariantes en el constructor. `AnalysisNode` es la única clase mutable del dominio, y representa el árbol de variantes. |
| `backend/.../domain/port/in` | Interfaces de casos de uso — el contrato que expone el dominio hacia el exterior. |
| `backend/.../domain/port/out` | Interfaces que el dominio necesita del exterior — implementadas por `infrastructure/adapter/out`. |
| `backend/.../domain/exception` | Excepciones de negocio, mapeadas a códigos HTTP en `GlobalExceptionHandler`. |
| `backend/.../application/service` | Orquestación de casos de uso: coordina puertos de dominio sin conocer detalles de Spring/HTTP/ficheros. |
| `backend/.../application/config` | `GlobalProperties`, con subclases `@ConfigurationProperties` (render, storage, vision, engine). |
| `backend/.../infrastructure/adapter/in` | Controladores REST y el filtro de autenticación. Traducen HTTP ↔ casos de uso. |
| `backend/.../infrastructure/adapter/out` | Implementaciones concretas de los puertos: ficheros JSON (`ChessFileRepository`), proceso Stockfish, clientes HTTP a los MS de IA, JPA/SQLite para usuarios, hashing BCrypt, *token store* en memoria. |
| `backend/.../infrastructure/config` | `CorsConfig`, `AppConfig` (crea directorios de almacenamiento al arrancar), `DomainConfig` (registra *beans* de dominio puro). |
| `frontend/src/api` | Capa de acceso a la API REST. Conviven dos clientes HTTP: `axios` (`httpClient.ts`, `bookApi.ts`, `authApi.ts`) y `fetch` nativo (`analysisApi.ts`, vía `authFetch.ts`), cada uno con su propia lógica de inyección de token y gestión de sesión expirada. |
| `frontend/src/auth` | Estado de sesión reactivo (token/usuario) persistido en `localStorage`. |
| `frontend/src/chess` | Validación de legalidad FEN del lado del cliente (réplica del backend) y saneado de FEN para `chessground`. |
| `frontend/src/components/Auth` | Pantallas de login/registro. |
| `frontend/src/components/BookList` | Listado, filtro por categoría, edición inline de título/categoría, importación (`ImportBookModal`, con *drag & drop*). |
| `frontend/src/components/PdfViewer` | Visor paginado con zoom y overlay de tableros detectados posicionado proporcionalmente sobre la imagen renderizada. |
| `frontend/src/components/ChessBoard` | El componente más complejo del frontend: tablero interactivo, barra de evaluación, flujo de corrección manual de piezas, árbol de variantes recursivo (`AnalysisLine`). |
| `frontend/src/types` | Contrato TypeScript compartido con las respuestas del backend. |
| `ai-service/ms-board-detector/app` | `main.py` (API FastAPI que carga el modelo una vez al arrancar) y `detector.py` (envoltorio sobre `ultralytics.YOLO`). |
| `ai-service/ms-board-segmenter/app` | `main.py` (API) y `segmenter.py` (pipeline de visión clásica: bordes Sobel, líneas Hough, regularización de la rejilla, *fallback* a rejilla uniforme). |
| `ai-service/ms-piece-classifier/app` | `main.py` (API), `model.py` (arquitectura ResNet-18 sin pesos preentrenados), `classifier.py` (inferencia), `color_detector.py` (heurístico de brillo del núcleo de la pieza). |

---

## Dependencias importantes

| Dependencia | Por qué existe |
|---|---|
| **SQLite** (vía Spring Data JPA) | Persistencia de credenciales de usuario sin necesidad de levantar ni administrar un servidor de base de datos independiente; coherente con el resto del sistema, que ya usa ficheros locales para libros y análisis. |
| **Ficheros JSON `.chess`** (Jackson) | Modelan de forma natural una estructura arbórea de profundidad variable (el árbol de variantes de análisis), sin necesidad de un esquema relacional. |
| **Apache PDFBox + `jbig2-imageio`** | PDFBox renderiza páginas PDF a imagen; `jbig2-imageio` añade soporte para decodificar imágenes comprimidas en formato JBIG2 (habitual en escaneos en blanco y negro), que PDFBox no incluye por defecto. |
| **`chess.js` importado de forma independiente de `vue3-chessboard`** | Aunque `vue3-chessboard` integra `chess.js` internamente, el editor de análisis y la integración con Stockfish necesitan manipular el estado de la partida (generar FEN, comprobar si una posición es terminal) sin depender de que exista un tablero visual instanciado. |
| **`chessground` (vía `vue3-chessboard`)** | Renderizado del tablero interactivo con soporte de arrastrar y soltar; exige como precondición interna exactamente un rey por color, de ahí la función de saneado `sanitizeFenForLoading` en el frontend. |
| **YOLOv8 (Ultralytics)** | Detección de la posición de los diagramas dentro de la página del PDF; se ejecuta sobre CPU dentro del contenedor Docker. |
| **OpenCV** | Detección de bordes (Sobel) y líneas (transformada de Hough) para segmentar la cuadrícula de 8×8 casillas de un tablero ya localizado. |
| **PyTorch / torchvision** | Define y ejecuta en inferencia la red ResNet-18 usada para clasificar el tipo de pieza de cada casilla. |
| **Stockfish** | Motor de análisis externo, integrado como proceso del sistema operativo controlado mediante el protocolo estándar UCI, en vez de una librería embebida. |
| **BCrypt (`spring-security-crypto`)** | Hashing de contraseñas de usuario, sin incorporar el resto del aparato de Spring Security. |
| **Lombok** | Reduce código repetitivo (getters/setters, logger) en clases de infraestructura y aplicación. |
| **Docker / docker-compose** | Empaqueta cada componente (backend, frontend, 3 microservicios) en contenedores aislados que se comunican por una red Docker interna. |

---

## Despliegue

El mecanismo de despliegue soportado por el repositorio es **Docker y docker-compose**, con cada componente ejecutándose en su propio contenedor aislado:

```bash
docker compose up --build -d
```

Detalles relevantes extraídos de `docker-compose.yml` y los `Dockerfile`:

- **Backend:** build multi-stage — `eclipse-temurin:21-jdk-alpine` compila con el *wrapper* de Maven; el runtime final es una imagen basada en Debian (no Alpine, porque Stockfish se instala vía `apt-get`) con Stockfish incluido. Se ejecuta con un usuario no root usando `gosu` en `docker-entrypoint.sh`.
- **Frontend:** build multi-stage — `node:20-alpine` compila con `npm ci && npm run build`; el runtime final es `nginx:1.25-alpine` sirviendo los estáticos y actuando de *reverse proxy* de `/api/` hacia el contenedor `backend`.
- **Microservicios de IA:** cada uno es una imagen `python:3.11-slim` independiente con las librerías gráficas mínimas de sistema necesarias para OpenCV/Pillow, ejecutando como usuario no root.
- **Arranque ordenado:** `depends_on` con condición `service_healthy` garantiza que el `backend` no arranca hasta que los 3 microservicios de IA respondan `/health`, y que el `frontend` no arranca hasta que el `backend` responda `/actuator/health`.
- **Persistencia:** volumen nombrado `chess-data` montado en `/data` del contenedor `backend`.
- **Red:** todos los servicios comparten una red *bridge* (`chessreader-net`); los 3 microservicios de IA no publican puertos al host (`expose`, no `ports`), solo son alcanzables desde `backend` dentro de la red interna.

Tras el arranque, la aplicación queda disponible en `http://localhost` (puerto 80, servido por el contenedor `frontend`).
