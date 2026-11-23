# BackendUfinet · API REST de Ofertas

API reactiva para **crear ofertas, calcular su precio (MRC) y presentarlas**, construida con arquitectura hexagonal (puertos y adaptadores) sobre Spring Boot 3, WebFlux, JPA/Hibernate y H2 en memoria.

---

## 1. Especificaciones técnicas

| Aspecto | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5 (WebFlux, Data JPA, Validation, Actuator) |
| Servidor | Netty (no bloqueante) · puerto **8080** |
| Persistencia | Spring Data JPA + Hibernate 6 |
| Base de datos | H2 en memoria (`jdbc:h2:mem:ufinetdb`), esquema en `db/schema.sql` |
| Documentación | springdoc-openapi (Swagger UI) |
| Errores | `ProblemDetail` (RFC 9457) con formato homogéneo |
| Build | Maven (incluye Maven Wrapper `mvnw`) |
| Pruebas | JUnit 5, AssertJ, Mockito, Reactor `StepVerifier`, `WebTestClient` |
| Cobertura | JaCoCo (umbral mínimo de líneas: 80 % en `mvn verify`) |

> **WebFlux + JPA.** JPA es bloqueante; el adaptador de persistencia ejecuta cada operación dentro de una transacción (`TransactionTemplate`) en un *scheduler* elástico dedicado (`jpaScheduler`). Así los hilos del *event loop* de Netty nunca se bloquean.

---

## 2. Instalación y ejecución

### Requisitos
- JDK 21 (`java -version`)
- Maven 3.9+ o el wrapper incluido (`mvnw` / `mvnw.cmd`), que descarga Maven automáticamente
- (Opcional) Docker Desktop

### Ejecutar en local
```bash
# Windows
mvnw.cmd spring-boot:run
# Linux / macOS
./mvnw spring-boot:run
```

O bien empaquetar y ejecutar el JAR:
```bash
mvnw.cmd clean package
java -jar target/backend-ufinet.jar
```

### Ejecutar con Docker (backend + frontend)
```bash
docker compose up --build
```
El `docker-compose.yml` de esta carpeta levanta el backend (8080) y el frontend de `../FrontendUfinet` (5173).

### URLs útiles
| Recurso | URL |
|---|---|
| API | http://localhost:8080/api/offers |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |
| Health | http://localhost:8080/actuator/health |

### Variables de entorno
| Variable | Por defecto | Uso |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Orígenes permitidos por CORS (solo el frontend) |
| `SPRING_SQL_INIT_DATA_LOCATIONS` | — | Ponla en `classpath:db/sample-data.sql` para cargar datos de ejemplo |

---

## 3. Arquitectura hexagonal

```
              ┌──────────────── infrastructure ────────────────┐
  HTTP ──▶    │  adapter/in/web  (OfferController, DTOs,        │
              │                   GlobalExceptionHandler)        │
              │        │ usa puertos de entrada                  │
              │  ┌─────▼──────────── application ─────────────┐ │
              │  │ port/in  (CreateOffer, ListOffers,          │ │
              │  │           GetOffer, PresentOffer UseCases)  │ │
              │  │ service  OfferApplicationService            │ │
              │  │ port/out OfferRepositoryPort                │ │
              │  │  ┌──────────────── domain ───────────────┐  │ │
              │  │  │ model    Offer, OfferedService,       │  │ │
              │  │  │          ContractTerm, enums          │  │ │
              │  │  │ service  PricingPolicy (regla precio) │  │ │
              │  │  │          PresentationPolicy (regla    │  │ │
              │  │  │          presentación), OfferFactory  │  │ │
              │  │  │ exception                            │  │ │
              │  │  └───────────────────────────────────────┘  │ │
              │  └─────────────────────┬───────────────────────┘ │
              │  adapter/out/persistence  (OfferPersistenceAdapter,│
              │   entidades JPA, Spring Data, mapper)  ──▶ H2    │
              │  config (beans, CORS, OpenAPI)                    │
              └──────────────────────────────────────────────────┘
```

- **Dominio** (`domain`): Java puro, sin Spring ni JPA. Contiene las reglas de negocio:
  - `PricingPolicy`: regla de precio `MRC = mbps × 2,50 × (1 − descuento)` con redondeo `HALF_UP` a 2 decimales.
  - `PresentationPolicy`: regla de presentación (`PRESENTADA` / `NO_VIABLE`; 409 si no está en `BORRADOR`).
  - `OfferFactory`: invariantes del modelo (cliente, 1–5 servicios, Mbps 10–10.000, plazo 12/24/36). Reúne todos los errores encontrados.
  - `Offer` es inmutable: presentar devuelve una nueva instancia.
- **Aplicación** (`application`): casos de uso (puertos de entrada) y el servicio que los orquesta. Solo depende de los puertos.
- **Infraestructura** (`infrastructure`): adaptadores de entrada (REST/WebFlux) y de salida (JPA/Hibernate), y la configuración que ensambla las capas (`BeanConfiguration`).

### Estructura de carpetas
```
src/main/java/com/ufinet/offers
├── domain/{model, service, exception}
├── application/{port/in, port/out, service}
└── infrastructure
    ├── adapter/in/web/{dto, mapper, validation, handler}
    ├── adapter/out/persistence/{entity, repository, mapper}
    └── config
src/main/resources
├── application.yml
└── db/{schema.sql, sample-data.sql}
```

---

## 4. Modelo de datos y script de base de datos

`src/main/resources/db/schema.sql` crea las tablas al arrancar (Hibernate no genera DDL: `ddl-auto: none`).

| Tabla | Columna | Tipo | Regla |
|---|---|---|---|
| `oferta` | `id` | BIGINT identity | PK autogenerada |
| | `cliente` | VARCHAR(100) | Obligatorio |
| | `estado` | VARCHAR(20) | `BORRADOR`, `PRESENTADA`, `NO_VIABLE` |
| | `version` | BIGINT | `@Version` (bloqueo optimista) |
| `servicio` | `id` | BIGINT identity | PK |
| | `oferta_id` | BIGINT | FK → `oferta.id` (1 oferta : 1..5 servicios) |
| | `tipo` | VARCHAR(20) | `INTERNET`, `CAPACIDAD` |
| | `mbps` | INTEGER | 10 – 10.000 |
| | `plazo_meses` | INTEGER | 12, 24, 36 |
| | `viable` | BOOLEAN | Lo envía el cliente |
| | `mrc` | DECIMAL(12,2) | Calculado por el backend |

`db/sample-data.sql` contiene datos de ejemplo opcionales.

---

## 5. Reglas de negocio

**Regla de precio (MRC mensual, USD)**: `MRC = mbps × 2,50 × (1 − descuento)`; descuento 12 m = 0 %, 24 m = 5 %, 36 m = 10 %; `HALF_UP` a 2 decimales.
Ejemplo: 100 Mbps a 36 m = **225,00**; 50 Mbps a 24 m = **118,75**; total = **343,75**.

**Regla de presentación**
- Algún servicio con `viable = false` → `NO_VIABLE`.
- Todos viables → `PRESENTADA`.
- Si ya no está en `BORRADOR` → **409** y no cambia nada.
- Dos presentaciones simultáneas de la misma oferta: la segunda falla por bloqueo optimista (`@Version`) → **409**.

---

## 6. Endpoints

| Método y ruta | Hace | Respuestas |
|---|---|---|
| `POST /api/offers` | Crea la oferta en `BORRADOR` y calcula el MRC de cada servicio | **201** con la oferta (y cabecera `Location`); **400** si falla una validación |
| `GET /api/offers` | Lista las ofertas con `id`, `cliente`, `estado` y `mrcTotal` | **200** |
| `GET /api/offers/{id}` | Devuelve la oferta con sus servicios y `mrcTotal` | **200**; **404** si no existe |
| `POST /api/offers/{id}/present` | Aplica la regla de presentación | **200** con la oferta; **404**; **409** si no está en `BORRADOR` |

### Ejemplo
```bash
curl -i -X POST http://localhost:8080/api/offers \
  -H "Content-Type: application/json" \
  -d '{
    "cliente": "Cliente Demo S.A.",
    "servicios": [
      { "tipo": "INTERNET",  "mbps": 100, "plazoMeses": 36, "viable": true },
      { "tipo": "CAPACIDAD", "mbps": 50,  "plazoMeses": 24, "viable": true }
    ]
  }'
```
Respuesta `201`:
```json
{
  "id": 1, "cliente": "Cliente Demo S.A.", "estado": "BORRADOR", "version": 0,
  "servicios": [
    { "id": 1, "tipo": "INTERNET",  "mbps": 100, "plazoMeses": 36, "viable": true, "mrc": 225.00 },
    { "id": 2, "tipo": "CAPACIDAD", "mbps": 50,  "plazoMeses": 24, "viable": true, "mrc": 118.75 }
  ],
  "mrcTotal": 343.75
}
```
```bash
curl -X POST http://localhost:8080/api/offers/1/present   # 200 -> "estado": "PRESENTADA"
curl -X POST http://localhost:8080/api/offers/1/present   # 409
```

---

## 7. Manejo de errores

Todos los errores devuelven `application/problem+json` con el **mismo formato**:

```json
{
  "type": "urn:ufinet:problem:validation-error",
  "title": "Solicitud inválida",
  "status": 400,
  "detail": "La solicitud contiene errores de validación",
  "instance": "/api/offers",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-11-22T20:15:00Z",
  "errors": [
    { "field": "cliente", "message": "El cliente es obligatorio" },
    { "field": "servicios[0].mbps", "message": "Los Mbps deben estar entre 10 y 10000" }
  ]
}
```

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Bean Validation del DTO o invariantes del dominio (`errors[]` con la ruta de cada campo) |
| 400 | `MALFORMED_REQUEST` | JSON mal formado, tipo/enum no válido (p. ej. `"tipo":"VOZ"`), id no numérico |
| 404 | `OFFER_NOT_FOUND` | `No existe una oferta con id {id}` |
| 409 | `INVALID_OFFER_STATE` | Presentar una oferta que no está en `BORRADOR` |
| 409 | `CONCURRENT_MODIFICATION` | Conflicto de bloqueo optimista |
| 4xx | `HTTP_ERROR` | Otros errores HTTP (415, 405…) |
| 500 | `INTERNAL_ERROR` | Error inesperado (no expone detalles internos; se registra en el log) |

Mensajes de validación:

| Campo | Mensaje |
|---|---|
| `cliente` | `El cliente es obligatorio` · `El cliente no puede superar 100 caracteres` |
| `servicios` | `La oferta debe tener entre 1 y 5 servicios` |
| `servicios[i].tipo` | `El tipo es obligatorio (INTERNET o CAPACIDAD)` |
| `servicios[i].mbps` | `Los Mbps son obligatorios` · `Los Mbps deben estar entre 10 y 10000` |
| `servicios[i].plazoMeses` | `El plazo es obligatorio` · `El plazo debe ser 12, 24 o 36 meses` |
| `servicios[i].viable` | `El indicador viable es obligatorio` |

---

## 8. Pruebas unitarias

```bash
mvnw.cmd test          # ejecuta las pruebas y genera target/site/jacoco/index.html
mvnw.cmd verify        # además valida la cobertura mínima (80 % de líneas)
```

**Tests mínimos del enunciado**

| # | Prueba | Clase |
|---|---|---|
| 1 | MRC para los tres plazos (225,00 · 118,75 · total 343,75) | `PricingPolicyTest.calculatesMrcForTheThreeTerms` |
| 2 | Presentar con todos viables → `PRESENTADA` | `PresentationPolicyTest.allViableServicesResultInPresented` |
| 3 | Presentar con uno no viable → `NO_VIABLE` | `PresentationPolicyTest.oneNonViableServiceResultsInNotViable` |
| 4 | Presentar una oferta ya presentada → 409 | `OfferControllerTest.conflictWhenNotDraft`, `OfferApiIntegrationTest.fullFlowPresented` |

**Resumen por capa (≈109 casos)**

| Capa | Clase | Qué cubre |
|---|---|---|
| Dominio | `PricingPolicyTest` | Regla de precio, descuentos, redondeo `HALF_UP` |
| Dominio | `PresentationPolicyTest` | Regla de presentación, 409 de dominio, inmutabilidad |
| Dominio | `OfferFactoryTest` | Todas las validaciones (cliente, 1–5 servicios, Mbps, plazo, nulos) |
| Dominio | `OfferTest`, `ContractTermTest` | Total MRC, viabilidad, plazos y descuentos |
| Aplicación | `OfferApplicationServiceTest` | Casos de uso con el puerto simulado (Mockito + `StepVerifier`) |
| Web | `OfferControllerTest` (`@WebFluxTest`) | Códigos 201/200/400/404/409/415/500 y formato `ProblemDetail` |
| Web | `OfferWebMapperTest`, `ContractTermValidatorTest`, `ProblemDetailFactoryTest`, `GlobalExceptionHandlerTest` | Mapeos, validador del plazo, construcción de errores |
| Persistencia | `OfferPersistenceAdapterIntegrationTest` | JPA/H2, versión y bloqueo optimista |
| Extremo a extremo | `OfferApiIntegrationTest` | Flujo completo crear → consultar → listar → presentar → 409, CORS, OpenAPI |
