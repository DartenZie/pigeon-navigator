# Kotlin Multiplatform Shared Model Infrastructure Rules

This repository follows a **domain-first, feature-modular, platform-at-the-edges** Kotlin Multiplatform (KMP) architecture.All contributors (humans and code agents) **must** follow this document.

If a change would violate these rules, **refactor the code to comply** rather than adding exceptions.

---

## 1) Architecture goals (non-negotiable)

1. **Shared owns "what the app does."** Platforms own "how it looks/behaves."
2. **Dependency direction is inward only** (no reverse dependencies).
3. **Business logic is testable in shared** without platform frameworks.
4. **State is explicit** (undirectional data flow) and side-effects are isolated.
5. **Platform APIs are behind interfaces** (adapters) and injected.

---

## 2) Module layout (enforced)

The project MUST conform to the following Gradle module structure (names can vary slightly, shape cannot):

### Core
- `:core:util`
  Pure Kotlin utilities. No dependency on domain/data/features/apps.

- `:core:platform`
  `expect/actual` platform primitives & bindings (time, uuid, filesystem, secure storage, connectivity, etc.).
  May depend on `:core:util` only.

- `:core:network`
  Ktor setup, auth interceptors, serialization config, network error mapping.
  May depend on `:core:util` and `:core:platform`.

- `:core:database`
  SQLDelight schema, drivers, migrations, transaction helpers.
  May depend on `:core:util` and `:core:platform`.

- `:core:di` (optional)
  DI helpers and module factories. Must not import UI frameworks.

### Domain
- `:domain`
  Entities/value objects, use-cases, repository interfaces, domain failures.
  **Pure Kotlin**. No Ktor, no SQLDelight, no platform APIs, no DI frameworks required.

### Data
- `:data` (or `:data:<feature>` per feature)
  Repository implementations, data sources, DTOs, mappers, cache policies.
  May depend on core modules + domain. Must NOT depend on features or apps.

### Features
- `:feature:<name>`
  Each feature is a scalable unit.
  Contains:
  - Presentation state machine (State/Intent/Effect/Reducer/Store)
  - Feature-level DI module factory (if used)
  - Public entrypoint API for platform UI  
  May depend on `:domain` and required core modules.
  Must NOT depend on app modules.

### Apps / Platforms
- `:app-android`, `:app-ios`
  UI, navigation host, composition root, platform bindings implementations.
  May depend on features + data + core as needed.
  Must NOT contain business rules that belong in shared.

---

## 3) Dependency rules (hard constrains)

### Allowed dependency direction
`app/* → feature/* → domain → core/*`

`data → domain → core/*`

### Forbidden
- `domain` depending on `data`, `feature`, or any `app` module.
- `feature` dependeing on any `app` module.
- `data` depending on any `feature` module.
- Shared modules importing AndroidX/Swift/iOS SDK types directly (except inside platform `actual` source sets).

If you need platform behavior in shared:
- Add and interface in shared (`:core:platfrom` or feature contract), and implement it in the platform app.
- Inject it via constructor/DI.

---

## 4) "Shared Model" presentation rules (UDF/MVI)

Each feature MUST implement an explicit state model:

### Required types (per feature)
- `State`: immutable data class representing the full UI state
- `Intent` (or `Action`): user/system inputs
- `Effect`: one-shot outputs (navigation, toast, open URL, etc.)
- `Reducer`: pure function `(State, Intent) -> State` (or `(State, Msg) -> State`)
- `Store` (or `FeatureComponent`): runs coroutines, invokes use-cases, emits State/Effect

### Required streams
- `val state: StateFlow<State>`
- `val effects: Flow<Effect>` (or Channel-backed flow)
- `fun send(intent: Intent)`

### Side-effects & IO
- Network/DB/platform calls MUST NOT happen inside a reducer.
- Side effects MUST happen in the Store (or in use-cases).
- Store MUST use injected dispatchers.

### Lifecycle
Feature Store MUST be disposable:
- provide `close()` / `dispose()`
Platforms are responsible for retaining and disposing per lifecycle.

---

## 5) Domain layer rules

### Domain MUST contain
- Entities/value objects
- Use-cases (interactors)
- Repository interfaces
- Domain failure model

### Domain MUST NOT contain
- DTOs
- SQLDelight/Room schemas
- Ktor client types
- Platform-specific types
- UI models that are framework-specific

### Errors
Domain failures MUST be explicit (sealed type). Examples:
- `Failure.Network`
- `Failure.Unauthorized`
- `Failure.NotFound`
- `Failure.Validation`
- `Failure.Unexpected`

Do not throw exceptions across layers as control flow.
Exceptions may be used internally but MUST be mapped to `Failure` at boundaries.

---

## 6) Data layer rules (offline-first, mapping, policies)

### Repository pattern
- `interface XRepository` lives in `:domain`
- `class XRepositoryImpl` lives in `:data`
- `RemoteDataSource` and `LocalDataSource` split is required for non-trivial features

### Mapping rules
- DTOs and database models MUST NOT leak into domain or feature presentation.
- Map to domain entities/value objects at repository boundary.

### Cache policies
When relevant, repository MUST define a clear policy (choose one and document it):
- cache-first
- network-first
- stale-while-revalidate
- local-only (rare)
- remote-only (rare)

---

## 7) Platform abstraction (`expect/actual` and adapters)

### Prefer library first
Use shared libraries (Ktor, SQLDelight, kotlinx-datetime, okio) where possible.

### Use `expect/actual` only for true platform primitives
Examples:
- secure storage (Keychain/Keystore)
- filesystem paths
- device info
- connectivity
- background execution hooks (if needed)

### No platform SDK leakage
Shared code MUST NOT reference:
- Android `Context`, `Activity`, `ViewModel`, etc.
- iOS `NSObject`, `UIKit`, `Swift` types
except in `actual` implementations or platform app code.

---

## 8) Concurrency & dispatchers (mandatory)

### Dispatcher provider
Shared modules MUST use an injected dispatcher provider:
- `Main`
- `IO`
- `Default`

No hard-coded `Dispatchers.Main` / `Dispatchers.IO` inside shared logic
(except in platform wiring or core platform actuals).

### Structured concurrency
- Each feature Store owns a `CoroutineScope` tied to lifecycle.
- Cancel scopes on dispose.

---

## 9) DI rules (if used)

- DI wiring lives in **platform composition root** (`:app-*`).
- Shared may provide **module factory functions**, but must not own composition root.
- Domain must be constructible without DI.

No service locator pattern in shared logic.

---

## 10) Code organization and naming conventions

### Feature package layout
`feaute/<name>/`
- `api/` - public entrypoint constrains (what UI uses)
- `presentation/` - State/Intent/Effect/Reducer/Store
- `di/` - feature module factory (optional)
- `internal/` - implementation details not exported

### Naming
- `State`, `Intent`, `Effect`, `Reducer`, `Store` per feature
- Use `sealed interface` / `sealed class` for `Intent` and `Effect`
- Use `data class` for `State`

### Visibility
- Default to `internal` inside feature modules.
- Only expose minimal public entrypoints under `api/`.

---

## 11) Testing requirements (gating)

Every PR that changes shared business logic MUST include tests.

### Minimum
- Domain use-cases: unit tests
- Reducers: pure state tests
- Repository policy: tests with fakes for remote/local
- Store: coroutine tests for at least one happy path + one failure path

### Test rules
- Use `kotlinx-coroutine-test`
- Use a rest dispatcher provider
- No network/DB calls in unit tests (use fakes)

---

## 12) PR checklist (must pass)

Before merging, verify:
- [ ] Module dependencies follow allowed direction (no forbidden edges).
- [ ] Feature changes follow State/Intent/Effect/Reducer/Store pattern.
- [ ] Reducers are pure (no IO).
- [ ] Side effects are isolated (Store/use-cases) and injected.
- [ ] Domain remains pure (no DTOs, no platform, no Ktor/SQLDelight).
- [ ] Data layer maps DTO/DB models to domain at boundaries.
- [ ] Errors are explicit (`Failure`), not uncontrolled exceptions.
- [ ] Dispatcher provider is injected; no hard-coded dispatchers in shared logic.
- [ ] Tests added/updated for changed shared logic.
- [ ] Public APIs are minimal; internals are `internal`.

---

## 13) Violations and how to fix them

### "I need Context/Keychain/API in shared..."
Create an interface in shared (`:core:platform` or feature `api/`) and implement it in the platform app.
Inject it via constructor/DI.

### "My reducer needs to call the network..."
Move the call into Store/use-case, emit an Intent/Msg with result, let reducer update state.

### "DTO leaked into UI state..."
Introduce domain model and mapper at repository boundary; UI state should use domain-friendly models.

### "Feature depends on app module"
Invert dependency: define a contract in feature `api/`, implement it in app.

---

## 14) Enforcement note

Agents (and humans) MUST treat this file as a **contract**:
- Do not add new modules or patterns without aligning with these rules.
- Do not bypass boundaries to "ship quickly."
- If you encounter legacy code, refactor incrementally toward compliance,

When in doubt: **push platform concerns outward** and **keep shared pure, explicit, and testable**.

