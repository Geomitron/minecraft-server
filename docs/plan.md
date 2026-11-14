# Plan

## Current setup

**Server.** Vanilla Minecraft 26.3 on Geo's VPS, run by the systemd unit
`minecraft.service` as root from `/home/minecraft`:
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java -Xmx6G -Xms1G -jar server.jar nogui`, port 25565. Survival, hard,
whitelist enforced (11 players), online mode. The world (`world/`) is 1.9 GB. Earlier archives sit beside it:
`minecraft-archive-2026-10-29-1.21.11.tar.gz` and `minecraft-backup-2025-12-12.tar.gz`. No mods or plugins. The VPS has
30 GB of RAM (about 16 GB available) and 163 GB of free disk; it also hosts other services.

**Players.** Unmodded today. Geo prefers Prism Launcher (Geo, inbox request 402). Geo is new to how Minecraft mods
work (Geo, inbox request 408), so explanations start from what a mod, Fabric and an instance are.

## Decisions

From inbox requests 401 and 402 (revisions 1 to 4):

- Keep the existing 26.3 world (401).
- Stay close to vanilla: no Create, nothing "too far off of a vanilla experience" (402 r1).
- The goal is planning: measure layouts, plan room sizes and shapes before committing, see and judge how a build looks
  (a free or fixed camera), and lay out walls, shapes and other components quickly, with copy and paste and symmetry,
  as a plan everyone on the server sees, which the group then builds for real by hand (402 r1).
- Effortless Building's shapes, mirror, repeat and Randomizer edit only Litematica ghost plans, never real blocks
  (402 r2, comment c1).
- Randomized block mixes, such as cobblestone with some mossy cobblestone (402 r2, c1).
- Layout guides are shared with everyone on the server; client-only guides like MiniHUD's are rejected (402 r2, c2).
- No automation (block placers, breakers, quarries) for now (402 r2).
- Build the planning mod with live shared editing: each change to a plan reaches everyone as it's made, and several
  players can edit one plan at once (402 r4, Q1 B).
- Players get their mods from a Prism Launcher instance that updates itself at each launch (packwiz's installer
  run before launch, reading a pack at a public web address), so new test builds reach everyone (408, Q1 A).
- Switch the survival server to Fabric with Syncmatica now (408, Q2 A), as "Server switch" below describes.
- The work runs as its own T3 project, "Minecraft Server", with a project thread of that name; its workspace is this
  repository, which becomes a public GitHub repository under Geo's account once there's code to publish (409, and
  402 r4 Q1).
- Players run Litematica (with MaLiLib), Syncmatica and Freecam, with Fabric API; the server runs Fabric, Fabric API
  and Syncmatica. These are the mods 402 r2 to r4 proposed and 408 described when Geo approved the pack and the switch.

## Agent-ready work

- **Server switch** (approved, 408 Q2 A): back up `world/`, replace `server.jar` in `minecraft.service` with Fabric's
  server launcher for 26.3, add Fabric API and Syncmatica (26.3 builds), restart when no one is online, and check the
  world loads. Players who install nothing can still join; switching back to `server.jar` stays possible.
- **Player pack** (approved, 408 Q1 A): a packwiz pack of the player mods for 26.3 at a public web address, and a Prism
  instance file that runs packwiz's installer before each launch. It includes steps for players on the official
  launcher (install Fabric, copy the mods by hand, no automatic updates).

## Design work: the planning mod

Geo decides these in inbox request 411, which holds the questions and their options: the plan model, growth past a
plan's edge, permissions, undo, live sync for Litematica's own Edit Schematic tools, whether Effortless Building goes on
the server, how the mod is built, and its name. The messages players see go to Geo for review once something is built.

### Source evidence (read 2026-11-13)

- **Effortless Building**: `Requios/effortless-building-multi`, branch `26.3` (`f31a5b5`, 2026-10-10), LGPL-3.0.
  `common/.../buildpipeline/BuildPipelineClient.java` computes the full block set on the client (build mode, then
  modifiers, then constraints) and on the last click sends `PlaceBuildModePacket` with only the click positions and
  options; the server rebuilds the set itself (`BuildPipeline.SERVER.runServerPipeline`). The preview runs the same
  pipeline each frame. A plan mode can take the client's block set at that send and write it into a plan instead.
  - On the server it turns away players without it: its Fabric entry point (`fabric/.../EffortlessBuilding.java`)
    registers the item `effortlessbuilding:randomizer_tool` and a menu type in built-in registries, and Fabric API
    disconnects a client that can't receive them (`FabricMC/fabric-api` branch `26.3` @ `b8eae12`,
    `fabric-registry-sync-v0/.../RegistrySyncManager.java`, `configureClient`).
  - Without the server copy, its client still previews but places nothing: `FabricNetworkHelper.sendToServer` sends
    without checking the server can receive it, and the server drops the packet (issue #16, the owner's diagnosis:
    https://github.com/Requios/effortless-building-multi/issues/16).
  - The Randomizer works only while the main hand holds `RandomizerToolItem` (`common/.../RandomizerSystem.java`), an
    item the server must register.
  - Its server settings can turn off breaking (`survivalAllowBreaking`) but not placing (`common/.../ServerConfig.java`).
  - Its undo runs on the server, per player, 50 steps, over real block changes only (`common/.../UndoManager.java`).
- **Litematica**: `sakura-ryoko/litematica`, branch `26.3` (`eb15b39`), LGPL-3.0. `util/SchematicUtils.java`
  (private `setTargetedSchematicBlockState`) writes a block into a placement's sub-region container only inside the
  sub-region's box, then calls `markChunkForRebuild`; the pieces it uses (`getAllPlacementsTouchingChunk`,
  `getSchematicContainerPositionFromWorldPosition`, `getUntransformedBlockState`) are public.
  `LitematicaSchematic.createEmptySchematic(area, author)` makes an empty plan from an area selection.
  - A plan's region can't grow in place: `schematic/LitematicaSchematic.java` keeps block containers and region sizes
    in private final maps with no resize, and `SchematicPlacement` holds its schematic in a `final` field. Growing
    means building a bigger schematic, copying the blocks in and replacing the placement, or a mixin into those maps.
  - All ten Edit Schematic actions run from `event/InputHandler.java` into `util/SchematicUtils.java`, which writes
    blocks only through four `container.set` calls, each also marking the plan modified: a hook point for live sync.
- **Syncmatica**: `sakura-ryoko/syncmatica`, branch `26.3` (`37a969e`), CC0. `data/ServerPlacement` identifies a
  plan's contents by a fixed hash of its file (`hashValue`); `MODIFY` packets carry only placement values (origin,
  rotation, mirror, sub-regions). Block changes don't sync today; a changed plan must be shared again as a new file.
  - Each downloaded file is checked against the plan's fixed `hashValue` (`communication/exchange/DownloadExchange.java`,
    `data/FileStorage.java` `hashCompare`), so it can't carry a changed plan.
  - It has no owner checks: any player can move or delete any shared plan, one move per plan at a time
    (`communication/ServerCommunicationManager.java`, `communication/exchange/ModifyExchangeServer.java`).
- **Upkeep** (GitHub API, commits on each default branch in the year to 2026-11-13): Effortless Building 80,
  Litematica 100 or more (the API's page limit, translations included), Syncmatica 26. The files an add-on would hook
  changed less: `BuildPipelineClient.java` 6 times, `SchematicUtils.java` 7. A changed copy of an LGPL-3.0 mod keeps
  that licence and publishes its source; an add-on's licence is free; CC0 asks nothing.

Unverified: how long a player's reload takes when a plan is replaced by a bigger one, and whether a Fabric player who
lacks Effortless Building is turned away like an unmodded one (likely, from the registry code).
