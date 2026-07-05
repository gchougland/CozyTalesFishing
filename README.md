# Cozy Tales: Fishing

A relaxed shadow-based fishing mod for Hytale, part of the **Cozy Tales** series by **Hexvane**.

## About the mod

Cozy Tales: Fishing adds a new fishing system built around spotting fish in the water and timing your catch.

### Planned gameplay

- **Fish shadows** of varying sizes appear in bodies of water
- **Fishing rods** let the player cast a bobber into the water
- When a bobber lands near a shadow and is in range, the fish **boops the bobber** a number of times before pulling it under
- The player must **rapidly press a button** to reel in and catch the fish

### Planned fish data

Each fish species will have stats and spawn requirements, including:

- **Water type** — ocean, river, or pond
- **Weather** — conditions required for the fish to appear
- **Time of day** — when the fish is available
- **Season / time of year** — possible seasonal availability

## Status

Early setup — the mod loads and builds, but fishing gameplay is not yet implemented.

## Prerequisites

- **Java 25** (JetBrains Runtime recommended for hot reload during development)
- **IntelliJ IDEA** (Community Edition is fine) or another Java IDE
- **Hytale** installed via the launcher (for `runServer`)

## Development

Build the mod:

```bat
.\gradlew.bat build
```

Run the local dev server:

```bat
.\gradlew.bat runServer
```

On first run you may need to authorize the server. Use `runServerNoSync` when editing `src/main/resources` directly so post-exit asset sync does not overwrite your files:

```bat
.\gradlew.bat runServerNoSync
```

## Gradle tasks

| Task | Description |
|------|-------------|
| `build` | Compile and package the mod JAR |
| `runServer` | Start dev server; syncs assets back to source on exit |
| `runServerNoSync` | Same as `runServer` without post-exit asset sync |
| `verifyReleaseJar` | Fail if HytaleServer was accidentally bundled |
| `syncAssets` | Copy `build/resources/main` back to `src/main/resources` |

## Project layout

```
src/main/java/com/hexvane/cozytalefishing/   Java plugin code
src/main/resources/manifest.json              Mod manifest (Gradle token expansion)
src/main/resources/Common/                    Client/shared assets (models, UI, etc.)
src/main/resources/Server/CozyTalesFishing/   Server-side mod data JSON
src/main/resources/Server/Item/Items/         Item definitions (add as needed)
src/main/resources/Server/Languages/          Localization (.lang files)
```

See [Aetherhaven](../Aetherhaven) for a full-featured reference mod using the same patterns.

## HStats setup

This project includes optional [hstats.dev](https://hstats.dev) metrics integration.

1. Register your mod at [hstats.dev](https://hstats.dev).
2. Set your mod UUID in `gradle.properties`:

```properties
hstats_mod_uuid=your-uuid-here
```

Or pass at build time:

```bat
set COZYTALESFISHING_HSTATS_MOD_UUID=your-uuid-here
.\gradlew.bat build
```

Leave `hstats_mod_uuid` empty to disable metrics (the plugin logs a skip message at startup).

**Note:** Only change the package name in `HStats.java` — do not modify the metrics-sending logic per HStats license.

## Optional build options

In `build.gradle.kts`, the `hytale { }` block supports:

- `addAssetsDependency = true` — attach `Assets.zip` for IDE browsing (very large)
- `updateChannel = "pre-release"` — target pre-release server builds

## Bundling dependencies

If your mod needs to ship third-party libraries in the release JAR, see Aetherhaven's `modEmbed` pattern in its `build.gradle.kts`. Do not merge `runtimeClasspath` — it can include HytaleServer.
