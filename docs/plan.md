# Plan

## Current setup

**Server.** Minecraft 26.3 on Fabric (loader 0.20.1, server launcher 1.1.2) with Fabric API 0.166.2+26.3 and
Syncmatica 0.3.20 (`/home/minecraft/mods/`), on Geo's VPS. The systemd unit
`minecraft.service` runs it as root from `/home/minecraft`:
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java -Xmx6G -Xms1G -jar fabric-server-mc.26.3-loader.0.20.1-launcher.1.1.2.jar nogui`,
port 25565. Survival, hard, whitelist enforced (11 players), online mode. Neither mod adds blocks or items, so players without
mods should still be able to join (no unmodded client has joined yet to confirm it). The world (`world/`) is 1.9 GB.
Archives sit beside it: `minecraft-backup-2026-11-14-pre-fabric.tar.gz` (the world just before the switch to Fabric,
1.5 GB), `minecraft-archive-2026-10-29-1.21.11.tar.gz` and `minecraft-backup-2025-12-12.tar.gz`.
The vanilla `server.jar` stays in place: to switch back, change the jar in `ExecStart` to `server.jar`, then run
`systemctl daemon-reload` and `systemctl restart minecraft.service`. The VPS has 30 GB of RAM (about 16 GB available) and
163 GB of free disk; it also hosts other services.

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
- The player guide covers Prism Launcher only, with no steps for the official launcher (410, comment c1).
- The server's errors email Geo and reach the project thread as inbox notices through the VPS's existing Grafana and
  Prometheus, as inbox request 415 describes; a known error's rate alerts above 30 an hour and above 3 times its
  hourly average across the day before (415, Q1 A and Q2 A).

The planning mod, from inbox request 411:

- It's named Building Planner (Q8).
- Named plans: each plan is a box that players show, hide, move and turn as a whole, with its own material list (Q1 A).
- A shape that crosses a plan's edge grows the plan to fit it (Q2 A).
- Anyone can change a shared plan; only its creator can delete it (Q3 B).
- Undo takes back only that player's own changes in a plan (Q4 A).
- Changes made with Litematica's own Edit Schematic tools sync live too (Q5 A).
- Effortless Building stays off the server, on players' computers only, so players without the pack can still join.
  Its Randomizer item needs the server copy, so Building Planner gives players its own block-mix picker (Q6 A).
- One add-on that hooks into unchanged Effortless Building, Litematica and Syncmatica, with its own licence (Q7 A).

## Agent-ready work

- **Player pack** (approved, 408 Q1 A): a packwiz pack of the player mods for 26.3 at a public web address, and a Prism
  instance file that runs packwiz's installer before each launch, and a player guide for Prism Launcher only.
- **Error alerts** (approved, 415): a watcher beside the server that gives each error a short id and exports counts
  for Prometheus, a `minecraft.service` drop-in that records how the process ended, and the Grafana rules, contact
  point and route, as inbox request 415 describes.

## Design work: Building Planner

Geo's decisions from inbox request 411 are above. As 411 Q7 described it, the add-on takes each finished Effortless
Building shape where Effortless Building would send it to the server and writes it into the plan instead; it sends
Litematica's Edit Schematic writes live; it grows a plan by replacing its placement with a bigger schematic; and it
sends live changes over its own connection, sharing the updated plan through Syncmatica for players who join later,
with an owner check on Syncmatica's delete. How players use it (starting a plan, the block-mix picker, undo, menus
and keys) and the add-on's licence are still open. The messages players see go to Geo for review once something is
built.

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
