# Integration tests

## Running independently or together

Requires Java 25 and Docker. No manual database setup or secrets are required:

```powershell
# All business scenarios and the two schema migration checks.
./mvnw.cmd test

# One independently runnable domain suite.
./mvnw.cmd '-Dtest=PaymentIntegrationTest' test

# One independently runnable scenario.
./mvnw.cmd '-Dtest=AuthIntegrationTest#registrationConfirmationHashesPasswordConsumesTokenAndEnablesLogin' test

# Several suites in a single JVM, sharing their infrastructure.
./mvnw.cmd '-Dtest=AuthIntegrationTest,EmployeeIntegrationTest,MenuIntegrationTest' test

# Entire suite, application package, and coverage report.
./mvnw.cmd -Pcoverage verify
```

The same classes and methods can be run using IntelliJ's JUnit runner. On Linux
and macOS use `./mvnw` instead of `./mvnw.cmd`. Do not use `-DskipTests` when verifying
business behavior. Coverage is written to `target/site/jacoco/index.html`; ordinary
test runs avoid the coverage agent. Surefire reports are in `target/surefire-reports`.

## Shared resources and isolation

All domain tests inherit `AbstractIntegrationTest`. It supplies exactly the same
Spring configuration, external boundary mocks, dynamic database properties, and
fixture lifecycle to every suite. The Spring test context cache therefore reuses
one application, one random HTTP port, one EntityManagerFactory, and one Hikari
connection pool (maximum six connections) for the whole domain run. Surefire uses
one reusable JVM. JUnit runs methods sequentially; concurrency tests explicitly
start their own joined workers.

`SharedMySql` lazily starts one MySQL 8.4 container per JVM, independently of JUnit
class lifecycle. There is no per-class `@Container` or `@DirtiesContext`. The
container is limited to 512 MiB and one CPU, the test JVM to 512 MiB and two active
processors. Testcontainers removes its containers at JVM exit. Separate IDE/JVM
invocations naturally have separate pools and can use separate automatic containers.

Liquibase runs once for the shared application. `DatabaseState` creates SQL
baseline copies of the fixture tables once. Before each method it restores their
rows with `DELETE`/`INSERT ... SELECT` in one transaction, using one existing pooled
connection. It does not recreate tables, run migrations, restart MySQL, or rebuild
the Spring context. Foreign key checks are disabled only on that restore connection
and restored in `finally`. Tests create committed data, so HTTP, scheduled-method,
and cross-thread behavior use real transaction boundaries instead of relying on a
test transaction rollback.

Financial data, confirmation tokens, shifts, and provider key caches start empty.
Roles, the restaurant, four employees, menu, images, tables, and opening hours have
stable baseline data. Employees' initial password is the test-only
`Str0ngPass!2026`. Every method restores its own starting state; no scenario depends
on another test's execution order. `SharedInfrastructureIntegrationTest` explicitly
checks context/pool identity across every domain suite and verifies data restoration
without additional migration executions.

The payment synchronization scheduler has a 24-hour initial delay in tests, so
background polling cannot interfere with a scenario. Tests call the real scheduler
method explicitly when exercising polling. Production retains its five-minute
defaults.

## Reusing an external test database

For repeated independent JVM/IDE runs without container startup, provide an
existing dedicated MySQL schema:

```powershell
$env:TEST_DATABASE_URL = 'jdbc:mysql://127.0.0.1:33317/restaurant_test'
$env:TEST_DATABASE_USERNAME = 'restaurant_test'
$env:TEST_DATABASE_PASSWORD = 'restaurant_test'
./mvnw.cmd '-Dtest=*IntegrationTest' test
```

Its name must end with `_test`, checked before any application migration runs.
Its data is disposable: the suite deletes and restores domain rows. Never use a
working database. Baseline copies persist in `_fixture_*` tables so independent
invocations restore the original baseline instead of snapshotting mutations left
by the previous method. If application migrations change the column layout, use
a fresh test schema; an incompatible baseline is rejected explicitly.

Full migration checks additionally need root access on the **test server**:

```powershell
$env:TEST_DATABASE_ADMIN_URL = 'jdbc:mysql://127.0.0.1:33317/mysql'
$env:TEST_DATABASE_ADMIN_PASSWORD = 'restaurant_test_root'
./mvnw.cmd verify
```

Without external database settings these credentials are obtained from the single
automatic container. The migration checks create and remove two isolated
`restaurant_migration_test_<uuid>` schemas on that same server. They intentionally
exercise different initial schemas and do not clear the shared business schema.
No new database server is created per migration or scenario.

## Business path inventory

PaymentOperationsIntegrationTest covers invoice recovery, reconciliation, refund
authorization/idempotency/retries/unknown outcomes, cash returns and reports.
OrderReliabilityIntegrationTest covers concurrent additions and fulfillment after payment.
RestaurantTimeZoneIntegrationTest covers zone selection and UTC/DST report boundaries.

| Suite | Scenarios |
|---|---|
| `AuthIntegrationTest` | Registration, admin/employee roles, optional profile photo, duplicate email, email delivery failure rollback, resend activation, confirmation, token owner/type/expiry/reuse, inactive/deleted/invalid login, cookies, refresh/logout, password reset/change, HTTP validation, rendered/localized emails. |
| `AuthorizationIntegrationTest` | Anonymous rejection on every protected business route, employee rejection on every admin route, JWT type/signature/expiry/subject failures, CORS. |
| `EmployeeIntegrationTest` | Profile/picture replacement, self/admin permissions, email change and collisions, pending email confirmation, deletion/restoration, last admin protection, reassignment requirement, existing token rejection, historical preservation. |
| `MenuIntegrationTest` | Category parent/child CRUD and role constraints, deletion while referenced, localized names/ingredients/flags/prices/images, partial updates, unavailable items, restore availability, pagination/category filters/currency, invalid payloads and missing resources. |
| `GuestOrderIntegrationTest` | Guest ordering/editing/cancellation, ownership and cookie consistency, current guest first, snapshot price/name, per-person limits and canceled positions, noneditable states, unavailable dishes, closed orders and payload validation. |
| `EmployeeOrderIntegrationTest` | All 24 employee status transitions, quantity/comment editing, first confirmation assignment, existing/new/foreign guests, normal and manual closure, cancellation without revenue, reassignment, open/history/detail/payment listing and date/status filters. |
| `GuestSessionIntegrationTest` | Stale/unrelated/missing guest cookies, repeated visit and reuse of the current guest. |
| `DiningTableIntegrationTest` | Table CRUD/status/read/list, occupied-table guard, active filtering, decoded QR contents, QR entry cookies, inactive/unknown tables, concurrent first visitors sharing one order. |
| `ManagementIntegrationTest` | Restaurant profile/images/region/currency confirmation, closed-order requirement, working days/full/partial hours, shift create/read/update/delete, overlap and adjacency, employee/date filters, deleted/missing employee, invalid hours. |
| `PaymentIntegrationTest` | Cash/online competition and idempotency, reserved positions, allowed payable states, provider request amounts/currency/tips, payer/order ownership, failed attempt history and retry, ambiguous timeout reservation, known invoice sync, all provider status mappings, repeated synchronization failures/recovery, partial cash settlement and API validation. |
| `WebhookIntegrationTest` | Real HMAC and ECDSA verification, all supported events/provider states, same timestamp and duplicate success, stale events/signatures, tampering, malformed payloads/signatures, unknown invoice, key rotation, success downgrade reconciliation. |
| `ProviderHttpIntegrationTest` | Real HTTP adapters against loopback server: auth/idempotency headers, form/JSON and URL encoding, status requests, 4xx/429/5xx responses, malformed JSON, key fetch/cache/refresh, timeout, connection failure and thread interruption. |
| `AnalyticsIntegrationTest` | Cash/online/tips and currency separation, payment versus distinct order count, month boundaries and completion timestamps, excluded unsuccessful payments, unknown history, employee/currency tip grouping, deleted/unassigned recipients, payout/reversal/filtering, invalid report periods. |
| `ReviewIntegrationTest` | Verified online payer, cash invitation and one-use consumption, eligibility/list permissions, invalid/reconstructed/conflicting/old receipts, validation boundaries, per-guest/order uniqueness across payments, foreign tokens and concurrent submissions. |
| `ImageIntegrationTest` | Persistence/public bytes and response headers, supported/unsupported MIME, empty uploads, missing resources and deletion. |
| `SharedInfrastructureIntegrationTest` | Shared context and pool across all suites; data restoration preserves pool and migration history. |
| `MigrationSafetyTest` | Original schema upgrade preserving existing rows, reconstructed cash/unknown historical currency, repeat startup preservation, production bootstrap without demo data. |

The matrix describes observable business rules. JaCoCo additionally shows line,
method, and branch coverage, including branches that are not separate user paths.
Real payment/email delivery is outside this suite: business tests replace the
external payment and email boundaries; HTTP adapter tests exercise real clients
against a local provider server. No money is charged and no email is sent.

When adding scenarios, inherit the shared base and add a normal `@Test` or
parameterized test. Keep provider/email mock declarations in the base; adding
different per-class overrides or `@DirtiesContext` defeats cache reuse. Join any
workers before returning from a test. Do not enable concurrent execution of whole
test classes against the single shared schema.
