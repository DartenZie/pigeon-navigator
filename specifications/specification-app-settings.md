# App Settings Specification

## Goal

Implement global application settings that can be injected into any module on Android and iOS while preserving the repository's domain-first Kotlin Multiplatform architecture.

The initial settings scope is:

- Preferred units.
- Preferred time to collision threshold for warnings.

The design must be modular and easy to extend with additional settings groups later.

## Non-Goals

- Build the settings UI.
- Add user accounts or cloud synchronization.
- Store large or sensitive data.
- Move domain calculations away from canonical units.
- Introduce platform SDK types into shared domain, data, or feature APIs.

## Architecture

Settings must be exposed through a domain repository interface and implemented in a data module backed by platform key/value storage.

Required dependency shape:

```text
app -> feature -> domain -> core
data:settingsData -> domain + core:platform
```

No feature or domain code may depend directly on Android `SharedPreferences`, Android DataStore, iOS `NSUserDefaults`, or any other platform SDK type.

## Modules

### `:domain`

Owns the public settings contract:

- Settings value objects.
- Settings enums.
- `AppSettingsRepository` interface.
- Any settings-related use cases if needed later.

### `:core:platform`

Owns the platform abstraction for primitive persisted key/value access.

This module may provide:

- A common `KeyValueSettingsStore` interface.
- Platform bindings/factories for Android and iOS.

### `:data:settingsData`

Owns persistence implementation:

- `AppSettingsRepositoryImpl`.
- Settings keys.
- Persistence mapping.
- Defaults and migration policy.
- Koin module factory `settingsDataModule()`.

### App Modules

Android and iOS composition roots must register the settings module once during Koin startup.

## Domain Model

Settings must be grouped by concern instead of stored as a flat set of fields.

Required initial model:

```kotlin
data class AppSettings(
    val units: UnitPreferences = UnitPreferences(),
    val warning: WarningPreferences = WarningPreferences(),
)

data class UnitPreferences(
    val distance: DistanceUnit = DistanceUnit.NauticalMiles,
    val altitude: AltitudeUnit = AltitudeUnit.Feet,
    val speed: SpeedUnit = SpeedUnit.Knots,
)

data class WarningPreferences(
    val timeToCollisionWarningSeconds: Int = DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS,
)
```

Required enums:

```kotlin
enum class DistanceUnit {
    NauticalMiles,
    Kilometers,
    Miles,
}

enum class AltitudeUnit {
    Feet,
    Meters,
}

enum class SpeedUnit {
    Knots,
    KilometersPerHour,
    MilesPerHour,
    MetersPerSecond,
}
```

Default warning threshold:

```kotlin
const val DEFAULT_TIME_TO_COLLISION_WARNING_SECONDS = 60
```

The exact default may be adjusted before implementation if product requirements change, but it must be centralized and tested.

## Repository Contract

Add this interface to `:domain`:

```kotlin
interface AppSettingsRepository {
    val settings: StateFlow<AppSettings>

    suspend fun updateUnits(units: UnitPreferences): AppResult<Unit, Failure>

    suspend fun updateWarningPreferences(
        warning: WarningPreferences
    ): AppResult<Unit, Failure>
}
```

Rules:

- `settings` must always emit a complete `AppSettings` value.
- Missing persisted values must resolve to defaults.
- Invalid persisted values must resolve to defaults instead of crashing.
- Update methods must persist changes and update the in-memory `StateFlow`.
- Failures must be represented with the existing explicit `Failure` model.
- Repository consumers must not need to know how settings are persisted.

## Platform Storage Abstraction

Add a primitive storage abstraction to `:core:platform`:

```kotlin
interface KeyValueSettingsStore {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)

    suspend fun getInt(key: String): Int?
    suspend fun putInt(key: String, value: Int)
}
```

Initial platform backing:

- Android: `SharedPreferences` is acceptable for the first implementation because settings are small primitive values and the repo does not currently depend on DataStore.
- iOS: `NSUserDefaults`.

All platform implementations must be bound in platform-aware DI modules or platform factories. Shared consumers must only depend on `KeyValueSettingsStore` or `AppSettingsRepository`.

## Persistence Format

Use stable string keys and stable enum names/serialized values.

Required initial keys:

```text
app_settings.units.distance
app_settings.units.altitude
app_settings.units.speed
app_settings.warning.time_to_collision_seconds
```

Persistence rules:

- Store enum values as explicit stable strings, not ordinals.
- Never persist Kotlin enum ordinal values.
- Clamp or reject invalid warning threshold values.
- A valid time to collision threshold must be greater than zero.
- If a stored value is missing or invalid, use the default and keep the repository usable.

Recommended threshold bounds:

```text
minimum: 5 seconds
maximum: 600 seconds
```

If the user enters values outside this range, the repository should return a validation failure and avoid persisting the invalid value.

## DI

Create `settingsDataModule()` in `:data:settingsData`:

```kotlin
fun settingsDataModule() = module {
    single<AppSettingsRepository> {
        AppSettingsRepositoryImpl(
            store = get(),
            dispatcherProvider = get(),
        )
    }
}
```

Android startup must include the settings data module in `PigeonNavigatorApplication`.

iOS startup must include the settings data module in `initKoin()`.

The module should be registered before feature modules so feature stores can inject `AppSettingsRepository` during construction.

## Android Binding

Android must provide a singleton `KeyValueSettingsStore` backed by application-scoped storage.

Requirements:

- Use application context, not an activity context.
- Do not leak Android types outside the binding implementation.
- Use a stable preferences file name, for example `pigeon_navigator_app_settings`.

## iOS Binding

iOS must provide a singleton `KeyValueSettingsStore` backed by `NSUserDefaults`.

Requirements:

- Use standard user defaults unless a suite is explicitly required later.
- Do not expose Foundation types through common interfaces.
- Register the binding during Koin initialization.

## Feature Integration

### Terrain Warning

The terrain warning feature must consume the preferred time to collision warning setting.

Implementation requirements:

- Inject `AppSettingsRepository` into `RealTerrainWarningStore` or a dedicated use case that builds terrain warning parameters.
- Do not read settings inside `TerrainWarningReducer`.
- Do not hard-code `TerrainConflictParameters()` if settings should affect warning thresholds.
- Convert `WarningPreferences.timeToCollisionWarningSeconds` into `TerrainConflictParameters.warningTimeToImpactSeconds`.
- Keep all terrain calculations in canonical domain units.

The store may either:

- Read `appSettingsRepository.settings.value` for each computation.
- Collect settings changes and cache current settings inside the store.

Live updates are preferred if implementation remains simple.

### Units

Unit preferences must be used at formatting/presentation boundaries.

Rules:

- Domain models should continue using canonical units such as meters, meters per second, seconds, and degrees.
- Data repositories must not convert persisted aviation, terrain, or location data based on user preferences.
- UI or presentation formatting should convert canonical values to preferred display units.

## iOS Swift API

If Swift UI needs direct access to settings, add a Swift-friendly handle similar to existing store handles.

Possible API:

```kotlin
class SettingsHandle(
    private val repository: AppSettingsRepository
) {
    fun startSettings(onEach: (AppSettings) -> Unit)
    fun stopSettings()
    fun updateUnits(...)
    fun updateTimeToCollisionWarningSeconds(seconds: Int)
    fun close()
}
```

This handle is optional until Swift UI needs to display or edit settings directly.

## Error Handling

Settings operations must not throw exceptions as control flow across layers.

Rules:

- Map persistence errors to `Failure.Unexpected` or a more specific existing failure if available.
- Map invalid settings updates to `Failure.Validation` if the existing failure model supports it.
- If `Failure.Validation` does not exist yet, add it in `:domain` before using validation errors.
- Read failures should emit defaults if possible and return failure only for explicit update operations.

## Concurrency

The repository must use injected `DispatcherProvider`.

Rules:

- No hard-coded `Dispatchers.IO`, `Dispatchers.Default`, or `Dispatchers.Main` in shared settings logic.
- Storage reads and writes should run on `dispatcherProvider.io`.
- `StateFlow` updates must remain thread-safe.

## Extensibility

Future settings must be added as new grouped value objects when they represent a new concern.

Examples:

```kotlin
data class AppSettings(
    val units: UnitPreferences = UnitPreferences(),
    val warning: WarningPreferences = WarningPreferences(),
    val map: MapPreferences = MapPreferences(),
    val location: LocationPreferences = LocationPreferences(),
)
```

Rules for future settings:

- Add defaults in one place.
- Add stable persistence keys.
- Add repository update methods by concern, not one method per tiny field unless justified.
- Add migration handling when changing serialized values.
- Add tests for defaults, persistence, invalid values, and affected feature behavior.

## Testing Requirements

Add shared tests for settings behavior.

Required tests:

- Repository emits default `AppSettings` when storage is empty.
- Repository persists and emits updated unit preferences.
- Repository persists and emits updated warning preferences.
- Invalid persisted enum values fall back to defaults.
- Invalid time to collision values are rejected or clamped according to the chosen implementation rule.
- Terrain warning uses the configured warning time to collision threshold.

Test rules:

- Use `kotlinx-coroutines-test`.
- Use a test `DispatcherProvider`.
- Use fake `KeyValueSettingsStore` implementations.
- Do not use Android or iOS platform storage in common tests.

## Acceptance Criteria

- `AppSettingsRepository` can be injected from Android app code, iOS bridge code, data modules, feature modules, and shared stores.
- Android and iOS both persist settings across app restarts.
- Missing settings use defaults.
- Invalid persisted values do not crash the app.
- Terrain warning uses configured time to collision warning threshold.
- Preferred units are represented in shared settings and available to presentation code.
- Shared business logic remains platform-free.
- Reducers remain pure.
- All changed shared logic has tests.
