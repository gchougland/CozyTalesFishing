# Changelog

## [1.1.0] - Unreleased

### Added

- **Mod fishable fluids** — Other mods can register custom fluids via `CozyTalesFishing/Config/FishableFluids` JSON (or `CozyTalesFishingIntegration.registerFishableFluid`). Species in any pack can use a `Fluid` spawn rule. See [docs/mod-fish-and-fluids.md](docs/mod-fish-and-fluids.md).
- **MMO Skill Tree integration** — Journal fish award FISHING XP via `MMOSkillTreeAPI` (optional dep `Ziggfreed:MMOSkillTree`, plugin classloader bridge). XP scales by rarity plus first-catch bonus. **Fishing Luck** (FISHING-scoped `LUCK_PCT`) adds treasure shadow spawn chance and can grant extra journal fish on the same catch; tunable in `FishingModConfig`.
- **Fishing rod salvage** — All five Cozy fishing rods can be salvaged at the Salvage Bench (outputs match vanilla metal pickaxe salvage per tier; wooden rod matches crude shortbow-style salvage).

### Fixed

- **Aquarium sizing** — Fish may be placed in any aquarium at or above their minimum `AquariumSize` tier (small → medium → grand). Oversized fish still require a larger tank.
- **Aetherhaven Reed quests** — Shop and house quest objectives use proper progression kinds (`plot_blueprint_*`, `construction_built`, `assign_house_resident`, `dialogue_turn_in`) so dialogue turn-in completes after the shop is built or Reed is assigned to a house.

## [1.0.1] - 7/11/2026
### Changed

- **Reed Castwell dialogue** — Reed is now written as a castaway who washed ashore after losing his boat. First-meeting intro, greetings, quest lines, tips, and gifts reflect that story; quest and guide text match.

### Fixed

- **Fish Processor crafting tier** — The Fish Processor recipe at the Fishing Bench now requires bench tier 2 instead of tier 3.
