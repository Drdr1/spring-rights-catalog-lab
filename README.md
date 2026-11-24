# spring-rights-catalog-lab

A small full-stack module of the kind teams add to an existing Java/Angular product: a
**media rights catalog** with titles, license windows (territory × platform × dates, optionally
exclusive), and an "what can we show here today?" availability check.

| layer | what's in it |
|---|---|
| API | Java 21, Spring Boot 3.4 (MVC, Data JPA, Security), Hibernate, PostgreSQL 16, Liquibase |
| Auth | Keycloak SSO: Angular signs in with PKCE, the API is an OAuth2 resource server, realm roles mapped to Spring roles |
| SQL | two tuned read queries with indexes, plus a database exclusion constraint for overlapping exclusive windows |
| Integration | external metadata REST service via `RestClient`, with timeouts and error mapping |
| UI | Angular 22 standalone components, signals, reactive forms, functional HTTP interceptor |
| Tests | JUnit 5 + MockMvc + Testcontainers (real PostgreSQL), MockRestServiceServer, Vitest for Angular |
| Run | `docker compose up --build` (PostgreSQL, Keycloak with an imported realm, API, web) |

## Roles

| Keycloak realm role | Spring role | may |
|---|---|---|
| `viewer` | `ROLE_VIEWER` | read titles, windows, availability |
| `editor` | `ROLE_EDITOR` | + create titles |
| `rights-manager` | `ROLE_RIGHTS_MANAGER` | + add license windows |

Anything not explicitly allowed is denied (`anyRequest().denyAll()`). `ApiIT` tests the role
matrix: 401 without a token, 403 for the wrong role, and 403 for a token with no known roles.

## The business rule worth reading

Two windows for the same title, territory and platform may not overlap if either one is
exclusive. Dates are inclusive, so a window ending on June 30 and one starting on July 1 is
fine, but starting on June 30 is not. The service checks this to return a clear 409. A
PostgreSQL `EXCLUDE USING gist` constraint guarantees it for exclusive-vs-exclusive windows
even under concurrent requests. Both are tested.

## Query tuning

[docs/query-tuning.md](docs/query-tuning.md) walks through the two slow queries and their fixes:
a trigram GIN index for substring title search, and a composite index for availability.
`QueryPlanIT` runs `EXPLAIN` on 200,000 titles and 80,000 windows, and asserts that the new
queries use their indexes and that the old versions did not.

## Run it

```bash
docker compose up --build
# web  http://localhost:4200   (viewer/viewer, editor/editor, manager/manager; local demo only)
# api  http://localhost:8080/api
# keycloak admin http://localhost:8081 (admin/admin)
```

Tests:

```bash
cd api && mvn verify                       # needs Docker for Testcontainers
cd web && npm ci && npx ng test --watch=false
```

## Layout

```
api/src/main/java/dev/derops/rights/
  title/          entity, service, controller, TitleQueries (tuned search SQL)
  license/        license windows, overlap rule
  availability/   AvailabilityQueries (tuned lookup SQL), controller
  metadata/       RestClient integration with timeouts
  security/       resource server config, Keycloak role converter
  common/         problem-detail error mapping
api/src/main/resources/db/changelog/   Liquibase (schema, indexes, exclusion constraint)
web/src/app/      core (auth, API, models), titles, availability
keycloak/         realm import (roles, PKCE client, demo users)
```

Public work sample by [Ahmed Darder](https://github.com/Drdr1). No client code or data.
