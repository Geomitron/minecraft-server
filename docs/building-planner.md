# Building Planner

Building Planner is a Fabric add-on for Minecraft 26.3. With it, the group plans builds together as Litematica ghost
plans that everyone on the server sees and edits live, then builds them by hand. It hooks into unchanged copies of
Effortless Building, Litematica and Syncmatica. Geo's decisions behind it are in [plan.md](plan.md) ("Decisions").

## What players get

- **Plans.** A plan is a named box of ghost blocks in the world, shared with everyone on the server. Each player can
  show or hide any plan, and anyone can move or turn it as a whole. Litematica lists each plan's materials.
- **Starting a plan.** The player marks an area with Litematica's area tool, presses Building Planner's "Start a plan"
  key (N unless the player changes it in Controls) and types a name. The empty plan is created and shared to everyone
  in one step.
- **Seeing plans.** Every plan appears for everyone in its place as soon as it's shared, and each player can hide it:
  with Litematica's rendering toggle in its placement list, which Litematica remembers, or with Syncmatica's Unload,
  which lasts until the player next joins.
- **Drawing shapes.** With an Effortless Building mode chosen (line, wall, floor, cube, slope, circle, cylinder,
  sphere and the rest), a player's clicks draw the shape into a plan as ghost blocks, with Effortless Building's
  mirror, radial mirror and array (repeat). A shape drawn with the break (left) click erases those ghost blocks from
  the plan. Effortless Building never places or breaks real blocks.
  - A shape goes into the plan it starts in, or into the player's selected Litematica plan when it starts outside every
    plan.
  - A shape can start on a plan's ghost blocks as well as on real blocks.
- **Growing.** A shape that crosses a plan's edge grows the plan to fit it. Everyone sees the bigger plan without
  reloading it.
- **Fine edits.** Litematica's ten Edit Schematic actions (place, break, replace and fill ghost blocks, singly, in a
  direction or across the plan) change the shared plan live, as shapes do. They only change blocks inside a plan's box;
  they never grow it.
- **Block mixes.** A key opens a picker that lists every block in the game, with search and a toggle that filters the
  list to the blocks in the player's inventory. The player picks up to nine blocks, each with a share (say 80
  cobblestone to 20 mossy cobblestone); while the mix is on, shapes and Litematica's place, fill and replace use it
  instead of the held block, and the same key turns it off. A mix of one block lets a survival player plan with a block
  they don't have.
- **Undo and redo.** Undo takes back only that player's own changes, newest first: a whole shape, or one Litematica
  action, at a time, in whichever plan it was made. A block someone else has changed since stays as they left it.
  Effortless Building's Ctrl+Z (and the undo button in its radial menu) undoes, and its Ctrl+Y (and the redo button)
  redoes the newest undone change, until the player's next new change.
- **Others' edits.** When someone else changes a plan, the changed ghost blocks glow for a few seconds, with the
  editor's name beside them.
- **Deleting.** Anyone can change, move or turn a plan; only the player who shared it, or a server operator, can delete
  it.
- **Copy and paste.** After the first version works, a player can copy part of a plan and paste it elsewhere in a plan.
- **Messages.** Every message players see goes to Geo for review once there's something built to show it in.

## Geo's goals, and what meets each

| Goal (402 r1, r2) | Met by | Gap |
|---|---|---|
| Measure layouts | Litematica's area tool shows each box's size on screen (`Dim:` in its HUD); a plan's box shows its size to everyone in Litematica's placement list. | Area boxes and their sizes are each player's own; the shared measure is a plan's box. MiniHUD-style personal guides were rejected (402 r2, c2). |
| Plan room sizes and shapes before committing | Effortless Building's shapes drawn into plans; Litematica area boxes; plans grow to fit. | None. |
| See and judge a build (free or fixed camera) | Litematica draws plans as ghost blocks; Freecam moves the camera away from the player. | Effortless Building aims from the player's eyes, not the camera, so shapes can't be drawn from a Freecam view. Litematica's edits aim from the camera entity; whether Freecam's camera counts is unverified. |
| Lay out walls, shapes and components quickly | Effortless Building's eleven build modes, captured into plans. | None. |
| Copy and paste | Effortless Building's array repeats while drawing. Litematica can copy a real area into a new plan (its clone action saves real blocks) and load any saved schematic as a new plan. | No tool yet copies part of a plan and pastes it elsewhere in a plan; Building Planner adds one after the first version works. |
| Symmetry | Effortless Building's mirror and radial mirror. | None. |
| Randomized block mixes | Building Planner's block-mix picker (Effortless Building's Randomizer needs the server copy, which Geo kept off the server). | None. |
| A plan everyone on the server sees, built for real by hand | Syncmatica shares plans; Building Planner keeps their blocks live for everyone; Litematica's material list and ghost blocks guide the build. | None. |

## Technical design

### Parts

One jar, mod id `buildingplanner`, installed on the server and in the player pack. It registers no blocks, items or
menus, so it adds nothing to the registries Fabric API syncs at login, and players without mods can still join.

- **Common:** the plan model (regions, palette, growth), the `.litematic` reader and writer, and the network payloads.
- **Server:** the plan store, the edit log and its order, per-player undo, the Syncmatica listener and the delete check.
  They run only on a dedicated server, which is how the group plays; in a singleplayer or LAN world Syncmatica works
  as it does without the add-on.
- **Client:** the Litematica bridge (attach plans to placements, apply edits, capture Edit Schematic writes, grow in
  place), starting a plan, the edit glow, the Effortless Building capture and the block-mix picker.

They're built in the steps [plan.md](plan.md) lists. Step 6 built the client's Litematica bridge (hooks L1, L3, S2 and
E4), starting a plan and the edit glow. Step 7 built the Effortless Building capture (E1 to E3 and E5 to E9), the
block-mix picker with its inventory toggle, the mix's part of L1, and L2. Step 8 adds the client's notice when the
server lacks the add-on. The dev clients run Effortless Building 4.3 for Fabric 26.3 (Modrinth version `R8eAP63A`, set
in `mod/gradle.properties`), the release built from the source the hooks were checked in; the dev server doesn't run
it.

`fabric.mod.json` depends on Fabric API and Syncmatica, which both sides run. Litematica, MaLiLib and Effortless
Building run only on players' computers, so the add-on lists them under `suggests` (Fabric Loader warns at start about a missing `recommends`, not a missing `suggests`), puts every mixin into those mods
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
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo and redo to the plan. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While a mix is on, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipeline.java` `isBuildTriggerItem`; `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While a mix is on, treat the held item as Effortless Building's Randomizer, so clicks start shapes and the preview draws each entry's own block. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not plans. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapOperation` on both `ItemUsageTracker.compute` calls, clearing the tracker instead | No red "missing items" tint: plans don't use up items. |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `resolveFirstClickPos` | `@WrapOperation` on `ClientLevel.clip`; on `Level.getBlockState` | Let shapes start on ghost blocks: aim at the nearer of the real block and Litematica's ghost block (`RayTraceUtils.traceToSchematicWorld`), and read the ghost block's state, where the real block can be replaced, when picking the start position, so the shape goes beside it. |

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
`getBlockEntityMapForRegion`, `getEntityListForRegion`; `LitematicaBlockStateContainer`'s constructor, `get` and `set`;
`SchematicMetadata` setters for name, volume and enclosing size; `SchematicHolder.getOrLoad`, `addSchematic`;
`SchematicPlacement.createFor`, `toggleLocked`, `resetAllSubRegionsToSchematicValues`,
`getRelativeSubRegionPlacement`; `SchematicPlacementManager.getAllSchematicsPlacements`, `addSchematicPlacement`,
`markAllPlacementsOfSchematicForRebuild`, `markChunkForRebuild`; `PositionUtils.getValidBoxes`,
`getTransformedPlacementPosition`, `getTransformedBlockPos`; `DataManager.getSelectionManager().getCurrentSelection()`,
`getSchematicsBaseDirectory`. From MaLiLib: `RenderEventHandler.registerWorldLastRenderer` and `RenderUtils`' batched
box and text-plate drawing.

Syncmatica, under `src/main/java/ch/endte/syncmatica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| S1 | `communication/ServerCommunicationManager.java`, `handle(ExchangeTarget, PacketType, FriendlyByteBuf)` | `@Inject` at head, cancellable, when the type is `REMOVE_SYNCMATIC` | The delete check (below). |
| S2 | `litematica/LitematicManager.java`, `renderSyncmatic(ServerPlacement)` | `@WrapOperation` on `SchematicHolder.getOrLoad(Path)` | Load each plan's file into its own schematic. Litematica caches schematics by path, and Syncmatica names files by content hash, so two plans from one file would otherwise share their blocks. When the cached schematic is in no placement, the plan takes it. |

Public Syncmatica API the add-on calls: `Syncmatica.getContext(SERVER_CONTEXT)`;
`SyncmaticManager.addServerPlacementConsumer`, `getPlacement`, `getAll`; `ServerPlacement.getId`, `getOwner` (its
`uuid`), `getDimension`; `ServerCommunicationManager.getGameProfile`; `IFileStorage.getLocalLitematic`,
`getLocalState`; `LitematicManager.getActiveContext`, `getRenderingSyncmatic`, `isRendered`, `renderSyncmatic`,
`updateRendered`, `getPlayerDimension`; `CommunicationManager.download`, `getDownloadState`, `startExchange`;
`ClientCommunicationManager.getServer`; `ShareLitematicExchange`'s public constructor.

Fabric API: `PayloadTypeRegistry` (`registerLarge` for snapshots), `ServerPlayNetworking`, `ClientPlayNetworking`
(`canSend`), `ServerPlayConnectionEvents`, `ClientPlayConnectionEvents`, `ClientTickEvents`, `KeyMappingHelper`. Both sides' payload handlers run on the main thread
(server thread, render thread), which the ordering below relies on. Fabric splits a payload registered with
`registerLarge` into packets of at most 1 MiB to players (32,767 bytes to the server) and joins them on arrival, so a
snapshot of any plan within the cap reaches the client whole.

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
  shape, erase or Litematica and `changes` is a list of region, plan position and block state, `undo` and `redo`.
- Server to client: `hello(protocolVersion, limits)` at join; `snapshot(plan, rev, regions)` with each region's
  minimum corner, size, palette, and the packed blocks of each 16-block cube that holds a block other than the
  palette's first entry, which fills the rest of the box; `applied(plan, rev, author, clientSeq, growth, changes)`;
  `rejected(plan, clientSeq, reason)`; `notice(key, args)` for messages players see.

Block states travel as ids in the block-state registry (both sides run the same vanilla blocks); files store them by
name and properties.

Order and conflicts:

1. The server applies edits one at a time on its main thread, in the order they arrive. Each applied edit raises the
   plan's revision by one and goes to every subscriber, its author included.
2. Each block takes the value of the last edit, in the server's order, that wrote it. There are no locks: several
   players can edit one plan, even the same blocks, and everyone ends with the same plan.
3. A client applies `applied` messages in the order they arrive (one connection keeps them in order). A gap in
   revisions makes it ask for a fresh snapshot by sending `subscribe` again. A `rejected` with no `clientSeq` answers
   a `subscribe` for a plan the server doesn't have, and the client drops that plan.
4. Litematica edits show at once on the author's screen, since Litematica writes them before the add-on sends them;
   shapes show when the server's `applied` comes back. When someone else's edit to the same block lands between an
   author's write and its echo, the author sees that block flip once, then settle on the server's order.
5. The server checks each edit: the plan exists, the player has it loaded, the batch is within the block cap, every
   region it names exists, a Litematica edit stays inside its region's box, and growth stays within the size cap. A
   failed check changes nothing and returns `rejected`, and the client takes a fresh snapshot to drop its own early
   copy. An erase skips positions outside its region's box, which are empty already, so it never grows the plan.

Starting caps, in the server's config file `config/buildingplanner.properties` (`max-edit-blocks`, `max-plan-side`):
50,000 blocks an edit, 512 blocks a side for a plan. The size cap applies to the plan's enclosing box, on each side an
edit grows; a plan shared already larger can still grow along its other sides. The server keeps a region's blocks in
16-block cubes at two bytes a block, only where the region holds something other than air, so its memory follows the
blocks a plan holds, not its box: a region at the cap on every side with one full floor (262,144 blocks) takes about
8.5 MB, and the same region filled throughout about 270 MB. A snapshot carries only those cubes, at two bits or more a
block: about 1 MiB for the full floor, and at least 32 MiB for the region filled throughout, built for each player who
loads the plan.

### Players joining mid-edit

The client side starts when the server's `hello` arrives with the same protocol version, and ends when the player
leaves. Each client tick it compares the placements Syncmatica draws (Syncmatica's render after a download, the
player's own share, or a placement Litematica restored) with the plans it has: it sends `subscribe` for each new one
and `unsubscribe` for each one removed from that player's Litematica list. The server answers a `subscribe` with a
`snapshot` at the current revision, then every later `applied`, on the same connection, so nothing falls between them.
The client builds each region's container from the snapshot, swaps it into the schematic (L3), takes the snapshot's
box if the plan has grown since its file was shared, and redraws.

Each tick the client also loads, once while the player is connected, every plan in the player's dimension that isn't
drawn yet (Syncmatica only draws a plan in its own dimension): from Syncmatica's copy of the file when it has a current
one, or else by starting Syncmatica's download, which draws the plan when it finishes. A plan the player unloads stays
unloaded until they next join.

### The client

Edit Schematic writes to a plan's container (L1) are recorded with the block's plan position and new state, the newest
state at each position, and sent once a client tick as one `edit` per plan. A tick's writes over the server's block cap
(from `hello`) aren't sent; the player gets the server's message for too many blocks and the client takes a fresh
snapshot. Writes to placements that aren't plans stay Litematica's own. Undo and redo (E4) first send the tick's
writes.

An `applied` edit writes its blocks into the plan's containers and marks the chunks they're in for redraw. When the
author is someone else, the changed blocks glow for 3 seconds, fading out: an outline and a translucent fill on each
block, or one translucent box around them for an edit of more than 512 blocks, with the editor's name (from the
player list) above them. Messages from the server show in the player's chat.

A finished shape (E1, E2) goes as one `edit` into the plan whose box holds the shape's first block, or else the block
first clicked; when neither is in a plan, it goes into the plan selected in Litematica's placement list, and with no
plan selected the player gets a message and nothing is sent. Each block goes into the plan region whose box holds it,
or else the region the shape started in (the plan's first region when it started outside the plan), at its plan
position from hook L2 and its state unrotated (`getUntransformedBlockState`). Its state is the one Effortless Building
would place: the block's placement rules with the first click's face and the player's facing, then the shape's mirror
and turn. Entries whose item isn't a block (a bucket, a tool) are left out. A shape over the server's block cap isn't
sent, with the server's message; Litematica writes made earlier in the tick are sent first.

Starting a plan: the key opens a screen asking for the plan's name, unless no area is marked, when the player gets a
message instead. Litematica's Simple selection mode, its default, holds an unmarked area as one box with both corners
at 0,0,0, so the add-on takes that box as no area. Create makes an empty schematic of the marked area (`createEmptySchematic`, with the player as
author and the name as the schematic's name), saves it to `Building Planner/<name>.litematic` in Litematica's
schematics folder (a number is added when the name is taken), places it at the area's origin, and shares it through
Syncmatica as its Share button does.

### Growth

When a shape's block falls outside its region, the server works out the region's new minimum corner and size (the old
box joined with the shape's blocks), copies the blocks into a bigger grid, and sends the growth in the same `applied`
as the shape. A grown region's position becomes its new minimum corner and its size turns positive (a Litematica region
can have a negative size, running from its position toward lower coordinates); block entities, entities and scheduled
ticks move with their blocks. Each client builds the bigger container from its copy of the plan, writes the new
container, size and region position into the schematic's maps (L3), moves the region's block entities and entities by
the same rule, then, with the placement unlocked as Syncmatica does when it updates one, resets the placement's
sub-regions to the schematic's values, lets Syncmatica put back its own sub-region data (`updateRendered`), and marks
the plan for redraw. The placement, its Syncmatica identity, origin and position stay the same, so no one reloads.
Plans never shrink; undo doesn't shrink them either.

### Plan storage on the server

The server keeps each plan in memory and writes it to `<world>/building-planner/plans/<plan id>.litematic` in
Litematica's own format (version 7: palette and packed block states per region, with block states as Litematica's
`Name` and `Properties`, not vanilla 26.3's `id` and `properties`), through a temporary file and a rename, at most every 30 seconds while it has changed, and when the server stops. When a placement is first shared,
the add-on reads its blocks from the file Syncmatica stored (`syncmatics/<hash>.litematic`). At start (on the first
server tick, once Syncmatica has loaded its placements), each Syncmatica placement loads the add-on's file, or
Syncmatica's when the add-on has none. When the add-on's file can't be read, the server logs an error, leaves the file
for repair by hand, and doesn't sync that plan. A deleted plan, as it was at the delete, goes to
`<world>/building-planner/deleted/<plan id>.litematic` and its file in `plans/` is removed; it is an ordinary
`.litematic` that a player can load in Litematica and share again. Revisions start again at each server start;
players reconnect and take snapshots. The add-on's files equal Litematica's as NBT (tests round-trip files Litematica's
own code writes, and Litematica's reader reads the add-on's); the bytes differ in the order of compound keys, which no
reader depends on.

### The delete check

Syncmatica's delete reaches the server as `REMOVE_SYNCMATIC` with the plan's id. Hook S1 reads the id without
consuming it, looks up the placement, and compares its owner (the player who shared it, which Syncmatica records and
saves in `placements.json`) with the sender (`getGameProfile(source)`). If they differ, the hook cancels the delete,
so no one's copy is removed, and sends the sender a `notice`. Syncmatica's move and turn stay open to everyone.
Server operators (`ops.json`) may delete any plan too, so a plan whose creator never returns can still be deleted in
the game, including a placement saved without an owner (older Syncmatica versions), which no one else can delete.

### Undo

The server keeps an undo list for each player, across plans: each step is one edit (a shape, an erase, or one tick of
Litematica writes) with each block's state before and after the whole edit. Undo takes the player's newest step and
sets each of its blocks back to its before-state only if the plan still holds that step's after-state there; blocks
someone has changed since stay as they are, and a `notice` says how many. The undo goes out as an ordinary `applied`
edit with no `clientSeq`; an undo that changes no block sends none, and with no steps left a `notice` says there's
nothing to undo. Steps survive growth and moves (plan coordinates) and the player leaving, are dropped with their plan
when it's deleted, keep the newest 50 per player (Effortless Building's own depth), and last until the server
restarts. The part of a step an undo changed goes onto the player's redo list; a redo applies it forward by the same
rule, and the player's next new edit clears the list.

### The block-mix picker

The mix is a list of up to nine blocks with a weight each, kept on the player's computer while the game runs: it
starts empty and off at each launch, and stays across servers. While it's on, hook E5 gives each shape position its
block, chosen by a fixed hash of the world position over the weights, so the preview and the plan agree; hook E6 makes
Effortless Building start shapes with any held item, or none, and draw the preview from those blocks; and hook L1
swaps each block Litematica's place, fill and replace actions write into a plan for the mix's block at its plan
position, keeping the properties both blocks have (a log's axis, a stair's facing). Without a mix, shapes use the held
block, as in Effortless Building.

The picker key (B unless the player changes it in Controls) opens the picker while the mix is off, and turns the mix
off, with a message, while it's on. The picker lists every block that has an item of its own, in the game's order,
with a search box (by name or id) and a toggle, "Only my inventory", that limits the list to the blocks in the
player's inventory. Clicking a block adds it to the mix with a share of 1; under each block in the mix is its share,
which the player types, and its percentage; clicking a block in the mix takes it out. "Use mix" turns the mix on, with
a message naming the key; "Clear" empties the list; "Cancel" and Escape leave the mix as it was.

### When the server lacks the mod

The client checks `ClientPlayNetworking.canSend` for its payloads after joining. Without them, Building Planner turns
itself off for that server: shapes are still dropped (Effortless Building's packets would be anyway) and a `notice`
says Building Planner isn't on the server; Litematica's edits stay on that player's screen, as plain Litematica does.
A protocol-version mismatch in `hello` does the same. The server sends nothing to a player who can't receive it
(`ServerPlayNetworking.canSend`), so players without the add-on, or without any mods, join and play as before; with
Syncmatica alone they see each plan as it was first shared.

### The add-on's licence

MIT (`mod/LICENSE`). Effortless Building and Litematica are LGPL-3.0 and Syncmatica is CC0; the add-on calls their
code and copies none of it, so it can take its own licence.

### In-game checks

`mod/clienttest/run.sh <checks>` runs the dev server and both dev clients in a Linux container, each client on its own
virtual screen with Mesa's software OpenGL, so nothing shows on the host. The clients carry a driver mod (the
`clienttest` source set, never in the add-on's jar) that runs commands from a file in the game folder: Litematica
actions, waits for a state, and screenshots. Key presses and clicks are real ones, sent with xdotool. `step6.sh`
checks, with Planner1 an operator and Planner2 not:

- Each client starts a session on `hello`.
- Starting a plan: N in a fresh Litematica (Simple selection mode, no area marked) gives the message; with an area
  marked, N, typing a name and Enter create and share the plan, and the other player sees it at once.
- A Litematica fill and a single block reach the other client, glowing with the editor's name, and the glow fades.
- An edit past the plan's box grows it in place on both clients.
- Ctrl+Z and Ctrl+Y undo and redo the player's edit on both clients.
- Syncmatica's Unload hides the plan for that player only, until they next join.
- A player who rejoins gets the edits and growth made while they were away.
- Syncmatica's Remove: refused, with the message, for a player removing someone else's plan; allowed for an operator
  and for the plan's owner.

`step7.sh` draws shapes in Effortless Building's Single mode with its array modifier, clicking the real mouse buttons
with the player looking straight down, and checks:

- A shape goes into the plan it starts in, grows it where it crosses the edge, and reaches the other player at once,
  glowing; no real blocks are placed.
- A shape started on a ghost block goes on top of it, and the break click erases a ghost block, leaving the real ones.
- A shape outside every plan gives the message with no plan selected, and goes into the selected plan otherwise.
- The picker key opens the picker, listing every block; the toggle limits it to the inventory's two blocks; clicking
  both, typing their shares and "Use mix" turns the mix on, with its message.
- With the mix on, each block of a 20-block shape, and a Litematica place, is the mix's block at its position, on
  both clients; the key turns the mix off, with its message.
- A client without Effortless Building (its jar left off the classpath) loads without it or mixin errors, keeps
  Litematica edits flowing both ways, and opens the picker.

The results, the game logs and the screenshots go to a folder outside the repository.

## Unverified

- Syncmatica's later moves on a grown plan, and how quickly growth redraws a large plan.
- Shapes drawn with Effortless Building's multi-click modes (line, wall, floor, cube, slope, circle, cylinder, sphere)
  and its mirror and radial mirror; the checks draw Single-mode shapes with the array, which take the same capture.
- That the shape preview draws the mix's blocks, and how block states turn out for blocks whose placement depends on
  their neighbours (stairs, fences, slabs).
- Whether Litematica's edits aim from Freecam's camera.
