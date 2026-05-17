# Building a Jmix-Like Spring Boot Project — Step-by-Step Roadmap

> **Goal:** A practical order for building a Spring Boot backend that replicates Jmix's developer experience.
> Each phase delivers working software you can run, test, and build on.

---

## Phase 0 — Decisions Before You Code

Before opening your IDE, lock these down:

| Decision | Recommendation | Why |
|---|---|---|
| Java version | **Java 21 (LTS)** | Virtual threads, modern language features |
| Build tool | **Gradle (Kotlin DSL)** or Maven | Both work; Gradle has better multi-module support |
| Database | **PostgreSQL 16+** | Best JPA support, mature, free |
| DB migrations | **Liquibase** | Closer to Jmix's approach than Flyway |
| Auth provider | **Authentik** | Modern, MIT, visual flow editor |
| API style | **REST + JSON** | Standard for React frontends |
| Project layout | **Modular monolith** (Gradle multi-module) | Closest to Jmix Composite Projects |

**Time investment:** 1 hour to write down your stack and create a README before any code.

---

## Phase 1 — Project Skeleton (Day 1)

**Goal:** A Spring Boot app that starts, connects to PostgreSQL, and responds to a health check.

### Steps

1. **Generate base project** at https://start.spring.io
   - Spring Boot 3.4+
   - Dependencies: `Spring Web`, `Spring Data JPA`, `Validation`, `PostgreSQL Driver`, `Liquibase`, `Lombok`, `Spring Boot DevTools`

2. **Set up PostgreSQL locally** — use Docker Compose:
   ```yaml
   # docker-compose.yml
   services:
     postgres:
       image: postgres:16
       environment:
         POSTGRES_DB: myapp
         POSTGRES_USER: myapp
         POSTGRES_PASSWORD: myapp
       ports: ["5432:5432"]
   ```

3. **Configure `application.yml`** — datasource, JPA settings, Liquibase changelog path

4. **Create your first Liquibase changelog** — empty `db/changelog/db.changelog-master.xml`

5. **Add a `/health` controller** — verify the app starts and responds

6. **Initialize Git** — commit the working skeleton

✅ **Deliverable:** `./gradlew bootRun` starts the app, `curl localhost:8080/health` returns OK.

---

## Phase 2 — Base Entity Foundation (Day 2)

**Goal:** Every entity in your app gets audit fields, soft delete, and optimistic locking for free.

### Steps

1. **Create `BaseEntity` mapped superclass** with:
   - `UUID id` (auto-generated)
   - `Integer version` (`@Version` — optimistic locking)
   - Audit fields: `createdDate`, `lastModifiedDate`, `createdBy`, `lastModifiedBy`
   - Soft delete: `deleted` boolean + `deletedDate`

2. **Enable JPA auditing** — `@EnableJpaAuditing` config class

3. **Implement `AuditorAware<String>`** — initially returns `"system"`, you'll wire it to Spring Security later

4. **Add Hibernate `@SoftDelete`** (Hibernate 6.4+) to BaseEntity — or use `@SQLDelete` + `@Where`

5. **Create one test entity** (e.g., `Customer`) extending `BaseEntity` to verify it all works

6. **Write a Liquibase changelog** for the `customer` table including audit columns

✅ **Deliverable:** You can save a `Customer` and see `createdDate`, `createdBy`, `version` populated automatically.

📚 **Learn:** Spring Data JPA Auditing docs, Hibernate `@SoftDelete` chapter.

---

## Phase 3 — Repository + Service Pattern (Days 3-4)

**Goal:** Establish the DataManager equivalent — Repository (data) + Service (logic) + DTO (API contract).

### Steps

1. **Create `CustomerRepository`** extending `JpaRepository<Customer, UUID>` and `JpaSpecificationExecutor<Customer>`

2. **Create DTOs** — `CustomerSummaryDTO`, `CustomerDetailDTO`, `CreateCustomerRequest`, `UpdateCustomerRequest`

3. **Add MapStruct** — generate mappers between entities and DTOs

4. **Create `CustomerService`** with these operations:
   - `findAll(Pageable, Specification)` — list with filtering
   - `findById(UUID)` — get one
   - `create(CreateCustomerRequest)`
   - `update(UUID, UpdateCustomerRequest)` — with optimistic lock handling
   - `softDelete(UUID)`

5. **Create `CustomerSpecs`** utility class — composable predicates (`nameContains`, `createdBetween`, etc.)

6. **Build `CustomerController`** — REST endpoints for the above

7. **Add validation** — `@Valid`, `@NotBlank`, `@Size` on DTOs

8. **Add global exception handler** — `@RestControllerAdvice` to translate exceptions to proper HTTP responses

✅ **Deliverable:** A working CRUD API for customers, with pagination, sorting, and filtering via query parameters.

📚 **Learn:** Spring Data Specifications, MapStruct, Bean Validation, `@RestControllerAdvice`.

---

## Phase 4 — API Documentation (Half a Day)

**Goal:** Self-documenting API so you (and future React developers) know what's available.

### Steps

1. **Add SpringDoc OpenAPI dependency** (`springdoc-openapi-starter-webmvc-ui`)
2. **Annotate controllers** — `@Tag`, `@Operation`, `@ApiResponse` on key endpoints
3. **Configure OpenAPI info** — title, version, description
4. **Visit `/swagger-ui.html`** — interactive docs

✅ **Deliverable:** Swagger UI showing all your endpoints, with try-it-out support.

📚 **Learn:** SpringDoc OpenAPI basics.

---

## Phase 5 — Authentication with Authentik (Days 5-6)

**Goal:** Secure your API with JWT tokens issued by Authentik.

### Steps

1. **Run Authentik via Docker Compose** — follow https://docs.goauthentik.io/docs/install-config/install/docker-compose

2. **In the Authentik admin UI:**
   - Create an **OAuth2/OIDC Provider** for your app
   - Create an **Application** linked to that provider
   - Note the `issuer-uri` (e.g., `https://authentik.local/application/o/myapp/`)
   - Create roles/groups (e.g., `admin`, `user`)
   - Create test users

3. **In Spring Boot:**
   - Add `spring-boot-starter-oauth2-resource-server` and `spring-boot-starter-security`
   - Configure `issuer-uri` in `application.yml`
   - Create `SecurityConfig` — `SecurityFilterChain` bean
   - Map JWT claims to Spring authorities (Authentik puts groups in the `groups` claim)
   - Configure CORS for your future React dev server

4. **Wire AuditorAware** to Spring Security — `createdBy` now reflects the real user

5. **Test with curl** — get a token from Authentik, call your API with `Authorization: Bearer <token>`

✅ **Deliverable:** Endpoints reject unauthenticated requests; `createdBy` shows actual usernames.

📚 **Learn:** Spring Security OAuth2 Resource Server, JWT structure, Authentik docs.

---

## Phase 6 — Authorization Layer (Days 7-8)

**Goal:** Replace Jmix's built-in RBAC with explicit Spring Security rules at three levels.

### Steps

1. **Enable method security** — `@EnableMethodSecurity` annotation

2. **Add screen/endpoint-level rules** — `@PreAuthorize("hasRole('ADMIN')")` on services or controllers

3. **Add row-level filtering** — in `CustomerService`, compose Specifications based on current user:
   - Admin sees all
   - Regular user sees only their records (`createdBy == currentUser`)

4. **Add field-level filtering** with `@JsonView`:
   - Define `Views.Basic` and `Views.Admin`
   - Tag sensitive fields with `@JsonView(Views.Admin.class)`
   - Use `MappingJacksonValue` in controllers to apply views

5. **Write integration tests** with `@WithMockUser` — verify each role can/can't access what they should

✅ **Deliverable:** Multi-tenant data isolation works; admins see everything, users see only theirs.

📚 **Learn:** Spring Security method security, SpEL expressions, `@JsonView`.

---

## Phase 7 — Fetch Plans (Day 9)

**Goal:** Control which associations load with which query — Jmix's fetch plans.

### Steps

1. **Add `@NamedEntityGraph`** to your entities for common loading patterns:
   - `"Customer.summary"` — just the customer
   - `"Customer.withOrders"` — customer + orders + order lines

2. **Use `@EntityGraph` on repository methods**:
   ```
   @EntityGraph("Customer.withOrders")
   Optional<Customer> findDetailById(UUID id);
   ```

3. **Add interface projections** for list endpoints — `CustomerListProjection` reads only needed columns

4. **Set `spring.jpa.open-in-view=false`** in `application.yml` — force yourself to be explicit about loading

5. **Add Hibernate statistics in dev** — verify your queries don't trigger N+1 problems

✅ **Deliverable:** List endpoints fire one optimized SELECT; detail endpoints load full graphs in one shot.

📚 **Learn:** JPA Entity Graphs, Spring Data projections, N+1 problem detection.

---

## Phase 8 — Lifecycle Events (Day 10)

**Goal:** Replace Jmix DataManager events with Spring Application Events.

### Steps

1. **Define event records** — `CustomerCreatedEvent`, `CustomerUpdatedEvent`, `CustomerDeletedEvent`

2. **Inject `ApplicationEventPublisher`** into services — publish events on key operations

3. **Create listener components** with `@EventListener` and `@TransactionalEventListener(AFTER_COMMIT)`

4. **Use JPA callbacks** (`@PrePersist`, `@PreUpdate`) for entity-self logic (computed fields, defaults)

5. **Add `@Async` support** — `@EnableAsync` + thread pool config for async listeners

✅ **Deliverable:** Saving a customer publishes events; you can plug in any side effect without touching the service.

📚 **Learn:** Spring Events, `@TransactionalEventListener`, `@Async`.

---

## Phase 9 — Modular Project Structure (Days 11-12)

**Goal:** Split into modules so the project scales like Jmix Composite Projects.

### Steps

1. **Restructure into Gradle multi-module project:**
   ```
   my-app/
   ├── app/                    # Spring Boot main app
   ├── common/                 # BaseEntity, shared utilities, security
   ├── modules/
   │   ├── customer/           # Customer entity, repo, service, controller
   │   ├── order/              # Order entity, depends on customer
   │   └── product/
   ```

2. **Define module boundaries** — each module exposes only public services/DTOs, internals stay package-private

3. **Add Spring Modulith** (optional but recommended):
   - `spring-modulith-starter-core`
   - Use `@Modulith` and module boundaries verification in tests

4. **Move cross-module communication to events** — `OrderModule` listens to `CustomerDeletedEvent` rather than calling `CustomerService` directly

✅ **Deliverable:** Modules have clear boundaries; you can develop one without affecting others.

📚 **Learn:** Gradle multi-module setup, Spring Modulith documentation.

---

## Phase 10 — Audit Log with Envers (Day 13)

**Goal:** Full change history per entity — replaces Jmix Entity Audit Log add-on.

### Steps

1. **Add `spring-data-envers` dependency**

2. **Enable Envers repositories** — `@EnableJpaRepositories(repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)`

3. **Annotate entities with `@Audited`** — start with key ones, expand later

4. **Configure Envers properties** — table suffix, store data at delete

5. **Add Liquibase changelog** for audit tables (Envers creates `*_aud` and `revinfo`)

6. **Extend critical repositories with `RevisionRepository<T, ID, Integer>`**

7. **Add `/api/customers/{id}/history` endpoint** — list revisions, view entity state at revision N

✅ **Deliverable:** Every change to audited entities is recorded; you can view who changed what and when.

📚 **Learn:** Hibernate Envers, Spring Data Envers.

---

## Phase 11 — File Storage (Day 14)

**Goal:** Upload/download files with metadata, ready for cloud later.

### Steps

1. **Create `FileMetadata` entity** — fileName, contentType, sizeBytes, storagePath, version

2. **Create `FileStorageService` interface** with `LocalFileStorageService` impl for dev

3. **Add upload endpoint** — `POST /api/files` accepts `MultipartFile`

4. **Add download endpoint** — `GET /api/files/{id}/download` streams the file with proper Content-Disposition

5. **Add file versioning** — link new uploads to previous version

6. **Plan for production** — interface allows swapping to S3/MinIO later without touching business code

✅ **Deliverable:** File upload/download API with metadata tracking.

📚 **Learn:** Spring MVC multipart, `Resource` responses.

---

## Phase 12 — Email Service (Day 15)

**Goal:** Send transactional emails (welcome, password reset triggers, notifications).

### Steps

1. **Add `spring-boot-starter-mail`** and `spring-boot-starter-thymeleaf` (for HTML templates)

2. **Configure SMTP** in `application.yml` — use MailHog or Mailpit in dev (Docker)

3. **Create `EmailService`** — methods like `sendWelcomeEmail(User)`, `sendOrderConfirmation(Order)`

4. **Create Thymeleaf templates** in `resources/templates/emails/`

5. **Trigger emails from event listeners** — `@TransactionalEventListener(AFTER_COMMIT) @Async`

6. **Don't block requests** — emails always async

✅ **Deliverable:** Domain events trigger emails automatically; SMTP is swappable per environment.

📚 **Learn:** Spring Boot mail, Thymeleaf basics.

---

## Phase 13 — Background Jobs with Quartz (Days 16-17)

**Goal:** Scheduled jobs that survive restarts — replaces Jmix Quartz add-on.

### Steps

1. **Add `spring-boot-starter-quartz`**

2. **Configure JDBC job store** — `spring.quartz.job-store-type=jdbc`, `initialize-schema=always`

3. **Create your first job** — extend `QuartzJobBean`, autowire services

4. **Register triggers** via `@Configuration` — `JobDetail` + `Trigger` beans

5. **Add a dynamic job management controller** — pause/resume/trigger jobs via REST (admin-only)

6. **Test cluster safety** — Quartz handles concurrent execution in multi-instance deployments

✅ **Deliverable:** Cron jobs running, survive restarts, manageable via REST.

📚 **Learn:** Quartz scheduler basics, Spring Boot Quartz auto-config.

---

## Phase 14 — Reports & Data Export (Days 18-19)

**Goal:** Excel and PDF exports — replaces Jmix Reports and Data Export.

### Steps

1. **Add Apache POI** for Excel — `poi-ooxml`

2. **Create `ExcelExportService`** — generic helper that takes a list of DTOs + column definitions

3. **Add `GET /api/customers/export/excel`** — streams XLSX with proper headers

4. **Add OpenHTMLtoPDF** — `openhtmltopdf-pdfbox`

5. **Create `PdfReportService`** — renders Thymeleaf HTML templates to PDF

6. **Build a sample report** — e.g., customer invoice PDF

✅ **Deliverable:** Working Excel and PDF export endpoints.

📚 **Learn:** Apache POI basics, HTML-to-PDF with OpenHTMLtoPDF.

---

## Phase 15 — Real-Time Notifications (Day 20)

**Goal:** Push updates to the React frontend — replaces Jmix Notifications.

### Steps

1. **Add `spring-boot-starter-websocket`**

2. **Configure STOMP** — `@EnableWebSocketMessageBroker`, broker prefixes, endpoints

3. **Secure the WebSocket** — verify JWT on connect

4. **Create `NotificationService`** with `SimpMessagingTemplate`:
   - `notifyUser(username, message)` — to specific user
   - `broadcast(message)` — to all

5. **Trigger from event listeners** — `OrderCreatedEvent` → notify assigned user

6. **Optional alternative:** SSE (`SseEmitter`) is simpler if you only need server→client

✅ **Deliverable:** Backend can push messages; React will subscribe in its phase.

📚 **Learn:** Spring WebSocket + STOMP, securing WebSockets.

---

## Phase 16 — Search with Elasticsearch (Days 21-22)

**Goal:** Full-text search across entities — replaces Jmix Elasticsearch add-on.

### Steps

1. **Add Elasticsearch via Docker Compose**

2. **Add `spring-boot-starter-data-elasticsearch`**

3. **Create search document classes** (`CustomerSearchDocument`) with `@Document`

4. **Create `ElasticsearchRepository` interfaces**

5. **Build an indexing service** — listens to `*CreatedEvent`, `*UpdatedEvent`, `*DeletedEvent` and updates the index

6. **Bulk re-index job** — Quartz job that rebuilds the index from scratch (admin trigger)

7. **Search endpoint** — `GET /api/search?q=...` with multi-field, fuzzy matching

✅ **Deliverable:** Working search across multiple entity types, kept in sync via events.

📚 **Learn:** Spring Data Elasticsearch, basic Elasticsearch query DSL.

---

## Phase 17 — Testing Strategy (Ongoing, Solidify Here)

**Goal:** Tests you trust before adding more features.

### Steps

1. **Unit tests** — services with Mockito for repositories

2. **Slice tests** — `@WebMvcTest` for controllers, `@DataJpaTest` for repos

3. **Integration tests with Testcontainers** — real PostgreSQL, optionally Elasticsearch

4. **Security tests** — `@WithMockUser`, `@WithMockJwt` to verify authorization

5. **Spring Modulith verification tests** — confirm module boundaries hold

✅ **Deliverable:** CI pipeline runs tests; you can refactor with confidence.

📚 **Learn:** Testcontainers, Spring Boot test slices, Spring Security testing.

---

## Phase 18 — Production Readiness (Days 23-25)

**Goal:** The boring but essential stuff before going live.

### Steps

1. **Spring Boot Actuator** — health, metrics, info endpoints (secured)

2. **Observability** — Micrometer + Prometheus, structured logging with Logback JSON encoder

3. **Configuration profiles** — `dev`, `test`, `prod` with environment variable overrides

4. **Secrets management** — never commit secrets; use environment variables or HashiCorp Vault

5. **Containerize** — multi-stage Dockerfile, set JVM flags for containers

6. **CORS production config** — lock to React production origin

7. **Rate limiting** — Bucket4j or API gateway

8. **Database backups** — automated, tested

✅ **Deliverable:** Production-quality deployment artifacts and observability.

📚 **Learn:** Spring Boot Actuator, Micrometer, Dockerfile best practices.

---

## Phase 19 — Optional Advanced Features

Pick from these once your core is solid:

| Feature | When to add | Library |
|---|---|---|
| **Multitenancy** | If you have multiple tenants from day one, do it early — retrofitting is painful | Hibernate Filters + tenant context |
| **LDAP/AD federation** | If clients require corporate login | Authentik handles this via UI |
| **Caching** | When you measure a hot read path | Spring Cache + Caffeine or Redis |
| **Message queue** | When async work outgrows in-process events | RabbitMQ or Kafka |
| **Cross-datastore** | Rare — only if business demands it | Multiple `DataSource` beans |

---

## Suggested Timeline

| Weeks | What you'll have |
|---|---|
| **Week 1** | Project skeleton, base entity, CRUD service for one entity, Swagger UI |
| **Week 2** | Authentik integration, full security (RBAC + row-level + field-level), fetch plans |
| **Week 3** | Modular structure, lifecycle events, audit log, file storage |
| **Week 4** | Email, Quartz jobs, exports/reports, WebSocket notifications |
| **Week 5** | Elasticsearch search, testing solidified, production readiness |
| **Week 6+** | React frontend (your next chapter) |

---

## Universal Rules to Follow

1. **One phase at a time.** Don't start Phase 4 until Phase 3 is committed and tested.
2. **Each phase ends with a working app.** Never leave the codebase in a half-broken state overnight.
3. **Database migrations only via Liquibase.** Never edit the DB directly.
4. **Write a small test for each new feature.** Even one happy-path test prevents regressions.
5. **Commit often, with meaningful messages.** "Phase 5: integrate Authentik resource server" beats "stuff."
6. **Bookmark the docs you'll re-read 50 times:**
   - https://docs.spring.io/spring-boot/reference/
   - https://docs.spring.io/spring-data/jpa/reference/
   - https://docs.spring.io/spring-security/reference/
   - https://docs.jboss.org/hibernate/orm/6.6/userguide/html_single/Hibernate_User_Guide.html
   - https://docs.goauthentik.io
   - https://www.baeldung.com (for any specific how-to)

---

## A Note on Mindset

Jmix gives you 80% of this out of the box. In Spring Boot, you build it explicitly — which means more code, but also:

- **No magic** — you understand every layer
- **No vendor lock-in** — every piece is industry-standard
- **Massive community** — every problem has a thousand StackOverflow answers
- **Full control** — customize anything, anywhere

This roadmap is roughly 5-6 weeks of focused work for one developer. The investment pays off with a system you fully own and can evolve in any direction.
