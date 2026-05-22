# PigeonNavigator

Offline-first aviation navigation app built with Kotlin Multiplatform, targeting **Android** (Jetpack Compose) and **iOS** (SwiftUI).

## Features

- **Moving map** — MapLibre GL with offline PMTiles-based vector tiles and a custom aviation style
- **Aviation data overlay** — airports, navaids (VOR/NDB), and airspace boundaries from bundled `.ofpkg` packages
- **Terrain warning (TAWS-like)** — forward-looking terrain conflict detection with configurable warning levels and hazard overlay
- **Nearby search** — airports, navaids, and containing airspaces near current position
- **Text search** — search aviation features by name or identifier
- **Route planning & navigation** — waypoint-based route with distance/ETA tracking
- **Map tap lookup** — tap the map to identify nearby aviation features
- **HUD cluster** — speed, altitude, GPS status
- **Configurable units** — NM/km/mi, ft/m, kts/kph/mph
- **Dual location sources** — device GPS or UDP stream (MSFS flight simulator or other flight sim integration)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.3 (Multiplatform) |
| Android UI | Jetpack Compose / Material 3 |
| iOS UI | SwiftUI |
| Maps | MapLibre GL (Android SDK 12.3, MapLibre Swift) |
| Tiles & Terrain | PMTiles (offline), Terrarium DEM |
| Database | SQLDelight 2.0 |
| DI | Koin 4.2 |
| Serialization | kotlinx-serialization |
| Architecture | MVI / Unidirectional Data Flow |

## Project Structure

```
core/
  util/             Pure Kotlin utilities (AppResult monad, etc.)
  platform/         expect/actual platform primitives (dispatchers, settings store)
  database/         SQLDelight driver factory

domain/             Entities, use-cases, repository interfaces, Failure model
                    Pure Kotlin — no framework dependencies

data/
  aviationData/     Aviation repo impl, OFPKG installer, SQLDelight queries
  terrainData/      Terrain repo impl, DEM sampler, PMTiles reader
  searchData/       Search repo impl, in-memory data source
  settingsData/     Settings repo impl, key-value persistence

feature/
  searchFeature/          Text search (State/Intent/Effect/Reducer/Store)
  searchDockFeature/      Search dock UI state machine
  terrainWarningFeature/  Terrain warning state machine

shared/             Cross-cutting wiring: location service, map style, Koin modules

composeApp/         Android app (Compose UI, MapLibre integration)
iosApp/             iOS app (SwiftUI, MapLibre integration)
```

Dependencies flow inward: `app → feature → domain → core`. Data modules depend on `domain` + `core` only.

## Build

**Prerequisites:** JDK 17+, Android SDK 36, Xcode 16+ (for iOS)

### Android

```shell
./gradlew :composeApp:assembleDebug
```

Or use the run configuration in Android Studio / Fleet.

### iOS

Open `iosApp/` in Xcode and run, or use the KMP run configuration in Fleet.

## MSFS UDP Record/Replay

PigeonNavigator listens for MSFS UDP location packets on port `49002`. A small
stdlib-only Python helper can record the full UDP datagram stream with timestamps
and replay it later at a configurable speed.

Record from MSFS:

```shell
python3 tools/msfs_udp_record_replay.py record recordings/msfs-flight.jsonl --port 49002
```

Replay to a local simulator/app instance at normal speed:

```shell
python3 tools/msfs_udp_record_replay.py replay recordings/msfs-flight.jsonl --host 127.0.0.1 --port 49002
```

Replay twice as fast to a device running the app:

```shell
python3 tools/msfs_udp_record_replay.py replay recordings/msfs-flight.jsonl --host <device-ip> --port 49002 --speed 2
```

On Windows, `WinError 10013` usually means Windows denied access to the UDP
port. Make sure only one process listens on the selected port, allow Python in
Windows Defender Firewall, try running the terminal as Administrator, or switch
MSFS and PigeonNavigator to another UDP port. If the error happens while sending
to a broadcast address such as `192.168.1.255`, add `--broadcast`. If it happens
while recording, check whether Windows has reserved the port:

```powershell
netsh interface ipv4 show excludedportrange protocol=udp
```

## Architecture

Each feature follows the **MVI** pattern with explicit types:

- `State` — immutable data class representing full UI state
- `Intent` — sealed interface of user/system inputs
- `Effect` — sealed interface of one-shot side effects (navigation, alerts)
- `Reducer` — pure function `(State, Intent) → State`
- `Store` — coroutine scope that runs use-cases and emits `StateFlow<State>` + `Flow<Effect>`

Reducers contain no IO. Side effects run in the Store or domain use-cases. Platform APIs are behind interfaces injected via Koin.

## Aviation Data

Aviation data is distributed as `.ofpkg` packages — ZIP archives containing a SQLite database and PMTiles tile archive. You can create `.ofpkg` packages using the Go tool in `data-pipeline/`. A Czech Republic package (`cz.ofpkg`) is bundled in `map-assets/`.

## License

School project — no license specified.
