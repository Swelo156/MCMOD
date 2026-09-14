# UnstableCompanions

A Fabric mod (Minecraft 1.21.11, Java 21) skeleton for spawning persistent,
AI-driven companion characters based on the Unstable Universe / Unstable SMP
roster. This is scaffolding, not a finished mod: entity behaviors, structure
generation, dialogue, and rendering all have clearly marked `TODO`s where
real content/assets/game-design decisions need to slot in.

## Design goals baked into the architecture

- **Performance first.** Companions near a player behave normally (full
  Goal-based AI, ticked every server tick like any mob). Companions far from
  every player are pulled into a coarse, batched "offline simulation" instead
  (see `persistence/OfflineSimulationManager`), so a world with hundreds of
  companions doesn't tank TPS. Everything's tunable in `config/ModConfig`.
- **One entity type per character, one shared base class.** Behavior is
  shared through `entity/CompanionEntity`; personality is expressed via
  character-specific `Goal`s (see `ai/goal/`) rather than a pile of
  `if (characterId.equals(...))` branches in one file.
- **Data over code where it makes sense.** Numeric personality tuning lives
  in `config/CharacterTraits`, not hardcoded in goal logic, so balancing
  doesn't require touching Java.
- **Dialogue is a seam, not a dependency.** `ai/dialogue/DialogueProvider`
  ships a static line-bank fallback and documents exactly how to swap in a
  local or cloud LLM later without the rest of the mod caring.

## Package layout

```
com.unstablecompanions
├── UnstableCompanionsMod        - common entrypoint: registration, tick hooks
├── UnstableCompanionsClient     - client entrypoint: renderers (stubbed)
├── config/
│   ├── ModConfig                - performance & gameplay tuning (JSON-backed)
│   └── CharacterTraits          - per-character numeric "personality dials"
├── entity/
│   ├── CompanionEntity           - shared base: relationships, faction link, NBT, dialogue hook
│   ├── ModEntities                - EntityType registrations
│   └── character/                 - one class per starting character
├── ai/
│   ├── goal/                      - one Goal class per unique mechanic
│   └── dialogue/DialogueProvider - LLM integration seam (see class doc)
├── relationship/
│   ├── RelationshipType           - Neutral/Friend/Enemy/Rival/RightHand
│   ├── RelationshipData           - per-pair score + tension, NBT round-trip
│   └── RelationshipManager        - owned by each CompanionEntity
├── faction/
│   ├── Faction                    - player-founded or villain-led group
│   ├── FactionRank                - Recruit -> Member -> Trusted -> RightHand -> Leader
│   └── FactionManager              - world-scoped registry + faction-archetype ticking
├── persistence/
│   ├── CompanionPersistentState   - world-attached save data (factions)
│   └── OfflineSimulationManager   - the "alive while offline" batching system
├── item/
│   └── ModItems                   - companion_spawner, stab_shot, nuke_shot
├── network/
│   ├── SpawnCompanionPayload      - client->server "spawn this character" message
│   └── ModNetworking               - registers the channel, does the server-side spawn
├── client/gui/
│   └── CompanionSpawnScreen       - the in-game spawn menu (see "spawning companions" below)
└── util/ModConstants
```

## Character -> mechanic -> code map

| Character     | Mechanic                                   | Where it lives |
|---------------|---------------------------------------------|----------------|
| FlameFrags    | Worthiness meter, confronts/attacks over rival friendships | `ai/goal/WorthinessConfrontGoal` |
| Wemmbu        | Eggchan revenge hunt, stab/nuke shots       | `ai/goal/RevengeHuntGoal`, `item/ModItems` |
| SpokeIsHere   | Ride-or-die rescue, all-out damage boost    | `ai/goal/RideOrDieRescueGoal` |
| Wifies        | Near-impossible prisons, betrayal risk      | `ai/goal/BuildPrisonGoal` |
| JumperWho     | Spy network, leaves over reckless choices   | `ai/goal/SpyNetworkGoal` |
| Jaden_MAN     | Opportunistic betrayal, gear appreciation   | `ai/goal/OpportunisticBetrayalGoal` |
| LettuceK      | Founds "The Law"                             | `entity/character/LettuceKEntity`, `faction/FactionManager#tickLawFaction` |
| Ashswagg      | Founds "Invisible Mafia", secret hits        | `entity/character/AshswaggEntity`, `faction/FactionManager#tickMafiaFaction` |
| JamatoP       | Founds "NULL/Purgatory", hunter groups       | `entity/character/JamatoPEntity`, `faction/FactionManager#tickPurgatoryFaction` |
| (any leader)  | Right-hand offer at high friendship          | `ai/goal/RecruitGoal#offerRightHand` |
| (player)      | Found own faction, recruit, shared base, ranks | `faction/FactionManager#createPlayerFaction`, `faction/FactionManager#tickPlayerFaction` |

## What's intentionally NOT implemented yet

These are flagged with `TODO` comments at the exact call site so they're easy
to find:

- Actual structure generation for Wifies' prisons and NULL/Purgatory (needs
  NBT structure templates / assets).
- Real capture/prison state tracking that `RideOrDieRescueGoal.isCaptured`
  and the villain hunter groups would read/write.
- Full NBT round-trip for `Faction` (members, standings, home base position)
  and `CompanionPersistentState`.
- Entity renderers/models/textures (client stub only).
- The actual LLM call in `DialogueProvider` (fallback line-bank ships instead).
- A "ghost companion" lightweight data model for companions in fully
  unloaded chunks, if true always-on offline progression (not just
  "far from player but still loaded") is required.

## Extending with a new character

1. Add a `Traits` entry in `config/CharacterTraits`.
2. Create `entity/character/<Name>Entity extends CompanionEntity`, implement
   `getCharacterId()` and `initCustomGoals()`.
3. Write any new `Goal`s under `ai/goal/`.
4. Register the entity type in `entity/ModEntities` and its default
   attributes in `UnstableCompanionsMod#registerDefaultAttributes`.
5. Add lang entries and (eventually) a texture/model.

## Building

The project ships with a working Gradle wrapper, so you don't need Gradle
installed separately -- just a Java 21 JDK.

**Easiest way:**
```
python build.py
```
This checks your Java version, runs the build, and tells you exactly where
the resulting jar is (or, on failure, shows just the relevant error lines
instead of Gradle's full noisy output). See `python build.py --help` for
options, including `--mods-dir` to auto-copy the jar into your Minecraft
`mods` folder.

**Manual way:**
```
./gradlew build        (Mac/Linux)
gradlew.bat build       (Windows)
```
The jar lands at `build/libs/unstablecompanions-0.1.0-alpha.jar`.

## In-game: spawning companions

Right-click while holding a **Companion Spawner** item (give it to yourself
with `/give @s unstablecompanions:companion_spawner`) to open a menu listing
all nine characters with a one-line description of their mechanic. Clicking
one sends a server-authoritative spawn request -- see `network/ModNetworking`
and `client/gui/CompanionSpawnScreen`.

## Changelog / notable fixes since the initial skeleton

This project targets a fast-moving API (Minecraft 1.21.11 / Fabric Loom
1.14), and several breaking changes landed in Minecraft between when this
skeleton was first sketched and now. Fixed so far:

- `Entity#getWorld()` was renamed to `getEntityWorld()` (1.21.9+) -- updated
  every call site.
- Entity NBT persistence moved from `writeCustomDataToNbt(NbtCompound)` /
  `readCustomDataFromNbt(NbtCompound)` to a View-based API,
  `writeCustomData(WriteView)` / `readCustomData(ReadView)` (1.21.6+) --
  `CompanionEntity` rewritten to bridge its existing NBT-based relationship
  serialization through this via `NbtCompound.CODEC`.
- `PersistentState.Type` was removed in favor of the Codec-based
  `PersistentStateType` record -- `CompanionPersistentState` rewritten
  accordingly.
- Both `Item` and `EntityType` registration now require a `RegistryKey` set
  *before* construction (1.21.2+) -- `ModItems` and `ModEntities` fixed.
- `EntityAttributeInstance#removeModifier` takes the actual
  `EntityAttributeModifier` instance, not a bare `Identifier` --
  `RideOrDieRescueGoal` now holds onto the modifier it applied so it can
  remove the same instance.
- An `NbtList` index accessor was wrong (`getCompoundOrEmpty(int)` doesn't
  exist; it's `getCompound(int)`) -- fixed in `RelationshipManager`.
- The Gradle wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) was
  missing entirely from the first drop of this project -- added, pinned to
  Gradle 9.0 (the version Loom 1.14 documents support for).

None of this has been compiled against the real 1.21.11 jar+mappings by me
directly (my environment can't reach Mojang/Fabric's servers) -- it's fixed
against documented API changes and release notes. If `python build.py`
still surfaces an error, **paste the exact error text** (the filtered output
it prints, or full output via `--verbose`) and it's almost always a precise,
one-line fix once we can see it.

