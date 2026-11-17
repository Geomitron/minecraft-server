# Building Planner

Building Planner is a Fabric add-on for Minecraft 26.3. With it, the group plans builds together as one shared layer of
ghost blocks that everyone on the server sees and edits live, then builds them by hand. Players draw with Effortless
Building; Litematica draws the ghost blocks, out of sight. It hooks into unchanged copies of both. Geo's decisions
behind it are in [plan.md](plan.md) ("Decisions").

**What's built.** Players run Building Planner 0.2.1, and the server 0.2.0, which keep ghost blocks in separate named
plans shared through Syncmatica. Built on `main` since then: hold to repeat at fixed speeds (hook E10), the look of
ghost blocks, the layer's storage and files, and the server's side of the layer and protocol 3, which the server runs
beside protocol 2's named plans (payload ids starting `plan_`) and which no client speaks until the player's layer is
built. Everything else this spec describes waits on the build steps in [plan.md](plan.md) ("Agent-ready work"), which
end with the release. The build reuses what still fits: Effortless Building's hooks E1 to E10 (E1, E2 and E6 change),
the rules of undo and redo, the edit glow, the block mix screen's layout, the check for a server without the add-on, and
the in-game check harness.

## What players get

- **Ghost blocks.** Each dimension has one layer of ghost blocks, shared with everyone on the server. Every change
  reaches everyone as it's made, and several players can draw at once, even on the same blocks. There are no plans to
  start, name, move or delete. Ghost blocks show see-through (see "How ghost blocks look").
- **Show or hide.** One switch shows or hides all ghost blocks for that player: a key ("Show or hide ghost blocks", N
  unless the player changes it in Controls) and a button in Effortless Building's radial menu. Building Planner
  remembers the choice across game runs; ghost blocks show at first. Drawing a shape while they're hidden shows them
  again, then draws.
- **Drawing shapes.** With an Effortless Building mode chosen (single, line, wall, floor, cube, slope, circle,
  cylinder, sphere and the rest), the place (right) click draws the shape as ghost blocks, with Effortless Building's
  mirror, radial mirror and array (repeat). A shape drawn with the break (left) click erases ghost blocks, whoever
  placed them. Effortless Building never places or breaks real blocks. A shape can start on a ghost block as well as
  on a real block.
- **Hold to repeat.** In Single mode, holding the place button keeps placing ghost blocks and holding the break button
  keeps erasing them. Two sliders in Effortless Building's radial menu set the placing speed and the erasing speed; at
  first they're the game's own pace for a held button, five a second to place and just over three to erase. The other
  modes act once for each click.
- **Disable.** With "Disable" chosen in Effortless Building's radial menu, clicks act as they do without mods, placing
  and breaking real blocks, even with the block mix on.
- **Block mixes.** The block mix key (B unless the player changes it in Controls) opens the block mix screen, the only
  place to choose a mix's blocks: it lists every block in the game, with search and an "Only my inventory" switch. The
  player picks up to nine blocks, each with a share (say 80 cobblestone to 20 mossy cobblestone), and turns the mix on.
  The screen closes, and the mix then applies to Effortless Building's shapes, in every mode but Disable, while the
  player holds a placeable item (a block); with a tool or any other item in hand, clicks act as they would without the
  mix. While the mix applies, a panel at the edge of the screen shows its blocks and shares and the keys to change it
  or turn it off. A mix of one block lets a survival player plan with a block they don't have.
- **Undo and redo.** Undo takes back only that player's own changes, newest first: a whole shape or paste at a time,
  in any dimension. A block someone else has changed since stays as they left it. Effortless Building's Ctrl+Z (and
  the undo button in its radial menu) undoes, and its Ctrl+Y (and the redo button) redoes the newest undone change,
  until the player's next new change.
- **Others' edits.** When someone else changes ghost blocks, the changed blocks glow for a few seconds, with the
  editor's name beside them.
- **Copy and paste.** The player presses Copy (J, or the Copy button in the radial menu) and draws a box with
  Effortless Building's "Cube" shape: the ghost blocks inside become a copy, which only that player sees. "Copy real
  blocks" (its radial menu button) does the same with the real blocks inside the box. The copy follows where the
  player aims; Turn (U) and Mirror (H) turn and flip it, and Paste (K) writes it into the layer as one change, which
  one undo takes back. The empty space in the copy leaves the ghost blocks under it as they are. The copy stays for
  more pastes until the player clears it (Delete). Each key also has a radial menu button.
- **Material list.** The player presses the Material list button in the radial menu and draws a box with the "Cube"
  shape: Litematica's material list opens for the ghost blocks inside, with how many of each block are still missing
  in the world.
- **How ghost blocks look.** On each player's computer, once, Building Planner turns on Litematica's "render blocks as
  translucent" and sets its ghost block alpha to 0.8, so ghost blocks draw see-through. It sets each only if the player
  hasn't changed it from Litematica's default (off, and 0.5), and never again, so a player's own later change stays.
  This happens whether or not the player joins a server with Building Planner.
- **Messages.** Building Planner's messages show in the player's chat. Every text players see (messages, key names,
  button and slider labels, the panel) goes to Geo for review once it's built; the wording in this spec is drafted for
  that review.

## Geo's goals, and what meets each

| Goal (402 r1, r2) | Met by | Gap |
|---|---|---|
| Measure layouts | Effortless Building shows a shape's size while it's drawn; the "Cube" shape's box measures an area before anything is placed. | Measures are each player's own until drawn as ghost blocks. MiniHUD-style personal guides were rejected (402 r2, c2). |
| Plan room sizes and shapes before committing | Effortless Building's shapes drawn as ghost blocks, erased with the break click. | None. |
| See and judge a build (free or fixed camera) | Litematica draws the ghost blocks; Freecam moves the camera away from the player; the switch hides them to compare with the real world. | Effortless Building aims from the player's eyes, not the camera, so shapes can't be drawn from a Freecam view. |
| Lay out walls, shapes and components quickly | Effortless Building's eleven build modes; hold to repeat in Single mode at the player's chosen speed. | None. |
| Copy and paste | Copy and "Copy real blocks" take a box drawn with the "Cube" shape; Paste writes the copy into the layer. Effortless Building's array repeats while drawing. | None. |
| Symmetry | Effortless Building's mirror and radial mirror. | None. |
| Randomized block mixes | Building Planner's block mix (Effortless Building's Randomizer needs the server copy, which Geo kept off the server). | None. |
| A plan everyone on the server sees, built for real by hand | The shared layer reaches everyone live; Litematica's ghost blocks, its missing-block colours and the material list for a marked area guide the build. | None. |

## Technical design

### Parts

One jar, mod id `buildingplanner`, installed on the server and in the player pack. It registers no blocks, items or
menus, so it adds nothing to the registries Fabric API syncs at login, and players without mods can still join.

- **Common:** the layer's column storage and its file format, the network payloads, the hold-to-repeat timer and the
  look defaults' rules.
- **Server:** the layer store and its files, column watching, edits and their order, per-player undo, and the one-time
  import of the plans Syncmatica shared. They run only on a dedicated server, which is how the group plays.
- **Client:** the player's copy of the layer and its column watching, the tiles that hand it to Litematica and the
  hooks that hide them, the show or hide switch, the Effortless Building capture, hold to repeat, the radial menu's
  buttons and sliders, the block mix and its panel, copy and paste, the material list, the edit glow, the look of
  ghost blocks, and the check for a server without the add-on.

`fabric.mod.json` depends on Fabric API, which both sides run. Litematica, MaLiLib and Effortless Building run only on
players' computers, so the add-on lists them under `suggests` (Fabric Loader warns at start about a missing
`recommends`, not a missing `suggests`) and puts every mixin into those mods in the `client` section of its mixin
config. Its mixin config plugin skips the Effortless Building mixins when that mod is missing, and the Litematica
mixins when Litematica or MaLiLib is; without Litematica and MaLiLib the client side stays off, as on a server without
the add-on. Injectors use MixinExtras, which Fabric Loader 0.20.1 bundles (0.5.5). The dev clients run Effortless
Building 4.3 for Fabric 26.3 (Modrinth version `R8eAP63A`, set in `mod/gradle.properties`), the release built from the
source the hooks were checked in; the dev server runs neither Effortless Building nor Syncmatica.

### Hooks

Checked in the source at: Effortless Building `Requios/effortless-building-multi` branch `26.3` @ `f31a5b5` (release
4.3 for Fabric 26.3); Litematica `sakura-ryoko/litematica` branch `26.3` @ `eb15b39`, where every file below matches
the pack's release 0.29.1 (`b1a2682`) except one line in `SchematicPlacementManager.java`, a line in `Configs.java`
that no option here depends on, and entity counting added to `TaskCountBlocksPlacement.java` (its block counting is
the same); Syncmatica `sakura-ryoko/syncmatica` branch `26.3` @ `37a969e` (0.3.20), read only
for the import's file format; Fabric API branch `26.3` @ `b8eae12`; Fabric Loader @ `cb9523b` (0.20.1).

Effortless Building, under `common/src/main/java/nl/requios/effortlessbuilding/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| E1 | `buildpipeline/BuildPipelineClient.java`, `handleClick` | `@WrapWithCondition` on `PacketHandler.sendToServer(PlaceBuildModePacket)`, reading the local `BlockSet blocks` | Take the finished shape (every entry, after mode, modifiers, mix and constraints) into the layer; don't send the packet. `getFirstClickHit()` is still set here, so block states use the first click's face. While a copy, a "Copy real blocks" or a material list waits for its box, take the box from `blocks.firstPos` and `blocks.lastPos` (a cube's opposite corners, which modifiers don't move) instead, and place nothing. |
| E2 | same method | `@WrapWithCondition` on `PacketHandler.sendToServer(BreakBuildModePacket)` | Erase the shape's positions from the layer; don't send. |
| E3 | same method | `@WrapWithCondition` on `PlacedBlockTracker.clientTrackAll` | Don't record ghost positions as real blocks the player placed. |
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo and redo to the layer. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While the mix applies, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipeline.java` `isBuildTriggerItem`; `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While the mix applies, treat the held block as Effortless Building's Randomizer, so the preview draws each entry's own block. In `shouldInterceptPlacing` this holds only while the mode isn't Disable (`BuildModes.CLIENT.getBuildMode() != BuildModeEnum.DISABLED`), so clicks in Disable stay vanilla's. While a box is awaited for copy or the material list, any held item, or none, starts the cube. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not ghost blocks. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapOperation` on both `ItemUsageTracker.compute` calls, clearing the tracker instead | No red "missing items" tint: ghost blocks don't use up items. |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `resolveFirstClickPos` | `@WrapOperation` on `ClientLevel.clip`; on `Level.getBlockState` | Let shapes start on ghost blocks: aim at the nearer of the real block and Litematica's ghost block (`RayTraceUtils.traceToSchematicWorld`), and read the ghost block's state, where the real block can be replaced, when picking the start position, so the shape goes beside it. |
| E10 | `buildpipeline/BuildPipelineClient.java`, public static `handleRightClick` and `handleLeftClick`, with `shouldInterceptPlacing`, `shouldInterceptBreaking` and `getBuildState`; `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem`; `buildmode/BuildModes.java`, `getBuildMode`; the click checks copy `fabric/.../EffortlessBuildingClient.java`'s client tick | No mixin: Building Planner's own `END_CLIENT_TICK` calls the two public methods again while a button stays down | Hold to repeat in Single mode (see "The client"). Effortless Building's own tick acts only when a button goes down (`rightJustPressed`, `leftJustPressed`). |
| E11 | `screen/RadialMenu.java`, `init()` | `@Inject` at tail, calling `Screen.addRenderableWidget` | Add Building Planner's buttons and sliders to the radial menu each time it opens (Minecraft clears a screen's widgets and calls `init` on each open). They're children of the screen, so `RadialMenu.mouseClicked`, which calls `super.mouseClicked` after its own `performAction`, routes clicks and drags to them. |
| E12 | `screen/RadialMenu.java`, `extractRenderState(GuiGraphicsExtractor, int, int, float)` | `@Inject` at tail | Draw those widgets: the method draws the menu itself and never calls `Screen`'s own drawing, which would draw them. |

Effortless Building's limits come from its config on the player's computer (`config/ServerConfig.java`; survival
defaults: reach 32, 2,000 blocks a shape, 64 a side). The server applies its own caps (below). The radial menu's own
buttons come from a fixed list (`RadialMenu.extractRenderState`, `buildmode/ModeOptions.java` `ActionEnum`), which is
why Building Planner adds widgets of its own rather than actions; they sit where the menu draws nothing, so its
`switchTo` and `doAction` stay empty when the player clicks one.

Litematica, under `src/main/java/fi/dy/masa/litematica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| L1 | `util/SchematicUtils.java`: `setTargetedSchematicBlockState(BlockPos, BlockState)`, `setSchematicBlockStates`, `setAllStatesToAirExcept(SchematicPlacementManager, PlacementPart, BlockState, Level)`, `replaceAllIdenticalBlocks` | `@WrapOperation` on each `LitematicaBlockStateContainer.set(int, int, int, BlockState)` (the only four writes the ten Edit Schematic actions make, all called from `event/InputHandler.java`) | Skip a write into a tile's container, so Litematica's Edit Schematic actions never change the layer, even on a computer whose Litematica tool mode is still "Edit Schematic" (`InputHandler.handleUseKey` and `handleAttackKey` edit in that mode whatever the player holds). Writes to other placements, such as the copy, stay Litematica's own. |
| L4 | `schematic/placement/SchematicPlacementManager.java`, `setSelectedSchematicPlacement(SchematicPlacement)` | `@ModifyVariable` at head, on the argument | A tile is never selected: selecting one selects nothing instead. Covers `addSchematicPlacement`, which selects a new placement when none is selected, `removeSchematicPlacement`, which then selects the first placement, selection by aim, and the placement list. |
| L5 | `gui/widgets/WidgetListSchematicPlacements.java`, `getAllEntries()` | `@ModifyReturnValue` | Leave tiles out of Litematica's placement list. |
| L6 | `render/OverlayRenderer.java`, `updatePlacementCache()` | `@WrapOperation` on `SchematicPlacementManager.getAllSchematicsPlacements()` | Leave tiles out of the boxes and origin markers `renderBoxes` draws. Litematica's own switch for those boxes (`enablePlacementBoxesRendering`) would hide the copy's box too. |
| L7 | `render/schematic/ChunkRendererSchematicVbo.java`, `getOverlayType(BlockState, BlockState)` | `@ModifyReturnValue` | While Building Planner has a session, turn `EXTRA` into `NONE`. Litematica marks a real block as extra wherever a placement's box holds air there (`schematicOverlayTypeExtra`, on by default), and a tile's box is a whole chunk column's sections, so every real block in it would be marked. The layer holds only blocks to place, so no real block is extra. Missing and wrong blocks keep their colours. |

Public Litematica API the add-on calls: `LitematicaSchematic.createEmptySchematic`, `getSubRegionContainer`,
`getAreaSize`; `LitematicaBlockStateContainer`'s `get` and `set`; `SchematicPlacement.createFor`, `createTemporary`,
`setShouldBeSaved`, `getSchematic`, `getOrigin`, `setOrigin`, `getRotation`, `setRotation`, `getMirror`, `setMirror`;
`SchematicPlacementManager.getAllSchematicsPlacements`, `addSchematicPlacement`, `removeSchematicPlacement`,
`setSelectedSchematicPlacement`, `markChunkForRebuild`; `AreaSelection`'s constructor and `addSubRegionBox`; `Box`'s
constructor; `PositionUtils.getTransformedBlockPos`; `RayTraceUtils.traceToSchematicWorld`;
`SchematicWorldHandler.getSchematicWorld`; `MaterialListPlacement`'s constructor, `DataManager.setMaterialList`,
`GuiMaterialList`'s constructor and MaLiLib's `GuiBase.openGui`; `Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT`,
`GHOST_BLOCK_ALPHA`, `Configs.Generic.TOOL_ITEM_ENABLED` and `Configs.saveToFile`. From MaLiLib:
`RenderEventHandler.registerWorldLastRenderer` and `RenderUtils`' batched box and text-plate drawing.

Fabric API: `PayloadTypeRegistry` (`registerLarge` for column snapshots), `ServerPlayNetworking`,
`ClientPlayNetworking` (`canSend`), `ServerPlayConnectionEvents`, `ClientPlayConnectionEvents`, `ClientTickEvents`,
`ClientChunkEvents` (`CHUNK_LOAD`, `CHUNK_UNLOAD`), `ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE`,
`ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL`, `HudElementRegistry` (the mix panel) and
`KeyMappingHelper`. Both sides' payload handlers run on the main thread (server thread, render thread), which the
ordering below relies on. Fabric splits a payload registered with `registerLarge` into packets of at most 1 MiB to
players and joins them on arrival.

The build steps remove what the shared layer replaces from today's code: hooks S1 to S3 into Syncmatica (the delete
check, each plan's own schematic, and Syncmatica's packets on the main thread) with `remove-loaded.sh`, L2 (plan
coordinates without clamping) and L3 (growing a plan's region in place), the L1 capture of Edit Schematic writes, the
"Start a plan" screen and key, and Syncmatica in `fabric.mod.json`, the build and the dev server.

### Tiles: how Litematica draws the layer

Litematica draws ghost blocks only through placements, so the player's game gives Litematica one placement, a "tile",
for each chunk column (16 by 16 blocks) that holds ghost blocks. A tile is only as tall as that column's 16-block
sections with ghost blocks, from the lowest to the highest. Its schematic is made with `createEmptySchematic` over that
box and filled from the player's copy of the layer, with structure void wherever the layer holds no block: Litematica
skips structure void when it draws (`WorldPlacingUtils.placeBlocksToProtoChunk`), so a tile's empty space never hides
the copy's blocks. Tiles are made with `SchematicPlacement.createFor`, marked not to be saved (`setShouldBeSaved`, and
they have no file), and added with `addSchematicPlacement` without messages.

Litematica keeps each chunk's placements in lists (`schematicsTouchingChunk`) and rebuilds what it draws one chunk
column at a time (`PlacementManagerTaskRebuild.buildTask`), so tiles never overlap and an edit rebuilds only its own
columns. An edit writes the changed blocks into the tile's container and calls `markChunkForRebuild` for the column.
An edit that adds a block in a section above or below the tile, or empties the tile's top or bottom section, replaces
the tile with one of the new height; a column left with no ghost blocks loses its tile.

Where two placements touch one chunk section, Litematica draws them in the order they were added, and with its
default "replace behavior" (`placementReplaceBehavior` = ALL) a later one's blocks cover an earlier one's. To keep the
copy drawn over the tiles, Building Planner removes the copy and adds it again after every tile it adds while a copy
exists.

Hooks L4 to L7 keep tiles out of Litematica's screens: never selected, not in the placement list, no boxes, and no
"extra block" marks. Litematica saves its other placements as before; tiles disappear when the player leaves the
server, and Litematica clears every placement when the player changes dimension (`DataManager.loadPerDimensionData`),
after which the tiles come back as the new dimension's columns arrive.

### The layer and its column watching

The layer is a map, for each dimension, from chunk column to the 16-block sections of that column that hold ghost
blocks. A section keeps its blocks as two-byte indexes into a palette of its own, which drops a block state when its
last block goes, and the section is freed when it holds no ghost block again, as is a column with no section left, so
memory follows the ghost blocks, not the area they span: 8 KB for each section with a ghost block. The server holds
every dimension's whole layer in memory; each player's game holds only the columns it watches. Block states travel as
ids in the block-state registry (both sides run the same vanilla blocks); files store them by name and properties.

A player's game watches the columns whose chunks it has loaded, since Litematica draws only in loaded chunks
(`SchematicPlacementManager.canHandleChunk`). Fabric API's `ClientChunkEvents.CHUNK_LOAD` and `CHUNK_UNLOAD` say which;
once a client tick, the game sends the columns that loaded as one `watch` and those that unloaded as one `unwatch`. A
change of dimension (`ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE`) drops all the old dimension's columns, and the
server drops that player's watches on its side (`ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL`). Freecam
loads no chunks, so it changes nothing.

The server answers each `watch` with one `columns` message that lists every column asked for, each with its sections,
or none for an empty column, and from then sends that player every edit that touches a watched column. A column
farther from the player than the server's view distance plus two chunks, and a watch of another dimension than the
player's, is ignored. Until a column's snapshot arrives, the
game ignores edits to it; a snapshot replaces whatever the game held for that column. Messages on one connection keep
their order, so nothing falls between a snapshot and the edits after it.

### Payloads and order

Payloads (namespace `buildingplanner`, protocol version 3):

- Client to server: `watch(dimension, columns)`, `unwatch(dimension, columns)`, `edit(dimension, clientSeq, kind,
  changes)` where `kind` is shape or erase and `changes` is a list of world position and block state (air for an
  erase), `undo` and `redo`.
- Server to client: `hello(protocolVersion, limits)` at join, where `limits` is the block cap for an edit;
  `columns(dimension, columns)`, each column its position and its sections, each section its height, palette and
  packed blocks; `applied(dimension, author, clientSeq, changes)`; `rejected(clientSeq, reason)`; `notice(message)`
  for messages players see.

Order and conflicts:

1. The server applies edits one at a time on its main thread, in the order they arrive, and sends each applied edit to
   every player who watches a column it touches. The author gets every edit back, as `applied` with only the changes
   that changed a block (perhaps none) or as `rejected`.
2. Each block takes the value of the last edit, in the server's order, that wrote it. There are no locks and no
   owners: anyone may erase anyone's ghost blocks, and everyone ends with the same layer.
3. A game applies `applied` messages in the order they arrive, to the columns it holds, and ignores the rest. A shape
   shows when the server's `applied` comes back.
4. The server checks each edit: the dimension is the player's own, every position is in a column that player
   watches, within the world's build height and inside the world border, and the edit is within the block cap. A
   failed check changes nothing and returns `rejected` with a message.

The block cap is the server's config file `config/buildingplanner.properties` (`max-edit-blocks`): 50,000 blocks an
edit.

### The layer's files on the server

The server writes the layer to `<world>/building-planner/layer/<dimension namespace>/<dimension path>/<x>.<z>.dat`, one
file for each 32 by 32 chunk columns (512 by 512 blocks), where `<x>` and `<z>` are the area's coordinates (chunk
coordinates divided by 32, rounded down). Each file is gzip-compressed NBT:

- `version`: the format's version, 1.
- `data_version`: the game's data version when the file was written; block states from an older one go through the
  game's data fixer when read.
- `columns`: each column with ghost blocks, with `x` and `z` (its chunk coordinates) and `sections`, each section with
  ghost blocks, lowest first, with `y` (its block y divided by 16, rounded down), `palette` (air, then each block state
  the section holds, by `Name` and `Properties` as Litematica's palettes store them) and `blocks` (each of the 4,096
  positions' index into the palette, y slowest, then z, then x, packed as Litematica packs a region: as many bits as
  the palette's size needs, at least 2, running on across longs, the first in the lowest bits).

A file whose area holds no ghost blocks is deleted. The server builds a changed area's contents on the server thread and
one background thread writes them, each through a temporary file and a rename, at most every 30 seconds while the layer
has changed, and when the server stops. At start the server reads every file; when it can't list the layer's folder, it
stops rather than run without the layer. When a file can't be read, the server logs
an error, adds none of its ghost blocks, leaves the file for repair by hand, and never writes that area while it runs,
so no edit overwrites it. A file can't be read when it's cut short or isn't compressed NBT, has another version or a
missing entry, lists a column outside its area or twice, or a section twice in one column, or has a section with a block
past its palette or a block array of the wrong length. A block the game doesn't know reads as no ghost block, as in
Litematica. The server skips a `.dat` file whose name or folder isn't an area's, with a warning, and ignores the
temporary files a crash leaves.

### The import of today's plans

The first time the server starts with this version, before `<world>/building-planner/layer/` exists, it builds the
layer from every plan Syncmatica shared, hidden ones included. It doesn't need Syncmatica to run: it reads Syncmatica's
list of placements, `<world>/syncmatica/placements.json` (each entry's `id`, `origin` with its `position` and
`dimension`, `rotation`, `mirror` and optional `subregionData`; `data/ServerPlacement.java` `toJson`,
`data/ServerPosition.java`, `extended_core/SubRegionPlacementModification.java`), and each plan's blocks from
Building Planner's own file `<world>/building-planner/plans/<id>.litematic`, or, when there is none, from Syncmatica's
stored file `syncmatics/<hash>.litematic` in the server's folder. Each non-air block goes to the world position where
Litematica drew it: its position from the region's corner, mirrored and turned by the plan's placement and then by the
region's own (from `subregionData` when a player moved or turned the region), plus the region's position (also from
`subregionData` when moved) mirrored and turned by the plan's, plus the plan's origin, with its state mirrored and
turned the same way (`LitematicaSchematic.placeBlocksToWorld`, `PositionUtils.getTransformedPlacementPosition`).
Structure void is skipped, like air.

Where plans overlap, the later plan's blocks win: plans go in the order of their metadata's `TimeCreated`, then their
id, and a later plan's air leaves an earlier plan's blocks. A plan whose file can't be read is logged and skipped. The
import writes the layer's files, then logs how many plans and blocks it moved. It leaves `plans/`, `deleted/`,
`syncmatica/` and `syncmatics/` where they are, unread from then on.

### Undo

The server keeps an undo list for each player: each step is one edit (a shape, an erase or a paste), with its
dimension and each block's state before and after the edit. Undo takes the player's newest step and sets each of its
blocks back to its before-state only if the layer still holds that step's after-state there; blocks someone has
changed since stay as they are, and a `notice` says how many. The undo goes out as an ordinary `applied` edit with no
`clientSeq`; an undo that changes no block sends none, and with no steps left a `notice` says there's nothing to undo.
Steps survive the player leaving or changing dimension, keep the newest 50 per player (Effortless Building's own
depth), and last until the server restarts. The part of a step an undo changed goes onto the player's redo list; a
redo applies it forward by the same rule, and the player's next new edit clears the list.

### The client

The client side starts when the server's `hello` arrives with the same protocol version, and ends when the player
leaves. At its start it removes from Litematica every placement whose schematic file lies in the game folder's
`schematics/sync/` (where Syncmatica's client kept shared plans) or `schematics/Building Planner/` (where "Start a
plan" saved them): Litematica saved those placements and restores them on its own, and they would show old plans over
the layer. The files stay.

A finished shape (E1, E2) goes as one `edit` of world positions. Each block's state is the one Effortless Building
would place: the block's placement rules with the first click's face and the player's facing, then the shape's mirror
and turn. Entries whose item isn't a block (a bucket, a tool) are left out. A shape over the server's block cap (from
`hello`) isn't sent; the player gets the server's message.

An `applied` edit writes its blocks into the player's copy of the layer and into the tiles, as "Tiles" describes. When
the author is someone else and ghost blocks are showing, the changed blocks glow for 3 seconds, fading out: an outline
and a translucent fill on each block, or one translucent box around them for an edit of more than 512 blocks, with the
editor's name (from the player list) above them, drawn through MaLiLib's renderer. Messages from the server show in
the player's chat.

Hold to repeat (E10): while Building Planner has a session, Effortless Building is loaded and its mode is Single, the
client tick counts how many ticks each of the use and attack buttons has been down, with no screen open. After the
tick a button goes down, which Effortless Building's own tick handles, it calls `handleRightClick` (use) or
`handleLeftClick` (attack) again every N ticks, where N is the placing or erasing slider's setting, under the
conditions Effortless Building sets for a fresh click (a build trigger item, or a sequence under way; for attack also
`shouldInterceptBreaking`). Each repeat is an ordinary click, so it sends an edit as any shape does. The other modes,
Disable, and a server without the add-on have no repeat. Effortless Building's mixins already cancel vanilla's own
repeat while it intercepts.

The two sliders' first settings are vanilla 26.3's pace for a held button, read in the game's classes:
`Minecraft.startUseItem` sets `rightClickDelay` to 4, which `Minecraft.tick` counts down once a tick and a held use
button acts again on reaching 0, so use repeats every 4 ticks; `MultiPlayerGameMode.startDestroyBlock` and
`continueDestroyBlock` set `destroyDelay` to 5 after a creative destroy, and `continueDestroyBlock` counts it down on
the next 5 ticks and destroys on the one after, so attack repeats every 6 ticks.

How ghost blocks look: on the first client tick of a game run, after Litematica has loaded its config, Building Planner
reads `config/buildingplanner-client.properties` on the player's computer. When it doesn't hold
`look-defaults-applied=true`, Building Planner sets Litematica's `Visuals` options `renderBlocksAsTranslucent` to on and
`ghostBlockAlpha` to 0.8, and its `Generic` option `toolItemEnabled` to off, each only while it still equals
Litematica's default (off, 0.5, and on; `Configs.java`), then calls `Configs.saveToFile()` and writes the line. With
the tool item off, Litematica's tool (a stick by default) does nothing, since the stick and Litematica's tool modes
play no part in planning. When the file holds the line, it does nothing, so a player's own later change, even back to
a default, stays. It doesn't need a session, so it happens before the player joins any server. If setting or saving
fails, it logs a warning, writes no line and tries again at the next start. An option the player already set to the
value Building Planner would set looks the same as one it set.

### Show or hide

The switch is the "Show or hide ghost blocks" key (N) and a radial menu button. Hiding removes every tile
from Litematica; showing adds them back. While hidden, the game keeps watching columns and applying edits to its copy of
the layer, so showing needs no reload, and the edit glow draws nothing. The state is kept in
`config/buildingplanner-client.properties` (`ghost-blocks-shown`) and starts as shown. When a shape starts (Effortless
Building's `getBuildState()` turns non-null, read once a client tick) or finishes (E1, E2), a paste is made, or a
material list is asked for while ghost blocks are hidden, they show again first, with a message the first time in each
session. Building Planner doesn't use Litematica's own rendering switch (`enableRendering`): while that's off, each
added tile would bring up Litematica's status panel for 10 seconds (`SchematicPlacementManager.onPlacementAdded`,
`StatusInfoRenderer.startOverrideDelay`), and it hides the copy too. Litematica's own switch still turns all its drawing
off.

### The radial menu's buttons and sliders

Hooks E11 and E12 add Building Planner's widgets to Effortless Building's radial menu (opened by holding its key, left
Alt by default; it closes when the key is let go): a row of buttons under the menu's left-hand buttons for "Show or
hide ghost blocks", "Copy", "Copy real blocks", "Paste" and "Material list"; a second row with "Turn", "Mirror" and
"Clear" while a copy exists; and under them two sliders, "Placing speed" and "Erasing speed", each showing its speed in
blocks a second. A slider's steps are the tick gaps 1 to 20 (20 to 1 a second); its setting is kept in
`config/buildingplanner-client.properties` (`place-repeat-ticks`, `erase-repeat-ticks`; at first 4 and 6). Each button
shows its name, and its key when it has one, on hover, as the menu's own buttons do. Their icons come from Lucide
(ISC licence), copied as path data into a source file that names the set, its version, its source and its licence, and
made into the mod's button textures from that data. The layout and labels are drafted for Geo's review, with
screenshots, before the step that builds them is pushed.

### The block mix and its panel

The mix is a list of up to nine blocks with a weight each, kept on the player's computer while the game runs: it
starts empty and off at each launch, and stays across servers. It applies while it's on, Building Planner has a
session, the main hand holds a `BlockItem`, and Effortless Building's mode isn't Disable. While it applies, hook E5
gives each shape position its block, chosen by a fixed hash of the world position over the weights, so the preview and
the layer agree, and hook E6 makes the preview draw those blocks. When it doesn't apply, shapes use the held block, as
in Effortless Building, and in Disable every click is vanilla's.

The mix key (B unless the player changes it in Controls) opens the block mix screen, the only way to choose the mix's
blocks. It lists every block that has an item of its own, in the game's order, with a search box (by name or id) and
an "Only my inventory" switch that limits the list to the blocks in the player's inventory. Clicking a block adds it
to the mix with a share of 1; under each block in the mix is its share, which the player types, and its percentage;
clicking a block in the mix takes it out. "Use mix" turns the mix on and closes the screen, with a message naming the
key; "Clear" empties the list; "Cancel" and Escape leave the mix as it was. Opened while the mix is on, the screen
shows the mix as it is, to change it, and has a "Turn off" button, which turns the mix off with a message.

While the mix applies, a panel at the right edge of the screen, drawn through `HudElementRegistry`, shows each of its
blocks (the block's icon and name) with its percentage, and under them the line "B: change or turn off", with the key
as the player has bound it. It hides while the mix doesn't apply, and while a screen is open or the game's HUD is
hidden (F1). Its look and text are drafted for Geo's review, with screenshots.

### Copy and paste

The copy is a Litematica placement of a schematic kept in memory on the player's computer. Only that player sees it,
and Litematica doesn't save it, since it has no file; hook L4 keeps it from being selected, and L5 leaves it out of the
placement list, as for tiles. Its keys and buttons, each key changeable in Controls under Building Planner: Copy (J),
"Copy real blocks" (no key unless the player sets one), Paste (K), Turn (U), Mirror (H) and Clear (Delete). None of the
keys is a default key, or part of a default key combination, of Minecraft 26.3, Litematica, MaLiLib, Effortless
Building 4.3 or Freecam 1.5.1.

Copy and "Copy real blocks" wait for a box: they switch Effortless Building to its "Cube" mode, remembering the mode
the player had, with a message saying to draw a box around what to copy. The cube's preview shows as usual, and its
last click makes the copy (hook E1), places nothing, and puts back the player's mode. Pressing either again, choosing
another mode, or Clear stops the wait. Copy takes the ghost blocks in the box from the player's copy of the layer;
"Copy real blocks" takes the real blocks in the box from the player's world (`ClientLevel.getBlockState`), every
non-air block state, without block entity contents. Each block keeps its world position relative to the box. The
copy's box is the smallest box around the copied blocks, and its schematic holds structure void in every position
with no copied block, so the ghost blocks under its empty space show. When the box has no blocks to copy, the player
gets a message and nothing changes. A copy of more blocks than the server's block cap is refused with a message,
since its paste would be. Otherwise the new copy replaces any earlier one, and the player gets a message naming the
Paste key. Effortless Building's own limit on a shape's sides (64 in survival by default) limits the box.

The copy follows where the player aims: each client tick its origin, its corner with the lowest
coordinates, goes on the block beside the face the player aims at, real or ghost (as hook E9 aims), within 64 blocks;
when they aim at nothing, it stays where it was. Turn gives the copy a quarter turn clockwise, seen from above, around
its origin. Mirror flips it left to right as the player faces it, around its origin: across the north-south line when
they face north or south, and across the east-west line when they face east or west. Both set the placement's rotation
and mirror, which Litematica applies mirror first, then rotation; Mirror combines its flip with the turn already set.

Paste sends each block of the copy other than air and structure void as one shape edit, at the world position where
Litematica draws it, with its state mirrored and turned as the copy is, so the server's order, the glow for other
players, undo, redo and the block cap work as they do for shapes. The copy's empty space leaves the layer's blocks as
they are. The copy stays after a paste, for more pastes. Paste with no copy gives a message; Turn, Mirror and Clear
with no copy do nothing. Clear removes the copy; leaving the server or changing dimension ends it too.

### The material list

The Material list button waits for a box as Copy does, with its own message. When the cube finishes, Building Planner
makes an empty schematic of the box (`createEmptySchematic` over one `Box`), a temporary placement of it at the box's
corner (`SchematicPlacement.createTemporary`), which it never adds to Litematica, and a `MaterialListPlacement` for it,
which it sets as Litematica's material list (`DataManager.setMaterialList`) and opens (`GuiMaterialList`). Litematica
counts the ghost blocks it draws inside the placement's box against the real world
(`TaskCountBlocksPlacement.countAtPosition`): the total of each block, how many are missing, and how many are the wrong
block. It counts only chunks loaded on the player's computer, which are the columns the player watches. Asking for a
material list while ghost blocks are hidden shows them first, since Litematica counts what it draws.

### When the server lacks the mod

On joining, the client checks with `ClientPlayNetworking.canSend` that the server takes all five of its payloads. When
it doesn't, Building Planner stays off for that server, silently: the player sees no message, and its keys, its radial
menu buttons and its sliders do nothing, and the mix panel doesn't show. While it's off, Effortless Building's shapes
place nothing: its place and break packets (hooks E1 and E2) and its undo and redo packets (E4) aren't sent, so a
server that runs Effortless Building without Building Planner never changes real blocks for them. The same holds from
joining until the server's `hello` arrives. A `hello` with another protocol version leaves it off the same way, so a
player whose pack still has Building Planner 0.2.1 sees no ghost blocks until their next launch updates it. A
singleplayer world, or a LAN host's own world, runs no server side, and Effortless Building works there as it does
without Building Planner. The server sends nothing to a player who can't receive it (`ServerPlayNetworking.canSend`),
so players without the add-on, or without any mods, join and play as before. It sends `hello` only to a game that also
takes `columns`, since Building Planner 0.2 takes a `hello` of another layout under the same name.

### The add-on's licence

MIT (`mod/LICENSE`). Effortless Building and Litematica are LGPL-3.0; the add-on calls their code and copies none of
it, so it can take its own licence. The radial menu's icons are Lucide's, under its ISC licence, with its notice.

### In-game checks

`mod/clienttest/run.sh <checks>` runs the dev server and both dev clients in a Linux container, each client on its own
virtual screen with Mesa's software OpenGL, so nothing shows on the host. The clients carry a driver mod (the
`clienttest` source set, never in the add-on's jar) that runs commands from a file in the game folder: actions, waits
for a state, and screenshots. Key presses and clicks are real ones, sent with xdotool. The results, the game logs and
the screenshots go to a folder outside the repository. Each script below checks one part, with Planner1 and Planner2
on the dev server; [plan.md](plan.md) says which build step makes or rewrites each.

`step6.sh`, the layer:

- Each client starts a session on `hello`, and each client's Litematica placement list, boxes and selection show no
  tile.
- A shape Planner1 draws reaches Planner2 at once, glowing with Planner1's name, and the glow fades; no real block is
  placed.
- Planner2 erases some of Planner1's ghost blocks with the break click, and both clients show them gone.
- Ctrl+Z and Ctrl+Y undo and redo the player's own edit on both clients, and leave the other player's blocks.
- A shape in a chunk Planner2 hasn't loaded shows for Planner2 when they walk to it; a player who rejoins sees the
  edits made while they were away; a shape in the Nether doesn't show in the overworld.
- No real block inside a tile's box shows Litematica's "extra block" mark.
- The server, stopped and started again, reads back the same layer.

`step7.sh`, drawing:

- Single mode with the array modifier, and a Line shape, add ghost blocks on both clients, and no real blocks.
- A shape started on a ghost block goes on top of it.
- The mix key opens the mix screen, listing every block; the switch limits it to the inventory's two blocks; clicking
  both, typing their shares and "Use mix" turns the mix on and closes the screen, with its message, and the panel
  shows both blocks, their percentages and the key line.
- With the mix on and a block held, each block of a 20-block shape is the mix's block at its position, on both
  clients. With the mix on and a pickaxe or an apple held, the place click draws no ghost block and places no real
  one, and an apple's break click breaks the real block as without mods. In Disable, with the mix on and a block held, the place
  click puts a real block and no ghost block, and the use click opens a door.
- The mix key while the mix is on opens the screen with the mix in it; "Turn off" turns it off, with its message.
- A client without Effortless Building (its jar left off the classpath) loads without it or mixin errors, keeps
  receiving the other player's edits, and opens the mix screen.

`step8.sh` restarts the server and the clients as its checks need (`start_server` and `start_client` can leave the
add-on's classes off the classpath) and checks:

- On a server with the add-on, both clients start a session.
- A client without the add-on joins that server, stays connected and places a real block.
- On the same server with Building Planner left out, both clients start no session and show no Building Planner
  message; its keys open nothing; an Effortless Building shape adds no ghost blocks and places no real ones, with the
  player staying connected; and, by the driver's counter of the custom payloads leaving the client (a mixin in the
  `clienttest` source set), no place, break, undo or redo packet of Effortless Building is sent after a shape, Ctrl+Z,
  Ctrl+Y and the break click.
- A `hello` with another protocol version, which the driver hands to the client, starts no session and shows no
  message.

`step9.sh`, copy and paste and the material list:

- J, then a cube around some ghost blocks, makes a copy, with the message naming the Paste key, places no real block,
  doesn't reach the other player, and puts back the player's mode; the copy follows the player's aim.
- U turns it a quarter turn clockwise, and H, facing south, flips it east to west, with its stairs turning with it.
- Over other ghost blocks, the copy's blocks show where they overlap, and the ghost blocks show in its empty space.
- K pastes it: the other player sees its blocks at once, turned and mirrored as the copy showed them, glowing with the
  paster's name; the copy's empty space leaves the ghost blocks under it, and the copy stays. One Ctrl+Z undoes the
  whole paste, and Ctrl+Y redoes it.
- "Copy real blocks", then a cube around a real house, makes a copy of its blocks that pastes as ghost blocks.
- J over no ghost blocks gives its message; a copy over the block cap gives the message with the server's limit and
  keeps the earlier copy; Delete clears the copy; K with no copy gives its message.
- The Material list button, then a cube around 10 ghost stone bricks, 4 of them built for real, opens Litematica's
  material list with 10 in all and 6 missing.
- With ghost blocks hidden, a shape shows them again, then draws.
- The radial menu's buttons do what their keys do.

`step10.sh`, hold to repeat and the look of ghost blocks. The driver counts the ticks each mouse button was down and
notes the client tick of each edit the client sends (a mixin in the `clienttest` source set), and the script holds the
real buttons with xdotool while Planner1 is teleported one block every two ticks along a row, looking straight down:

- Holding use in Single mode with stone bricks sends an edit when the button goes down and then one every 4 ticks, and
  each edit put one ghost block in the row, on both clients, and no real block.
- Holding attack over a row filled with ghost blocks sends an edit when the button goes down and then one every 6
  ticks, and each edit erased one ghost block, on both clients, leaving the real blocks.
- With the placing slider set to 2 ticks and the erasing slider to 10, holding use sends an edit every 2 ticks and
  holding attack one every 10; the settings survive a restart.
- Holding use in Line mode (which needs two clicks) and with the mode disabled sends no edit.
- Each client's first start leaves Litematica's options at on and 0.8 (and the tool item off), saved in its config,
  and the line in Building Planner's client config.
- A player's own change (off and 0.3) survives a restart. With the line gone and the server stopped, so the client
  joins nothing, a start sets only the option still at its default (on) and keeps the 0.3.

The import has unit tests rather than an in-game check: they build the layer from a copy of the files Syncmatica and
Building Planner write (a `placements.json` with turned, mirrored and overlapping plans, one with a moved sub-region)
and compare each block with where Litematica's own code places it.

## Unverified

- Tiles in the game: how long adding thousands of them takes when a player joins near large builds (expected tens of
  milliseconds for 10,000, from `SchematicPlacementManager`'s copy-on-write list), and how much memory they take
  (Litematica's containers are dense, at least 2 bits a position: about 4 KB for a column two sections tall).
- That Litematica restores, without Syncmatica, the placements Syncmatica made, which the client removes at its start.
- That the widgets hooks E11 and E12 add take clicks and drags in the radial menu (read in `RadialMenu.mouseClicked`
  and Minecraft's `Screen`, not run), and how they fit beside the menu's own buttons at each GUI scale.
- The material list on a placement never added to Litematica (read in `MaterialListPlacement` and
  `TaskCountBlocksPlacement`, not run).
- Shapes drawn with Effortless Building's multi-click modes other than Line, and its mirror and radial mirror; the
  checks draw Single and Line shapes with the array, which take the same capture.
- How block states turn out for blocks whose placement depends on their neighbours (stairs, fences, slabs).
- Hold to repeat in survival mode, with a held tool or bucket, and on a slow connection, where a held click sends edits
  faster than the server's answers come back; the checks use creative mode, a held block and a local server.
- A client without any mods joining a server with the add-on; the checks use a Fabric client without the add-on.
- A real server running another protocol version; the check hands the client a made-up `hello`.
- That Effortless Building places real blocks as usual in singleplayer and in a LAN host's own world.
- Mirror in the game for a player facing east or west; the unit tests cover it.
- The import on the server's real plans: the unit tests use files made in the dev setup.
