# Building Planner

Building Planner is a Fabric add-on for Minecraft 26.3. With it, the group plans builds together as Litematica ghost
plans that everyone on the server sees and edits live, then builds them by hand. It hooks into unchanged copies of
Effortless Building, Litematica and Syncmatica. Geo's decisions behind it are in [plan.md](plan.md) ("Decisions").

Choices marked **Open** are player-experience decisions that wait on Geo. Each names the option the agent recommends;
the build keeps each one behind a single function or setting until Geo answers.

## What players get

- **Plans.** A plan is a named box of ghost blocks in the world, shared with everyone on the server. Each player can
  show or hide any plan, and anyone can move or turn it as a whole. Litematica lists each plan's materials.
- **Starting a plan.** **Open (Q1)**: the player marks an area with Litematica's area tool, then a Building Planner key
  asks for a name and creates the empty plan, shared to everyone in one step (recommended); or the player uses
  Litematica's own steps (save the area as an empty schematic, place it, share it with Syncmatica's Share button); or
  a chat command does the key's job.
- **Seeing plans.** **Open (Q2)**: every plan appears for everyone in its place as soon as it's shared, and each player
  can hide it (recommended); or each player loads the plans they want from Syncmatica's list, as Syncmatica works today.
- **Drawing shapes.** With an Effortless Building mode chosen (line, wall, floor, cube, slope, circle, cylinder,
  sphere and the rest), a player's clicks draw the shape into a plan as ghost blocks, with Effortless Building's
  mirror, radial mirror and array (repeat). A shape drawn with the break (left) click erases those ghost blocks from
  the plan. Effortless Building never places or breaks real blocks.
  - Which plan a shape goes into: **Open (Q3)**: the plan the shape starts in, or the player's selected Litematica plan
    when it starts outside every plan (recommended); or always the selected plan; or only the plan the shape starts in,
    with shapes that start outside every plan doing nothing.
  - Where a shape can start: **Open (Q4)**: on a plan's ghost blocks as well as on real blocks (recommended); or on
    real blocks only, as Effortless Building aims today.
- **Growing.** A shape that crosses a plan's edge grows the plan to fit it. Everyone sees the bigger plan without
  reloading it.
- **Fine edits.** Litematica's ten Edit Schematic actions (place, break, replace and fill ghost blocks, singly, in a
  direction or across the plan) change the shared plan live, as shapes do. They only change blocks inside a plan's box;
  they never grow it.
- **Block mixes.** **Open (Q5)**: a key opens a picker that lists every block, with search. The player picks up to
  nine blocks, each with a share (say 80 cobblestone to 20 mossy cobblestone); while the mix is on, shapes and
  Litematica's place, fill and replace use it instead of the held block, and the same key turns it off. A mix of one
  block lets a survival player plan with a block they don't have (recommended). The alternative picker lists only
  blocks in the player's inventory.
- **Undo.** Undo takes back only that player's own changes, newest first: a whole shape, or one Litematica action, at a
  time, in whichever plan it was made. A block someone else has changed since stays as they left it.
  **Open (Q6)**: Effortless Building's Ctrl+Z (and the undo button in its radial menu) undoes and its Ctrl+Y redoes
  (recommended); or Ctrl+Z undoes and there is no redo; or Building Planner has its own keys in the Controls menu.
- **Others' edits.** **Open (Q7)**: the ghost blocks simply change as others edit (recommended); or the changed blocks
  also glow for a few seconds with the editor's name beside them.
- **Deleting.** Anyone can change, move or turn a plan; only the player who shared it can delete it.
- **Messages.** Every message players see goes to Geo for review once there's something built to show it in.

## Geo's goals, and what meets each

| Goal (402 r1, r2) | Met by | Gap |
|---|---|---|
| Measure layouts | Litematica's area tool shows each box's size on screen (`Dim:` in its HUD); a plan's box shows its size to everyone in Litematica's placement list. | Area boxes and their sizes are each player's own; the shared measure is a plan's box. MiniHUD-style personal guides were rejected (402 r2, c2). |
| Plan room sizes and shapes before committing | Effortless Building's shapes drawn into plans; Litematica area boxes; plans grow to fit. | None. |
| See and judge a build (free or fixed camera) | Litematica draws plans as ghost blocks; Freecam moves the camera away from the player. | Effortless Building aims from the player's eyes, not the camera, so shapes can't be drawn from a Freecam view. Litematica's edits aim from the camera entity; whether Freecam's camera counts is unverified. |
| Lay out walls, shapes and components quickly | Effortless Building's eleven build modes, captured into plans. | None. |
| Copy and paste | Effortless Building's array repeats while drawing. Litematica can copy a real area into a new plan (its clone action saves real blocks) and load any saved schematic as a new plan. | No tool copies part of a plan and pastes it elsewhere in a plan. |
| Symmetry | Effortless Building's mirror and radial mirror. | None. |
| Randomized block mixes | Building Planner's block-mix picker (Effortless Building's Randomizer needs the server copy, which Geo kept off the server). | None. |
| A plan everyone on the server sees, built for real by hand | Syncmatica shares plans; Building Planner keeps their blocks live for everyone; Litematica's material list and ghost blocks guide the build. | None. |

## Technical design

### Parts

One jar, mod id `buildingplanner`, installed on the server and in the player pack. It registers no blocks, items or
menus, so it adds nothing to the registries Fabric API syncs at login, and players without mods can still join.

- **Common:** the plan model (regions, palette, growth), the `.litematic` reader and writer, and the network payloads.
- **Server:** the plan store, the edit log and its order, per-player undo, the Syncmatica listener and the delete check.
- **Client:** the Litematica bridge (attach plans to placements, apply edits, capture Edit Schematic writes, grow in
  place), the Effortless Building capture, the block-mix picker and the plan keys.

`fabric.mod.json` depends on Fabric API and Syncmatica, which both sides run. Litematica, MaLiLib and Effortless
Building run only on players' computers, so the add-on lists them under `recommends`, puts every mixin into those mods
in the `client` section of its mixin configs (as Syncmatica does for its Litematica mixins), and uses a mixin config
plugin that skips the Effortless Building mixins when that mod is missing. Injectors use MixinExtras, which Fabric
Loader 0.20.1 bundles (0.5.5).

### Hooks

Checked in the source at: Effortless Building `Requios/effortless-building-multi` branch `26.3` @ `f31a5b5` (release
4.3 for Fabric 26.3); Litematica `sakura-ryoko/litematica` branch `26.3` @ `eb15b39`, where every file below matches
the pack's release 0.29.1 (`b1a2682`) except one line in `SchematicPlacementManager.java`; Syncmatica
`sakura-ryoko/syncmatica` branch `26.3` @ `37a969e` (0.3.20); Fabric API branch `26.3` @ `b8eae12`; Fabric Loader
@ `cb9523b` (0.20.1).

Effortless Building, under `common/src/main/java/nl/requios/effortlessbuilding/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| E1 | `buildpipeline/BuildPipelineClient.java`, `handleClick` | `@WrapWithCondition` on `PacketHandler.sendToServer(PlaceBuildModePacket)`, reading the local `BlockSet blocks` | Take the finished shape (every entry, after mode, modifiers, mix and constraints) into the plan; don't send the packet. `getFirstClickHit()` is still set here, so block states use the first click's face. |
| E2 | same method | `@WrapWithCondition` on `PacketHandler.sendToServer(BreakBuildModePacket)` | Erase the shape's positions from the plan; don't send. |
| E3 | same method | `@WrapWithCondition` on `PlacedBlockTracker.clientTrackAll` | Don't record ghost positions as real blocks the player placed. |
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo (and redo, if Q6 keeps it) to the plan. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While a mix is on, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipeline.java` `isBuildTriggerItem`; `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While a mix is on, treat the held item as Effortless Building's Randomizer, so clicks start shapes and the preview draws each entry's own block. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not plans. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapWithCondition` on `ItemUsageTracker.compute` | No red "missing items" tint: plans don't use up items. |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick`, `getPreviewBlocks`, `resolveFirstClickPos` | `@WrapOperation` on `Level.clip` and on `Level.getBlockState` | Only if Q4 allows starting on ghost blocks: aim at the nearer of the real block and Litematica's ghost block (`RayTraceUtils.traceToSchematicWorld`), and read the ghost block's state when picking the start position. |

Effortless Building's limits come from its config on the player's computer (`config/ServerConfig.java`; survival
defaults: reach 32, 2,000 blocks a shape, 64 a side). The server applies its own caps (below).

Litematica, under `src/main/java/fi/dy/masa/litematica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| L1 | `util/SchematicUtils.java`: `setTargetedSchematicBlockState(BlockPos, BlockState)`, `setSchematicBlockStates`, `setAllStatesToAirExcept(SchematicPlacementManager, PlacementPart, BlockState, Level)`, `replaceAllIdenticalBlocks` | `@WrapOperation` on each `LitematicaBlockStateContainer.set(int, int, int, BlockState)` (the only four writes the ten Edit Schematic actions make, all called from `event/InputHandler.java`) | If the container belongs to a plan, record the position and new state (and, while a mix is on, swap a placed block for the mix's block at that position), then write. Writes made in one client tick are sent as one edit. |
| L2 | `util/SchematicUtils.java`, private static `getReverserTransformedWorldPosition` | `@Invoker` | World position to plan coordinates without clamping. The public `getSchematicContainerPositionFromWorldPosition` clamps to the box, so it can't tell when a shape crosses the edge. |
| L3 | `schematic/LitematicaSchematic.java`, private final maps `blockContainers`, `subRegionPositions`, `subRegionSizes` | `@Accessor` | Grow a region in place: put a bigger container, a new size and (when growing toward negative coordinates) a new region position into the existing maps. |

Public Litematica API the add-on calls: `SchematicUtils.getUntransformedBlockState`;
`LitematicaSchematic.createEmptySchematic`, `createFromFile`, `writeToFile`, `getSubRegionContainer`, `getAreaSize`,
`getSubRegionPosition`; `SchematicMetadata` setters for total blocks, volume and enclosing size;
`SchematicPlacement.createFor`, `toggleLocked`, `resetAllSubRegionsToSchematicValues`;
`SchematicPlacementManager.addSchematicPlacement`, `markAllPlacementsOfSchematicForRebuild`;
`SchematicPlacementEventHandler.registerSchematicPlacementEventListener` (placement added and removed);
`DataManager.getSelectionManager().getCurrentSelection()`.

Syncmatica, under `src/main/java/ch/endte/syncmatica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| S1 | `communication/ServerCommunicationManager.java`, `handle(ExchangeTarget, PacketType, FriendlyByteBuf)` | `@Inject` at head, cancellable, when the type is `REMOVE_SYNCMATIC` | The delete check (below). |
| S2 | `litematica/LitematicManager.java`, `renderSyncmatic(ServerPlacement)` | `@WrapOperation` on `SchematicHolder.getOrLoad(Path)` | Load each plan's file into its own schematic. Litematica caches schematics by path, and Syncmatica names files by content hash, so two plans from one file would otherwise share their blocks. |

Public Syncmatica API the add-on calls: `Syncmatica.getContext(SERVER_CONTEXT | CLIENT_CONTEXT)`;
`SyncmaticManager.addServerPlacementConsumer`, `getPlacement`; `ServerPlacement.getId`, `getOwner` (its `uuid`),
`getDimension`; `ServerCommunicationManager.getGameProfile`; `IFileStorage.getLocalLitematic`;
`LitematicManager.getRenderingSyncmatic`, `isRendered`, `renderSyncmatic`; `CommunicationManager.download`;
`ClientCommunicationManager.getServer`; `ShareLitematicExchange`'s public constructor.

Fabric API: `PayloadTypeRegistry` (`registerLarge` for snapshots), `ServerPlayNetworking`, `ClientPlayNetworking`
(`canSend`), `ServerPlayConnectionEvents`, `KeyMappingHelper`. Both sides' payload handlers run on the main thread
(server thread, render thread), which the ordering below relies on.

### Roles of Syncmatica and the add-on

Syncmatica carries each plan's existence, name, creator, dimension, position, rotation and mirror: sharing, moving,
turning, listing and deleting work as Syncmatica does them, for everyone. Its stored file is fixed by a content hash,
so it can't carry changed blocks; the add-on carries a plan's blocks and size, live, from the server to everyone who
has the plan loaded. Every placement shared through Syncmatica on this server is a plan.

### Plan coordinates

Edits name blocks in plan coordinates: positions relative to the schematic's origin, before the placement's rotation
and mirror, with block states unrotated (`getUntransformedBlockState`). Moving or turning a plan doesn't change them,
and growth doesn't either: growth moves a region's minimum corner, never the origin. So an edit made before someone
else grows, moves or turns the plan still lands where its author meant. Each edit also names its region. Plans the
add-on creates have one region; a shared multi-region schematic keeps its regions, and a block outside all of them goes
to the region holding the shape's first block, or the first region.

### Live sync

Payloads (namespace `buildingplanner`):

- Client to server: `subscribe(plan)`, `unsubscribe(plan)`, `edit(plan, clientSeq, kind, changes)` where `kind` is
  shape, erase or Litematica and `changes` is a list of region, plan position and block state, and `undo` (and
  `redo` if Q6 keeps it).
- Server to client: `hello(protocolVersion, limits)` at join; `snapshot(plan, rev, regions)` with each region's
  minimum corner, size, palette and packed blocks; `applied(plan, rev, author, clientSeq, growth, changes)`;
  `rejected(plan, clientSeq, reason)`; `notice(key, args)` for messages players see.

Block states travel as ids in the block-state registry (both sides run the same vanilla blocks); files store them by
name and properties.

Order and conflicts:

1. The server applies edits one at a time on its main thread, in the order they arrive. Each applied edit raises the
   plan's revision by one and goes to every subscriber, its author included.
2. Each block takes the value of the last edit, in the server's order, that wrote it. There are no locks: several
   players can edit one plan, even the same blocks, and everyone ends with the same plan.
3. A client applies `applied` messages in the order they arrive (one connection keeps them in order). A gap in
   revisions makes it ask for a fresh snapshot.
4. Litematica edits show at once on the author's screen, since Litematica writes them before the add-on sends them;
   shapes show when the server's `applied` comes back. When someone else's edit to the same block lands between an
   author's write and its echo, the author sees that block flip once, then settle on the server's order.
5. The server checks each edit: the plan exists, the player has it loaded, the batch is under the block cap, and growth
   stays within the size cap. A failed check returns `rejected`, and the client takes a fresh snapshot to drop its own
   early copy.

Starting caps, in the server's config file: 50,000 blocks an edit, 512 blocks a side for a plan.

### Players joining mid-edit

When a plan's Litematica placement appears on a player's screen (Syncmatica's render after a download, or the
player's own share), the add-on sends `subscribe`. The server answers with a `snapshot` at the current revision, then
every later `applied`, on the same connection, so nothing falls between them. The client grows the region in place if
the plan has grown since its file was shared, fills the blocks from the snapshot, and redraws. The add-on unsubscribes
when the placement is removed from that player's Litematica list. If Q2 makes plans appear for everyone, the client
also starts Syncmatica's download for each new plan when the player is in its dimension (Syncmatica only draws a
plan in its own dimension).

### Growth

When a shape's block falls outside its region, the server works out the region's new minimum corner and size (the old
box joined with the shape's blocks), copies the blocks into a bigger grid, and sends the growth in the same `applied`
as the shape. Each client, with the placement unlocked as Syncmatica does when it updates one: builds the bigger
container, copies the old blocks across, writes the new container, size and region position into the schematic's maps
(L3), resets the placement's sub-regions to the schematic's values, updates the metadata's volume and enclosing size,
and marks the plan for redraw. The placement, its Syncmatica identity, origin and position stay the same, so no one
reloads. Plans never shrink; undo doesn't shrink them either.

### Plan storage on the server

The server keeps each plan in memory and writes it to `<world>/building-planner/plans/<plan id>.litematic` in
Litematica's own format (version 7: palette and packed block states per region), through a temporary file and a
rename, at most every 30 seconds while it has changed, and when the server stops. When a placement is first shared,
the add-on reads its blocks from the file Syncmatica stored (`syncmatics/<hash>.litematic`). At start, each Syncmatica
placement loads the add-on's file, or Syncmatica's when the add-on has none. A deleted plan's file moves to
`<world>/building-planner/deleted/`, where it can be put back by hand. Revisions start again at each server start;
players reconnect and take snapshots.

### The delete check

Syncmatica's delete reaches the server as `REMOVE_SYNCMATIC` with the plan's id. Hook S1 reads the id without
consuming it, looks up the placement, and compares its owner (the player who shared it, which Syncmatica records and
saves in `placements.json`) with the sender (`getGameProfile(source)`). If they differ, the hook cancels the delete,
so no one's copy is removed, and sends the sender a `notice`. Syncmatica's move and turn stay open to everyone. A plan
whose creator never returns can't be deleted in the game.

### Undo

The server keeps an undo list for each player, across plans: each step is one edit (a shape, an erase, or one tick of
Litematica writes) with each block's state before and after. Undo takes the player's newest step and sets each of its
blocks back to its before-state only if the plan still holds that step's after-state there; blocks someone has changed
since stay as they are, and a `notice` says how many. The undo goes out as an ordinary `applied` edit. Steps survive
growth and moves (plan coordinates), are dropped with their plan when it's deleted, keep the newest 50 per player
(Effortless Building's own depth), and last until the server restarts. With redo (Q6), an undone step goes onto a redo
list, cleared by the player's next new edit.

### The block-mix picker

The mix is a list of up to nine blocks with a weight each, kept on the player's computer. While it's on, hook E5 gives
each shape position its block, chosen by a fixed hash of the position over the weights, so the preview and the plan
agree; hook E6 makes Effortless Building start shapes and draw the preview from those blocks; and hook L1 swaps each
block Litematica's place, fill and replace actions write for the mix's block at that position. Block states come from
each block's placement rules with the first click's face and the player's facing, then the shape's mirror and turn.
Without a mix, shapes use the held block, as in Effortless Building. Which blocks the picker lists, its key and its
screen wait on Q5.

### When the server lacks the mod

The client checks `ClientPlayNetworking.canSend` for its payloads after joining. Without them, Building Planner turns
itself off for that server: shapes are still dropped (Effortless Building's packets would be anyway) and a `notice`
says Building Planner isn't on the server; Litematica's edits stay on that player's screen, as plain Litematica does.
A protocol-version mismatch in `hello` does the same. The server sends nothing to a player who can't receive it
(`ServerPlayNetworking.canSend`), so players without the add-on, or without any mods, join and play as before; with
Syncmatica alone they see each plan as it was first shared.

### The add-on's licence

**Open (Q8).** Effortless Building and Litematica are LGPL-3.0 and Syncmatica is CC0. The add-on calls their code
and copies none of it, so its own licence is free.

## Unverified

- Growth in place redraws correctly and quickly (L3 and the reset), and Syncmatica's later moves keep working on a
  grown plan.
- Whether Litematica's edits aim from Freecam's camera.
- The add-on's `.litematic` reader and writer match Litematica byte for byte; tests check them against files Litematica
  writes.
