# Building Planner

Building Planner is a Fabric add-on for Minecraft 26.3. With it, the group plans builds together as one shared layer of
ghost blocks that everyone on the server sees and edits live, then builds them by hand. Players draw with Effortless
Building; Litematica draws the ghost blocks, out of sight. It hooks into unchanged copies of both. Geo's decisions
behind it are in [plan.md](plan.md) ("Decisions").

**What's built.** Players run Building Planner 0.2.1, and the server 0.2.0, which keep ghost blocks in separate named
plans shared through Syncmatica. Built on `main` since then: hold to repeat (hook E10) with its speed sliders in the
radial menu (E11, E12), the look of ghost blocks, the block mix's rules, its panel and its "Turn off", clicks as
without mods with anything but an item that places a block (E13 to E15), the layer's storage and files, the server's
side of the layer and protocol 3, the one-time import of the plans Syncmatica shared, the player's copy of the layer
with its tiles, shapes and erasing in world positions, and the edit glow, hooks L1 and L4 to L7, which keep tiles out
of Litematica's screens and edits, the removal of old plans' placements, showing or hiding ghost blocks, with its
key, its radial menu button, and Litematica's tool item off at a first start, and copy and paste: Copy and "Copy real
blocks" with a box drawn in the "Cube" shape (hooks E1, E2, E13 to E15 and E17), the copy following the aim, Paste,
Turn, Mirror and Clear, their keys and radial menu buttons, and the copy kept out of Litematica's selection and
placement list (L4, L5). Neither side
speaks protocol 2's named plans or uses Syncmatica any more. Everything else this spec describes waits on the build
steps in [plan.md](plan.md) ("Agent-ready work"), which end with the release. The build reuses what still fits:
Effortless Building's hooks E1 to E10 (E1, E2 and E6 change), the rules of undo and redo, the edit glow, the block mix
screen's layout, the check for a server without the add-on, and the in-game check harness.

## What players get

- **Ghost blocks.** Each dimension has one layer of ghost blocks, shared with everyone on the server. Every change
  reaches everyone as it's made, and several players can draw at once, even on the same blocks. There are no plans to
  start, name, move or delete. Ghost blocks show see-through (see "How ghost blocks look").
- **Show or hide.** One switch shows or hides all ghost blocks for that player: a key ("Show or hide ghost blocks", N
  unless the player changes it in Controls) and a button in Effortless Building's radial menu, each saying in chat
  "Ghost blocks hidden." or "Ghost blocks shown.". Building Planner remembers the choice across game runs; ghost
  blocks show at first. Drawing a shape while they're hidden shows them again, then draws.
- **Drawing shapes.** With an Effortless Building mode chosen (single, line, wall, floor, cube, slope, circle,
  cylinder, sphere and the rest), the place (right) click draws the shape as ghost blocks, with Effortless Building's
  mirror, radial mirror and array (repeat). A shape drawn with the break (left) click erases ghost blocks, whoever
  placed them. Effortless Building never places or breaks real blocks. A shape can start on a ghost block as well as
  on a real block. Ghost blocks are drawn and erased only with an item that places a block in hand: a block, or the
  likes of redstone dust, fences, doors, signs, seeds and string; a door or a bed gets both its halves. With anything
  else in hand (a tool, food, an empty hand, or an item that places an entity or a fluid, such as a bucket, an item
  frame, an armor stand or a boat), every click acts as without mods, in every mode, breaking and using real blocks,
  and holding a button repeats as the game does.
- **Hold to repeat.** In Single mode, holding the place button keeps placing ghost blocks and holding the break button
  keeps erasing them. Two sliders in Effortless Building's radial menu, "Build speed" and "Break speed", set how fast,
  from 1 to 20 blocks a second; at first they're the game's own pace for a held button, five a second to place and
  just over three to erase. The first repeat waits at least 0.2 seconds, the game's own wait before a held click
  repeats, so a quick click places or erases one block at every speed. The other modes act once for each click.
- **Disable.** With "Disable" chosen in Effortless Building's radial menu, clicks act as they do without mods, placing
  and breaking real blocks, even with the block mix on.
- **Block mixes.** The block mix key (B unless the player changes it in Controls) opens the block mix screen, the only
  place to choose a mix's blocks: it lists every block in the game, with search and an "Only my inventory" switch. The
  player picks up to nine blocks, each with a share (say 80 cobblestone to 20 mossy cobblestone), and turns the mix on.
  The screen closes, and the mix then applies to Effortless Building's shapes, in every mode but Disable, while the
  player holds an item that places a block; with anything else in hand, clicks act as without mods. While the mix
  applies, a small panel at the right edge of the screen shows each block's icon and share. With the mix on, the mix
  key opens the screen again, which then has a "Turn off" button. A mix of one block lets a survival player plan with
  a block they don't have.
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
source the hooks were checked in; the dev server runs neither Effortless Building nor Syncmatica, and the dev clients
run without Syncmatica. Only the unit tests have Syncmatica on their classpath, so that its own writer makes the
import's fixture.

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
| E2 | same method | `@WrapWithCondition` on `PacketHandler.sendToServer(BreakBuildModePacket)` | Erase the shape's positions from the layer; don't send. While a box is awaited, take the box as E1 does and erase nothing, so either click draws it. |
| E3 | same method | `@WrapWithCondition` on `PlacedBlockTracker.clientTrackAll` | Don't record ghost positions as real blocks the player placed. |
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo and redo to the layer. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While the mix applies, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While the mix applies, treat the held block as Effortless Building's Randomizer, so the preview draws each entry's own block. In `shouldInterceptPlacing` this holds only while the mode isn't Disable (`BuildModes.CLIENT.getBuildMode() != BuildModeEnum.DISABLED`), so clicks in Disable stay vanilla's. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not ghost blocks. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapOperation` on both `ItemUsageTracker.compute` calls, clearing the tracker instead | No red "missing items" tint: ghost blocks don't use up items. |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `resolveFirstClickPos` | `@WrapOperation` on `ClientLevel.clip`; on `Level.getBlockState` | Let shapes start on ghost blocks: aim at the nearer of the real block and a ghost block (`GhostAim`, which traces the ray through the player's copy of the layer while ghost blocks show, not through Litematica's schematic world, which also holds the copy and shows an edit only after its rebuild; so a held click's repeat aims at the ghost blocks the click before it placed, as on real blocks), and read the ghost block's state, where the real block can be replaced, when picking the start position, so the shape goes beside it. |
| E10 | `buildpipeline/BuildPipelineClient.java`, public static `handleRightClick` and `handleLeftClick`, with `shouldInterceptPlacing`, `shouldInterceptBreaking` and `getBuildState`; `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem`; `buildmode/BuildModes.java`, `getBuildMode`; the click checks copy `fabric/.../EffortlessBuildingClient.java`'s client tick | No mixin: Building Planner's own `END_CLIENT_TICK` calls the two public methods again while a button stays down | Hold to repeat in Single mode (see "The client"). Effortless Building's own tick acts only when a button goes down (`rightJustPressed`, `leftJustPressed`). |
| E11 | `screen/RadialMenu.java`, `init()` | `@Inject` at tail, calling `Screen.addRenderableWidget` | Add Building Planner's buttons and sliders to the radial menu each time it opens (Minecraft clears a screen's widgets and calls `init` on each open). They're children of the screen, so `RadialMenu.mouseClicked`, which calls `super.mouseClicked` after its own `performAction`, routes clicks and drags to them. |
| E12 | `screen/RadialMenu.java`, `extractRenderState(GuiGraphicsExtractor, int, int, float)` | `@Inject` before its call to `drawTexts` | Draw those widgets: the method draws the menu itself and never calls `Screen`'s own drawing, which would draw them. Drawn before the menu's texts, they leave a hovered mode's name, which can fall on the sliders, on top. |
| E13 | `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem` | `@ModifyReturnValue` | On a server (with a session, or with Building Planner off), an item starts shapes only if it places a block (`ClickRule.placeable`: Minecraft's `BlockItem`, the same test as the mix's; `ClickRule` uses none of Litematica's classes, so these hooks can ask it while Building Planner is off). Effortless Building's own rule also takes buckets and tools with a `TOOL` component, and leaves out doors, beds and tall plants. While a box is awaited for copy or the material list, any held item, or none, counts, so it starts the cube. |
| E14 | `buildpipeline/BuildPipelineClient.java`, `shouldInterceptPlacing` and `shouldInterceptBreaking` | `@ModifyReturnValue` | With the same rule, while the main hand holds no item that places a block, no box is awaited and no shape is under way (`getBuildState()` null), neither click is Effortless Building's: its client tick, its `AttackBlockCallback`, and its cancelling of vanilla's `startUseItem`, `startAttack` and `continueAttack` all ask these first, and so does hold to repeat, so both clicks and holding them are vanilla's. A shape under way still takes the click that finishes or cancels it. |
| E15 | `Minecraft.pick(float)`, into which Effortless Building's `MixinGameRenderer` adds, at its tail, a reach of 32 for an empty hand, aiming through entities; `buildmode/BuildModes.java`, `getBuildMode` | `@WrapMethod` on `pick`; `@ModifyReturnValue` on `getBuildMode` | While the main hand holds no item that places a block, no box is awaited and no shape is under way, `getBuildMode` reads Disable during the pick, which that code skips, so the hand aims as without mods, at an entity before the block behind it. |
| E16 | `screen/RadialMenu.java`, `onClose()` | `@Inject` at head | Let Building Planner's buttons act when the menu closes, as the menu's own do: its `tick` calls `onClose` once its key is let go (Escape calls it too), and `onClose` acts on the menu's button under the pointer unless a click acted since the menu opened (`performedActionUsingMouse`). |
| E17 | `buildpipeline/BuildPipelineClient.java`, `resolveFirstClickPos` | `@ModifyReturnValue` | While a box is awaited, the cube's first corner is the aimed block itself, not the block beside it, so the box goes around the blocks the player aims at. |

Effortless Building's limits come from its config on the player's computer (`config/ServerConfig.java`; survival
defaults: reach 32, 2,000 blocks a shape, 64 a side). The server applies its own caps (below). The radial menu's own
buttons come from a fixed list (`RadialMenu.extractRenderState`, `buildmode/ModeOptions.java` `ActionEnum`), which is
why Building Planner adds widgets of its own rather than actions; they sit where the menu draws nothing, so its
`switchTo` and `doAction` stay empty when the player clicks one or lets the menu's key go over it.

Litematica, under `src/main/java/fi/dy/masa/litematica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| L1 | `util/SchematicUtils.java`: `setTargetedSchematicBlockState(BlockPos, BlockState)`, `setSchematicBlockStates`, `setAllStatesToAirExcept(SchematicPlacementManager, PlacementPart, BlockState, Level)`, `replaceAllIdenticalBlocks` | `@WrapWithCondition` on each `LitematicaBlockStateContainer.set(int, int, int, BlockState)` (the only four writes the ten Edit Schematic actions make, all called from `event/InputHandler.java`) | Skip a write into a tile's container, so Litematica's Edit Schematic actions never change the layer, even on a computer whose Litematica tool mode is still "Edit Schematic" (`InputHandler.handleUseKey` and `handleAttackKey` edit in that mode whatever the player holds). Writes to other placements, such as the copy, stay Litematica's own. |
| L4 | `schematic/placement/SchematicPlacementManager.java`, `setSelectedSchematicPlacement(SchematicPlacement)` | `@Inject` at head, cancellable | A tile or the copy is never selected: selecting one leaves the selection as it was, since selecting none would also clear Litematica's material list. Covers `addSchematicPlacement`, which selects a new placement when none is selected, `removeSchematicPlacement`, which then selects the first placement, selection by aim, and the placement list. Tiles and the copy are known as theirs before they're added. |
| L5 | `gui/widgets/WidgetListSchematicPlacements.java`, `getAllEntries()` | `@ModifyReturnValue` | Leave tiles and the copy out of Litematica's placement list. |
| L6 | `render/OverlayRenderer.java`, `updatePlacementCache()` | `@WrapOperation` on `SchematicPlacementManager.getAllSchematicsPlacements()` | Leave tiles out of the boxes and origin markers `renderBoxes` draws. Litematica's own switch for those boxes (`enablePlacementBoxesRendering`) would hide the copy's box too. |
| L7 | `render/schematic/ChunkRendererSchematicVbo.java`, `getOverlayType(BlockState, BlockState)` | `@ModifyReturnValue` | While Building Planner has a session, turn `EXTRA` into `NONE`. Litematica marks a real block as extra wherever a placement's box holds air there (`schematicOverlayTypeExtra`, on by default), and a tile's box is a whole chunk column's sections, so every real block in it would be marked. The layer holds only blocks to place, so no real block is extra. Missing and wrong blocks keep their colours. |

Public Litematica API the add-on calls: `LitematicaSchematic.createEmptySchematic`, `getSubRegionContainer`,
`getAreaSize`; `LitematicaBlockStateContainer`'s `get` and `set`; `SchematicPlacement.createFor`, `createTemporary`,
`setShouldBeSaved`, `getSchematic`, `getOrigin`, `setOrigin`, `getRotation`, `setRotation`, `getMirror`, `setMirror`;
`SchematicPlacementManager.getAllSchematicsPlacements`, `addSchematicPlacement`, `removeSchematicPlacement`,
`setSelectedSchematicPlacement`, `markChunkForRebuild`; `AreaSelection`'s constructor and `addSubRegionBox`; `Box`'s
constructor; `PositionUtils.getTransformedBlockPos`, `getMinCorner`, `getMaxCorner`;
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
`subregionData` when moved) mirrored and turned by the plan's, plus the plan's origin. Its state is mirrored by the
plan's mirror, then by the region's (east-west and north-south swapped when the plan's turn is a quarter turn), then
turned by both turns (`LitematicaSchematic.placeBlocksToWorld`, `PositionUtils.getTransformedPlacementPosition`).
Structure void is skipped, like air.

Where plans overlap, the later plan's blocks win: plans go in the order of their metadata's `TimeCreated`, then their
id, and a later plan's air leaves an earlier plan's blocks. A plan whose file can't be read is logged and skipped,
including one whose own file is damaged, which is never replaced by Syncmatica's; an entry of the list that lacks a
field or has a bad value is logged and skipped too. A list that can't be read at all stops the server, since starting
would make an empty layer and the plans would never move. The import writes the layer's files to
`building-planner/layer-import/` and then renames it `layer/`, so a crash part way leaves no layer and the next start
imports again; with no shared plans, `layer/` starts empty. It then logs how many plans and blocks it moved: "Building
Planner moved <plans> of <shared> shared plans into the layer: <blocks> ghost blocks". It leaves `plans/`, `deleted/`, `syncmatica/` and
`syncmatics/` where they are, unread from then on.

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
leaves. At its first tick, and the first tick after each change of dimension, it removes from Litematica every
placement whose schematic file lies in the game folder's `schematics/sync/` (where Syncmatica's client kept shared
plans) or in `Building Planner/` in Litematica's schematics folder (where Building Planner 0.2's "Start a plan" saved
them): Litematica saved those placements and restores each dimension's when the player enters it, and they would show
old plans over the layer. The files stay, and a plan the player loads from them later stays loaded.

A finished shape (E1, E2) goes as one `edit` of world positions. Each block's state is the one Effortless Building
would place: the block's placement rules with the first click's face and the player's facing, then the shape's mirror
and turn. Entries whose item isn't a block are left out. A block of two halves (a door, a tall plant, a bed) also gets
its other half, above it or toward the bed's head, where the shape has no block of its own; erasing either half of a
ghost one erases both, as breaking a real one does. A shape over the server's block cap (from
`hello`) isn't sent; the player gets the server's message.

An `applied` edit writes its blocks into the player's copy of the layer and into the tiles, as "Tiles" describes. When
the author is someone else and ghost blocks are showing, the changed blocks glow for 3 seconds, fading out: an outline
and a translucent fill on each block, or one translucent box around them for an edit of more than 512 blocks, with the
editor's name (from the player list) above them, drawn through MaLiLib's renderer. Messages from the server show in
the player's chat.

Hold to repeat (E10): while Building Planner has a session, Effortless Building is loaded and its mode is Single, the
client tick counts how many ticks each of the use and attack buttons has been down, with no screen open. After the
tick a button goes down, which Effortless Building's own tick handles, it calls `handleRightClick` (use) or
`handleLeftClick` (attack) again (`HoldRepeat`): first after the larger of 4 ticks and N, then every N ticks, where N
is the build or break slider's setting. The 4 ticks are the game's own wait before a held use button repeats (below),
so a click held under 0.2 seconds acts once at every speed. Each repeat comes under the conditions Effortless
Building sets for a fresh click (a build trigger item, or a sequence under way; for attack also
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
the layer, so showing needs no reload, and the edit glow draws nothing. A tile made while hidden is removed from
Litematica as soon as it's made: setting a placement up registers its chunks with Litematica's placement manager
(`SchematicPlacement.onModified`), which then draws it though it was never added. The state is kept in
`config/buildingplanner-client.properties` (`ghost-blocks-shown`) and starts as shown. When a shape starts (Effortless
Building's `getBuildState()` turns non-null, read once a client tick) or finishes (E1, E2), a paste is made, or a
material list is asked for while ghost blocks are hidden, they show again first, with a message the first time in each
session. Building Planner doesn't use Litematica's own rendering switch (`enableRendering`): while that's off, each
added tile would bring up Litematica's status panel for 10 seconds (`SchematicPlacementManager.onPlacementAdded`,
`StatusInfoRenderer.startOverrideDelay`), and it hides the copy too. Litematica's own switch still turns all its drawing
off.

The key and the button act only during a session; without one there are no ghost blocks to switch. Each time either
switches, it says so in chat: "Ghost blocks hidden." or "Ghost blocks shown." (`ClientSession.toggleGhosts`). The
button's icon shows the current state: an open eye while ghost blocks show, a closed eye while they're hidden. Showing
them again for a shape doesn't say "Ghost blocks shown."; the first time in a session it says "Ghost blocks shown
again. Press N to hide them.", with the key as the player has it bound (`ClientSession.showForDrawing`).

### The radial menu's buttons and sliders

Hooks E11 and E12 add Building Planner's widgets to Effortless Building's radial menu (opened by holding its key, left
Alt by default; it closes when the key is let go): two sliders, "Build speed" and "Break speed", just under the menu's
left-hand buttons (the wrench, undo and redo, and the row under them); buttons for "Show or hide ghost blocks",
"Copy", "Paste", "Copy real blocks" and "Material list"; and "Turn", "Mirror" and "Clear" while a copy exists. The
widgets sit by the menu's middle, as its own buttons do (`RadialMenuWidgets`): the menu's left-hand buttons are 20
across with their middles 26 apart, the leftmost 157 left of the middle, in two rows 13 above and below it, so they
span 167 to 95 left of the middle and end 23 below it. The sliders, with 2 between
them, start 29 below the middle, 6 under the lower row as it is under the upper, and end above the description of a
mode under the pointer, which the menu draws 100 below the middle. Each slider draws as the game's own sliders do
(`RepeatSlider`), with its text at the size of the mix panel's (`SmallText`, below), full size at GUI scales 1 to 3.
At full size the widest label, "Break speed: 3.33 blocks/s", is 137 across, more than the 102 between
the buttons' left edge and the menu's ring of modes (65 from the middle at most), so the sliders, on one line each,
end 65 left of the middle, outside the ring, and are as wide as the longest label either shows needs at the current
size, plus 4 on each side (145 at full size, 111 at GUI scale 4), reaching further left than the buttons (709 revision
3 Q1 A). They are 12 high and end 55 below the middle. At 854 by 480, GUI scale 2, they start 3 from the screen's left
edge; on a GUI narrower than 420 across at full size, such as 1024 by 768 at GUI scale 3 (341 across), they run off
it.

Each slider shows its speed in blocks a second, to two decimals at most ("Build speed: 5 blocks/s", "Break speed: 3.33
blocks/s" at first), so each step reads differently. Its steps are the tick gaps 20 to 1 from left to right (1 to 20
blocks a second); a click or a drag sets it, and the handle settles on the chosen step when let go. Each step it
reaches is saved at once in `config/buildingplanner-client.properties` (`place-repeat-ticks`, `erase-repeat-ticks`; at
first 4 and 6, which a missing value, or one that isn't a whole number from 1 to 20, reads as), where hold to repeat
reads it each tick. The sliders show and save on any server, and take effect only where hold to repeat runs.

Building Planner's buttons (`RadialButton`) look and act like the menu's own: 20 across, grey, light blue under the
pointer, with a 16-pixel icon centred in them, and, on hover, the button's name in aqua and its key, when it has one,
in grey. As with Effortless Building 4.3's own buttons (`RadialMenu`), each acts when the player lets go of the menu's
key with the pointer over it (hook E16; Escape closes the menu the same way), and on a click of any mouse button over
it, which leaves the menu open; after a click has acted, letting the key go acts on nothing. Each action plays the
menu's quiet click (`RadialMenu.playRadialMenuSound`). The buttons sit above the wrench, undo and redo, in the same
columns (157, 131 and 105 left of the middle), in rows 39 and 65 above the middle: "Show or hide ghost blocks", Copy
and Paste, then "Copy real blocks" and "Material list"; and in the row 91 above the middle, in the same columns, Turn,
Mirror and Clear, which show only while a copy exists. Copy, Paste, "Copy real blocks", Turn, Mirror and Clear do what
their keys do (see "Copy and paste"). "Material list" comes with step 11; until then it shows, doing nothing, only while
the development system property `buildingplanner.draftButtons` is `true`, which also shows Turn, Mirror and Clear
without a copy, so the checks can place all eight.

Each icon is a 16 by 16 image of its own in `mod/src/main/resources/assets/buildingplanner/textures/gui/radial/`,
named by what it shows: `ghost_blocks_shown`, `ghost_blocks_hidden`, `copy`, `paste`, `copy_real_blocks`,
`material_list`, and `turn`, `mirror` and `clear` for step 10's buttons. Changing an icon means replacing its file. Each
draws at 16 by 16 GUI pixels with nearest-pixel filtering, as the menu draws its own (`AllIcons.render`). The icons
were generated with OpenRouter's `google/gemini-3-pro-image` (Nano Banana Pro) on 2026-11-17, as Set 4 of 30
candidate sets, which Geo chose for now (inbox request 732); Geo is to pick each icon later.

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
clicking a block in the mix takes it out. "Use mix" turns the mix on and closes the screen, with the message "Block mix
on. Press B to change it or turn it off." (the key as the player has bound it); "Clear" empties the list; "Cancel" and
Escape leave the mix as it was. Opened while the mix is on, the screen shows the mix as it is, to change it, and has a
"Turn off" button between "Use mix" and "Clear", which turns the mix off, closes the screen and says "Block mix off.";
the mix's blocks stay in the screen for the next time. The buttons share the row's width.

While the mix applies, a panel at the right edge of the screen (`MixPanel`, drawn through `HudElementRegistry`),
centred top to bottom on a dark see-through box, shows the heading "Block mix", then each of the mix's blocks as its
icon and its percentage ("80%"). The box is as wide as its widest line. The whole panel, like the radial menu's
slider labels, draws at the smallest size at or above 70% of the game's at which each of the font's pixels covers a
whole number of screen pixels (`SmallText`), since at 70% they come out uneven and hard to read: ceil(GUI scale × 0.7)
/ GUI scale, which is full size at GUI scales 1 to 3, 75% at 4, 80% at 5 and 5/6 at 6. It hides while the mix doesn't
apply (`MixRule`), while a screen is open, and while
the game's HUD is hidden (F1). Without Effortless Building, whose mode the rule reads, it never shows.

### Copy and paste

The copy is a Litematica placement of a schematic kept in memory on the player's computer. Only that player sees it,
and Litematica doesn't save it, since it has no file; hook L4 keeps it from being selected, and L5 leaves it out of the
placement list, as for tiles. Its keys and buttons, each key changeable in Controls under Building Planner: Copy (J),
"Copy real blocks" (no key unless the player sets one), Paste (K), Turn (U), Mirror (H) and Clear (Delete). None of the
keys is a default key, or part of a default key combination, of Minecraft 26.3, Litematica, MaLiLib, Effortless
Building 4.3 or Freecam 1.5.1.

Copy and "Copy real blocks" wait for a box: they switch Effortless Building to its "Cube" mode, remembering the mode
the player had, with a message saying to draw a box around what to copy. The cube's first click takes the aimed block
itself as a corner (hook E17), its preview shows as usual, and its last click, with either button (hooks E1 and E2),
makes the copy, places or erases nothing, and puts back the player's mode at the next client tick (switching back
within the click would sound as a cancelled shape). While a box is awaited, any held item, or none, draws the cube
(E13 to E15). Pressing Copy or "Copy real blocks" while a box is awaited, choosing another mode, which stays, or Clear
stops the wait, without a message. Copy takes the ghost blocks in the box from the player's copy of the layer;
"Copy real blocks" takes the real blocks in the box from the player's world (`ClientLevel.getBlockState`), every
non-air block state, without block entity contents. Each block keeps its world position relative to the box. The
copy's box is the smallest box around the copied blocks, and its schematic holds structure void in every position
with no copied block, so the ghost blocks under its empty space show. When the box has no blocks to copy, the player
gets a message and nothing changes. A copy of more blocks than the server's block cap is refused with a message,
since its paste would be. Otherwise the new copy replaces any earlier one, and the player gets a message naming the
Paste key. Effortless Building's own limit on a shape's sides (64 in survival by default) limits the box.

The copy follows where the player aims: each client tick its origin, its corner with the lowest
coordinates, goes on the block beside the face the player aims at, real or ghost (as hook E9 aims: ghost blocks count
only while they show, and the copy itself never does, or it would creep toward the player), within 64 blocks; when
they aim at nothing, it stays where it was, and a copy made then starts at the box's corner. Its box stays drawn
(hook L6 leaves out only tiles). Turn gives the copy a quarter turn clockwise, seen from above, around
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
it, so it can take its own licence. The radial menu's icons were made for Building Planner with an image model ("The
radial menu's buttons and sliders" names it).

### In-game checks

`mod/clienttest/run.sh <checks>` runs the dev server and both dev clients in a Linux container, each client on its own
virtual screen with Mesa's software OpenGL, so nothing shows on the host. The clients carry a driver mod (the
`clienttest` source set, never in the add-on's jar) that runs commands from a file in the game folder: actions, waits
for a state, and screenshots. Key presses and clicks are real ones, sent with xdotool. The results, the game logs and
the screenshots go to a folder outside the repository. Each script below checks one part, with Planner1 and Planner2
on the dev server; [plan.md](plan.md) says which build step makes or rewrites each.

`step6.sh`, the layer:

- Each client starts a session on `hello`, and each client's Litematica placement list, boxes and selection show no
  tile, though the tile is Litematica's only placement.
- Litematica's own edits (a block placed into a tile, a fill over one) leave the layer and the tile as they were.
- A shape Planner1 draws reaches Planner2 at once, glowing with Planner1's name, and the glow fades; no real block is
  placed.
- Planner2 erases some of Planner1's ghost blocks with the break click, and both clients show them gone.
- Ctrl+Z and Ctrl+Y undo and redo the player's own edit on both clients, and leave the other player's blocks.
- A shape in a chunk Planner2 hasn't loaded shows for Planner2 when they walk to it; a player who rejoins sees the
  edits made while they were away; a shape in the Nether doesn't show in the overworld.
- No real block inside a tile's box shows Litematica's "extra block" mark, and a ghost block with no real block
  there is still marked as missing.
- Placements of old plans' files in `schematics/sync/` and `schematics/Building Planner/`, which Litematica saves and
  restores at the next start, are gone once the session starts, and their files stay.
- The server, stopped and started again, reads back the same layer.

`step7.sh`, drawing:

- Single mode with the array modifier, and a Line shape, add ghost blocks on both clients, and no real blocks.
- A shape started on a ghost block goes on top of it.
- The mix key opens the mix screen, without "Turn off", listing every block; the switch limits it to the inventory's
  two blocks; clicking both, typing their shares and "Use mix" turns the mix on and closes the screen, with its
  message, and the panel shows both blocks' percentages ("80%", "20%"). F1 hides the panel and shows it again. The
  panel is shot at GUI scale 2 and, in a window of 1920 by 1080, at 4.
- With the mix on and a block held, each block of a 20-block shape is the mix's block at its position, on both
  clients. With the mix on and a pickaxe or an apple held, the panel hides, the place click draws no ghost block and
  places no real one, and an apple's break click breaks the real block as without mods. In Disable, with the mix on and
  a block held, the panel hides, the place click puts a real block and no ghost block, and the use click opens a door.
- The mix key while the mix is on opens the screen with the mix in it and a "Turn off" button, and the panel hides
  while the screen is open; "Turn off" turns the mix off and closes the screen, with its message, and the panel stays
  hidden.
- In Single mode with the mix off, with a pickaxe, an apple or nothing in hand, every click is vanilla's and sends no
  edit: the place click on a real block or a ghost block does nothing, and the break click breaks a real block, and
  on a ghost block breaks the real block under it and leaves the ghost block.
- In Single mode and in Line mode: a pickaxe breaks a real block, an empty hand opens a door and a water bucket places
  water, each with no edit; redstone dust draws a ghost block, on both clients, and places no real block.
- A client without Effortless Building (its jar left off the classpath) loads without it or mixin errors, keeps
  receiving the other player's edits, and opens the mix screen.

`step8.sh` restarts the server and the clients as its checks need (`start_server` and `start_client` can leave the
add-on's classes off the classpath) and checks:

- On a server with the add-on, both clients start a session.
- A client without the add-on joins that server, stays connected and places a real block.
- A client with the add-on and Effortless Building but without Litematica and MaLiLib loads with Building Planner off,
  joins that server and stays connected.
- On the same server with Building Planner left out, both clients start no session and show no Building Planner
  message; the block mix key opens nothing; an Effortless Building shape adds no ghost blocks and places no real ones, with the
  player staying connected; and, by the driver's counter of the custom payloads leaving the client (a mixin in the
  `clienttest` source set), no place, break, undo or redo packet of Effortless Building is sent after a shape, Ctrl+Z,
  Ctrl+Y and the break click.
- A `hello` with another protocol version, which the driver hands to the client, starts no session and shows no
  message.

`step9.sh`, show or hide, the radial menu's buttons, copy and paste and the material list. Each check of the material
list waits on build step 11: until then its section logs one failure marked with that step and doesn't run. Boxes are
drawn with three real clicks of the "Cube" shape, aimed with the driver's `lookat`, while the players fly so they stay
where they're teleported.

- N hides every ghost block for that player alone, saying "Ghost blocks hidden." in chat: the game keeps them in its
  copy of the layer, Litematica has no tile, and the client config keeps the choice. Another player's edits, one of
  them in a column that had no ghost blocks, reach the hidden copy without showing or glowing, and N shows them with
  the rest, saying "Ghost blocks shown.".
- Letting go of the radial menu's key with the pointer on its "Show or hide ghost blocks" button hides them, with the
  chat line, and leaves Effortless Building's mode as it was; doing it again shows them, with its line. A click on the
  button hides them with the menu staying open, and letting the key go over it then doesn't act again.
- With ghost blocks hidden, a shape shows them again, then draws, with its message the first time only and never
  "Ghost blocks shown.".
- Hidden ghost blocks stay hidden after a restart, until N shows them.
- At 854 by 480, GUI scale 2, and at 1920 by 1080, GUI scales 2 and 4, the buttons, all eight shown,
  meet nothing the menu draws in any mode (its ring, buttons, mode options, mode names, descriptions and watermark),
  the sliders, each other or the screen's edges (the driver's `radialclear`); each is shot with ghost blocks hidden,
  with the buttons' boxes in the log, and at 1920 by 1080, GUI scales 2 and 4, so is the show or hide button's hover
  text with ghost blocks showing.
- J waits for a box in "Cube" mode, with its message; a cube around some ghost blocks then makes a copy, with the
  message naming the Paste key, places no real block, doesn't reach the other player, and puts back the player's mode.
  The copy's corner sits on the aimed block and follows the aim, and Litematica neither selects nor lists it.
- U turns it a quarter turn clockwise, and H, facing south, flips it east to west, with its stairs turning with it.
- Over other ghost blocks, the copy's blocks show where they overlap, and the ghost blocks show in its empty space.
- K pastes it: the other player sees its blocks at once, turned and mirrored as the copy showed them, glowing with the
  paster's name; the copy's empty space leaves the ghost blocks under it, and the copy stays. One Ctrl+Z undoes the
  whole paste, and Ctrl+Y redoes it.
- "Copy real blocks", from the radial menu, then a cube around a real house, makes a copy of its blocks that the
  menu's Paste button pastes as ghost blocks.
- J over no ghost blocks gives its message; a copy over the block cap gives the message with the server's limit and
  keeps the earlier copy; Delete clears the copy; K with no copy gives its message; a box of one block has its own
  message.
- J again, Delete, or another mode stops a wait; the other mode stays.
- The Material list button, then a cube around 10 ghost stone bricks, 4 of them built for real, opens Litematica's
  material list with 10 in all and 6 missing.
- The radial menu's Turn, Mirror, Copy (twice), Clear and Paste buttons do what their keys do, and Turn, Mirror and
  Clear go when the copy does.
- On the same server with Building Planner left out, N, J, K, U, H and Delete make no copy, open nothing and leave
  ghost blocks shown.

`step10.sh`, hold to repeat and the look of ghost blocks. The driver counts the ticks each mouse button was down and
notes the client tick of each edit the client sends (a mixin in the `clienttest` source set), and the script holds the
real buttons with xdotool while Planner1 is teleported one block every two ticks along a row, looking straight down:

- Holding use in Single mode with stone bricks sends an edit when the button goes down and then one every 4 ticks, and
  each edit put one ghost block in the row, on both clients, and no real block.
- Holding attack over a row filled with ghost blocks sends an edit when the button goes down and then one every 6
  ticks, and each edit erased one ghost block, on both clients, leaving the real blocks.
- At GUI scale 2 and, in a window of 1920 by 1080, at 4, the radial menu's sliders end above the description of the
  mode under the pointer, and start on the screen; each is shot.
- The radial menu, opened with left Alt held, shows both sliders at their first settings ("Build speed: 5 blocks/s",
  "Break speed: 3.33 blocks/s"). A click on the build slider sets it to 10 blocks a second and a drag on the break
  slider sets it to 2; hold to repeat takes both at once, and Building Planner's client config keeps them. Holding use
  then sends an edit 4 ticks after the button goes down and then every 2 ticks, and holding attack one every 10. After
  a restart the sliders show the same settings, and holding use still repeats every 2 ticks.
- With both sliders dragged to 20 blocks a second, a quick use click and a quick attack click each send one edit, and
  holding either sends its first repeat 4 ticks after the button goes down and then one every tick.
- The radial menu's wrench button opens Effortless Building's Modifiers screen.
- Holding use in Line mode (which needs two clicks) and with the mode disabled sends no edit.
- Each client's first start leaves Litematica's options at on and 0.8 (and the tool item off), saved in its config,
  and the line in Building Planner's client config.
- A player's own change (off and 0.3) survives a restart. With the line gone and the server stopped, so the client
  joins nothing, a start sets only the option still at its default (on) and keeps the 0.3.

The import's unit tests (`PlanImportTest`) build the layer from files the writers' own code makes: a `placements.json`
from Syncmatica's `ServerPlacement.toJson`, with turned, mirrored and overlapping plans and one with a moved and turned
sub-region of negative size; plans in Building Planner's own files and in Syncmatica's stored files written by
Litematica; a plan with no file and one with a damaged file. They compare each block with where Litematica's own
arithmetic and reader place it. `import.sh` copies that fixture into the dev server's world: the server, without
Syncmatica, logs the import's counts, and Planner1, standing at the plans, holds every expected ghost block and no
other in the game's own copy of the layer, again after a restart, which doesn't import again.

## Unverified

- Tiles in the game: how long adding thousands of them takes when a player joins near large builds (expected tens of
  milliseconds for 10,000, from `SchematicPlacementManager`'s copy-on-write list), and how much memory they take
  (Litematica's containers are dense, at least 2 bits a position: about 4 KB for a column two sections tall).
- That Litematica restores, without Syncmatica, the placements Syncmatica made, which the client removes at its start;
  `step6.sh` checks placements Litematica made itself of files in the same folders.
- The removal of old plans' placements after a change of dimension: the checks cover a session's start.
- How the radial menu's widgets fit at sizes other than the checks' (854 by 480 at GUI scale 2, 1920 by 1080 at 4);
  their positions follow the menu's middle, so larger screens leave more room.
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
- The import on the server's real plans: the unit tests and `import.sh` use files the writers' own code makes in a
  test, not files from players' sessions.
