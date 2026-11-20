# Building Planner

Building Planner is a Fabric add-on for Minecraft 26.3. With it, the group plans builds together as one shared layer of
ghost blocks that everyone on the server sees and edits live, then builds them by hand. Players draw with Effortless
Building; Litematica draws the ghost blocks, out of sight. It hooks into unchanged copies of both. Geo's decisions
behind it are in [plan.md](plan.md) ("Decisions").

**What's built.** The server and the player pack run Building Planner 0.5.0, which has everything this spec describes
except 0.6.0's changes, from Geo's requests of November 20th (in [plan.md](plan.md) "Decisions"):

- the copy placed with the controls of Satisfactory's build gun: against the aimed face, snapping to recent pastes,
  locking and nudging, Zoop and Vertical, and its box in blue, yellow and red (see "Copy and paste", "Building
  Planner's mouse modes" and "Keys");
- the radial menu's Delete button and its box (see "The Delete box");
- ghost blocks leaving the layer once real blocks fill them, with protocol version 5 (see "Ghost blocks the real world
  fills");
- editing ghost blocks from Freecam's camera (see "Editing from Freecam");
- an older shape's ghost blocks outside a new shape staying drawn: in 0.5.0 Geo saw most or all of an older shape
  disappear when a new shape was drawn across it (see "Overlapping shapes").

The build steps are in [plan.md](plan.md) ("Agent-ready work").

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
- **Block mixes.** The block mix key (B unless the player changes it in Controls) and the Block mix button in the
  radial menu open the block mix screen, the only place to choose a mix's blocks: it lists the blocks in the player's
  inventory at first ("Only my inventory" starts on), or every block in the game with that switch off, with search. A
  click on a block adds it to the mix, and a second click takes it out. The player picks up to nine blocks, each with
  a share (say 80 cobblestone to 20 mossy cobblestone), and turns the mix on. The screen closes, and the mix then
  applies to Effortless Building's shapes, in every mode but Disable, while the player holds an item that places a
  block; with anything else in hand, clicks act as without mods. While the mix applies, a small panel at the right edge
  of the screen shows each block's icon and share, and "B: Edit mix" under them. "Clear Mix" empties the mix and turns
  it off. A mix of one block lets a survival player plan with a block they don't have.
- **Undo and redo.** Undo takes back only that player's own changes, newest first: a whole shape or paste at a time,
  in any dimension. A block someone else has changed since stays as they left it. Effortless Building's Ctrl+Z (and
  the undo button in its radial menu) undoes, and its Ctrl+Y (and the redo button) redoes the newest undone change,
  until the player's next new change.
- **Others' edits.** When someone else changes ghost blocks, the changed blocks glow for a few seconds, with the
  editor's name beside them.
- **Built ghost blocks go.** Once a real block fills a ghost block (the same block, placed the same way), the ghost
  block leaves the layer for everyone, however the real block got there, including ghost blocks built before 0.6.0.
  Breaking the real block later doesn't bring the ghost block back (see "Ghost blocks the real world fills").
- **Delete.** The radial menu's Delete button draws a box, with the same limits as a box for Copy, and erases every
  ghost block in it as one change, which one undo takes back (see "The Delete box").
- **Overlapping shapes.** A shape drawn across older ghost blocks replaces them only where the two meet; the older
  shape's other ghost blocks stay (see "Overlapping shapes").
- **Freecam.** With Freecam's camera on, shapes, erasing, boxes and the copy aim from the camera, as if the player
  stood there (see "Editing from Freecam").
- **Boxes.** Copy, Delete and the material list each start Building Planner's own box mode, which only the left click works:
  three left clicks set a corner, the opposite corner on the same level, and the height, and a right click leaves the
  mode. The first corner is the space above the aimed block when the player aims at its top, and the aimed block
  otherwise. The screen shows what the box is for, the click that comes next, the box's size, and the blocks in it, on
  as many lines as they need. Pressing another of Copy, Delete and "Material list" while a box is drawn switches the
  box to that purpose. A box may be up to 256 blocks a side, and it never shrinks to fit a limit (see "Boxes for Copy
  and the material list").
- **Copy and paste.** The player presses the Copy button in the radial menu and draws a box: the blocks inside, ghost
  blocks and real blocks alike, become a copy, which only that player sees; where a ghost block stands in place of a
  real block, the copy takes the ghost block. While ghost blocks are hidden, the copy takes only real blocks. The copy
  then shows where the player aims, resting on the aimed face with its middle under the crosshair, and its controls,
  those of Satisfactory's build gun, show on screen: a left click pastes it into the layer as one change, which one
  undo takes back, and the copy stays for more pastes; the scroll wheel turns it; a middle click mirrors it; H locks it
  in place, and the arrow keys, Page Up and Page Down then nudge it a block at a time; R switches between Single, Zoop
  (a row of up to 10 copies, laid with two clicks) and Vertical; the copy snaps flush to the player's recent pastes,
  and holding Left Ctrl keeps it in line with the latest; its box shows blue, yellow where it would replace ghost
  blocks, and red where it can't be pasted; a right click puts it away. These take the place of what the clicks and
  the wheel do otherwise, such as changing the hotbar slot (see "Copy and paste"). The empty space in the copy leaves
  the ghost blocks under it as they are. Doing anything else in Building
  Planner or Effortless Building, such as choosing "Wall", pressing "Material list", or showing or hiding ghost
  blocks, also puts the copy away; undo, redo and the speed sliders keep it at the aim. The radial menu's Paste button
  brings back the most recent copy at the aim.
- **Material list.** The player presses the "Material list" button in the radial menu and draws a box: Litematica's
  material list screen opens for the blocks inside, ghost blocks and real blocks alike, with how many of each block the
  ghost blocks still need. While ghost blocks are hidden, it lists only the real blocks and leaves the ghost blocks
  hidden. A box with no blocks opens an empty list. The screen's "Info HUD" button pins the list to the screen as a
  panel like the block mix's, with each block's "All", "Hold" and "Need", in the screen's order and with its
  multiplier. "Save list" keeps the list under a name, and "Saved lists" opens a kept list again. The latest list
  counted from a box is always kept too, as "Last counted". The server keeps each player's lists, so they're the same
  from any computer, on that server only. The panel stays on across restarts, with the same list, until the player
  turns it off. The radial menu's "Material lists" button opens the current list (see "The material list").
- **Building Planner's name.** Where players see Effortless Building's name in the game, the corner of the radial
  menu, it reads "Building Planner". Effortless Building's four keys in Controls show under the "Building Planner"
  heading, with Building Planner's own.
- **Litematica's area selection.** On each player's computer, once, Building Planner turns off Litematica's drawing of
  area selections, so a selection a player left in Litematica, with its corners and its origin, doesn't show (see
  "Litematica's area selection").
- **M opens Freecam's menu.** On each player's computer, once, Building Planner binds M to Freecam's menu and takes M
  from Litematica's menu, which players then have no key to open (see "Keys").
- **How ghost blocks look.** On each player's computer, once, Building Planner turns on Litematica's "render blocks as
  translucent" and sets its ghost block alpha to 0.8, so ghost blocks draw see-through. It sets each only if the player
  hasn't changed it from Litematica's default (off, and 0.5), and never again, so a player's own later change stays.
  This happens whether or not the player joins a server with Building Planner.
- **Messages.** Building Planner's messages show in the player's chat. Every text players see (messages, key names,
  button and slider labels, the panel) is in the wording Geo approved, which this spec quotes.

## Geo's goals, and what meets each

| Goal (402 r1, r2) | Met by | Gap |
|---|---|---|
| Measure layouts | Effortless Building shows a shape's size while it's drawn; Building Planner's box for Copy, Delete and the material list shows its size and the blocks in it before anything is placed. | Measures are each player's own until drawn as ghost blocks. MiniHUD-style personal guides were rejected (402 r2, c2). |
| Plan room sizes and shapes before committing | Effortless Building's shapes drawn as ghost blocks, erased with the break click. | None. |
| See and judge a build (free or fixed camera) | Litematica draws the ghost blocks; Freecam moves the camera away from the player; the switch hides them to compare with the real world; with Freecam's camera on, every edit aims from the camera (see "Editing from Freecam"). | None. |
| Lay out walls, shapes and components quickly | Effortless Building's eleven build modes; hold to repeat in Single mode at the player's chosen speed. | None. |
| Copy and paste | Copy takes the ghost blocks and real blocks in a box; a left click pastes the copy into the layer, placed with the controls of Satisfactory's build gun. Effortless Building's array repeats while drawing. | None. |
| Symmetry | Effortless Building's mirror and radial mirror. | None. |
| Randomized block mixes | Building Planner's block mix (Effortless Building's Randomizer needs the server copy, which Geo kept off the server). | None. |
| A plan everyone on the server sees, built for real by hand | The shared layer reaches everyone live; Litematica's ghost blocks, its missing-block colours and the material list for a marked area guide the build. | None. |

## Technical design

### Parts

One jar, mod id `buildingplanner`, installed on the server and in the player pack. It registers no blocks, items or
menus, so it adds nothing to the registries Fabric API syncs at login, and players without mods can still join.

- **Common:** the layer's column storage and its file format, the network payloads, the material lists' format and
  limits, the hold-to-repeat timer and the look defaults' rules.
- **Server:** the layer store and its files, column watching, edits and their order, per-player undo, each player's
  material lists and their files, and the one-time import of the plans Syncmatica shared. They run only on a dedicated server, which is how the group plays.
- **Client:** the player's copy of the layer and its column watching, the tiles that hand it to Litematica and the
  hooks that hide them, the show or hide switch, the Effortless Building capture, hold to repeat, the radial menu's
  buttons and sliders, the block mix and its panel, copy and paste, the material list with its panel and saved lists,
  the edit glow, the settings it changes once, and the check for a server without the add-on.

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
| E1 | `buildpipeline/BuildPipelineClient.java`, `handleClick` | `@WrapWithCondition` on `PacketHandler.sendToServer(PlaceBuildModePacket)`, reading the local `BlockSet blocks` | Take the finished shape (every entry, after mode, modifiers, mix and constraints) into the layer; don't send the packet. `getFirstClickHit()` is still set here, so block states use the first click's face. |
| E2 | same method | `@WrapWithCondition` on `PacketHandler.sendToServer(BreakBuildModePacket)` | Erase the ghost blocks among the shape's positions, in the game's copy of the layer, with the other half of each two-block ghost block (`GhostErase.of`); don't send. A shape over no ghost blocks sends no edit. |
| E3 | same method | `@WrapWithCondition` on `PlacedBlockTracker.clientTrackAll` | Don't record ghost positions as real blocks the player placed. |
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo and redo to the layer. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While the mix applies, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While the mix applies, treat the held block as Effortless Building's Randomizer, so the preview draws each entry's own block. In `shouldInterceptPlacing` this holds only while the mode isn't Disable (`BuildModes.CLIENT.getBuildMode() != BuildModeEnum.DISABLED`), so clicks in Disable stay vanilla's. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not ghost blocks. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapOperation` on both `ItemUsageTracker.compute` calls and on `BreakDisplayTracker.compute`, clearing the tracker instead | No red "missing items" tint: ghost blocks don't use up items. No list of the real blocks under a shape drawn with the break click either (`render/RenderHandler.java`, `drawBreakingStacks`, which draws them in one row from the screen's middle that never wraps): that shape erases ghost blocks and leaves those real blocks. |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `resolveFirstClickPos` | `@WrapOperation` on `ClientLevel.clip`; on `Level.getBlockState` | Let shapes start on ghost blocks: aim at the nearer of the real block and a ghost block (`GhostAim`, which traces the ray through the player's copy of the layer while ghost blocks show, not through Litematica's schematic world, which also holds the copy and shows an edit only after its rebuild; so a held click's repeat aims at the ghost blocks the click before it placed, as on real blocks), and read the ghost block's state, where the real block can be replaced, when picking the start position, so the shape goes beside it. |
| E10 | `buildpipeline/BuildPipelineClient.java`, public static `handleRightClick` and `handleLeftClick`, with `shouldInterceptPlacing`, `shouldInterceptBreaking` and `getBuildState`; `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem`; `buildmode/BuildModes.java`, `getBuildMode`; the click checks copy `fabric/.../EffortlessBuildingClient.java`'s client tick | No mixin: Building Planner's own `END_CLIENT_TICK` calls the two public methods again while a button stays down | Hold to repeat in Single mode (see "The client"). Effortless Building's own tick acts only when a button goes down (`rightJustPressed`, `leftJustPressed`). |
| E11 | `screen/RadialMenu.java`, `init()` | `@Inject` at tail, calling `Screen.addRenderableWidget` | Add Building Planner's buttons and sliders to the radial menu each time it opens (Minecraft clears a screen's widgets and calls `init` on each open). They're children of the screen, so `RadialMenu.mouseClicked`, which calls `super.mouseClicked` after its own `performAction`, routes clicks and drags to them. |
| E12 | `screen/RadialMenu.java`, `extractRenderState(GuiGraphicsExtractor, int, int, float)` | `@Inject` before its call to `drawTexts` | Place those widgets for the opening animation (E20) and draw them: the method draws the menu itself and never calls `Screen`'s own drawing, which would draw them. Drawn before the menu's texts, they leave a hovered mode's name, which can fall on the sliders, on top. |
| E13 | `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem` | `@ModifyReturnValue` | On a server (with a session, or with Building Planner off), an item starts shapes only if it places a block (`ClickRule.placeable`: Minecraft's `BlockItem`, the same test as the mix's; `ClickRule` uses none of Litematica's classes, so these hooks can ask it while Building Planner is off). Effortless Building's own rule also takes buckets and tools with a `TOOL` component, and leaves out doors, beds and tall plants. |
| E14 | `buildpipeline/BuildPipelineClient.java`, `shouldInterceptPlacing` and `shouldInterceptBreaking` | `@ModifyReturnValue` | While the player draws a box or the copy is at the aim (`MouseMode`), neither click is Effortless Building's in any mode, so hooks M1 to M5 take them. With the same rule as E13, while the main hand holds no item that places a block and no shape is under way (`getBuildState()` null), neither click is Effortless Building's either: its client tick, its `AttackBlockCallback`, and its cancelling of vanilla's `startUseItem`, `startAttack` and `continueAttack` all ask these first, and so does hold to repeat, so both clicks and holding them are vanilla's. A shape under way still takes the click that finishes or cancels it. |
| E15 | `Minecraft.pick(float)`, into which Effortless Building's `MixinGameRenderer` adds, at its tail, a reach of 32 for an empty hand, aiming through entities; `buildmode/BuildModes.java`, `getBuildMode` | `@WrapMethod` on `pick`; `@ModifyReturnValue` on `getBuildMode` | While the main hand holds no item that places a block and no shape is under way, or while a box is drawn or the copy is at the aim, `getBuildMode` reads Disable during the pick, which that code skips, so the hand aims as without mods, at an entity before the block behind it. |
| E16 | `screen/RadialMenu.java`, `onClose()` | `@Inject` at head, finding the button under the pointer, and at tail, acting | Let Building Planner's buttons act when the menu closes, as the menu's own do: its `tick` calls `onClose` once its key is let go (Escape calls it too), and `onClose` acts on the menu's button under the pointer unless a click acted since the menu opened (`performedActionUsingMouse`). Building Planner's button is found as `onClose` begins, before closing grabs the mouse and moves the pointer to the window's middle, and acts after the menu has closed, like the menu's own, so "Block mix" can open its screen. |
| E18 | `screen/RadialMenu.java`, `drawTexts` | `@ModifyConstant` on the string `"Effortless Building"` | The menu's corner text, which the method draws right-aligned from the string's width, reads "Building Planner" (`buildingplanner.ui.radial.credits`). It is the only place the game shows Effortless Building's name to a player on this server: its welcome message and its build mode hint come from its server side (`config/WelcomeMessageStorage.java`, `config/BuildModeHintStorage.java`), which the server doesn't run, and its screens' titles ("Build Modes", "Modifier Settings") don't name it. |
| E19 | `screen/RadialMenu.java`, `drawRadialButtonBackgrounds` | `@ModifyExpressionValue` on `BuildModeEnum.ordinal()` | While a box is drawn or the copy is at the aim, no mode segment shows selected (the method compares each segment's index with the mode's `ordinal()`), since the player is in Building Planner's mode, whose button shows selected instead. |
| E20 | `screen/RadialMenu.java`, private field `visibility` | `@Shadow`, read by E12 | The menu's opening animation: `extractRenderState` adds 0.4 × the frame's partial tick to `visibility` each frame, up to 1, and places its ring and its buttons at 0.8 + 0.2 × `visibility` of their distance and size from the middle, while the screen darkens. E12 places and sizes Building Planner's widgets by the same factor before drawing them. |
| E21 | `buildmode/BuildModes.java`, `setBuildMode` | `@Inject` at head | Choosing a mode (the radial menu's `performAction` is its only caller outside `BuildModes`) ends a box and puts the copy away, even when the player chooses the mode they had. |
| E22 | `render/BlockPreviewRenderer.java`, `render` | `@Inject` at head, cancellable | No shape preview while a box is drawn or the copy is at the aim: the method draws the current mode's first block at the aim each frame (`BuildPipelineClient.getPreviewBlocks`). |
| E23 | `utilities/KeyBindings.java`, `<clinit>` | `@ModifyArg` on the category of each of the four `KeyMapping` constructors | Effortless Building's keys go under Building Planner's heading in Controls (see "Keys"). |

Effortless Building's limits on its shapes come from its config on the player's computer (`config/ServerConfig.java`),
which keeps its defaults, since only Effortless Building's server sends other values (`SyncServerConfigS2CPacket`): in
survival, reach 32, 2,000 blocks a shape and 64 a side; in creative, 200, 50,000 and 1,000. The preview keeps the
2,000 blocks nearest the first corner (`BuildPipelineClient.getPreviewBlocks` calls `BlockSet.sortByDistance`, then
`truncate`), so a larger shape shows as a ball around that corner, and `ConstraintSystem.processBlocks` marks the
blocks past 2,000 as rejected; each side is cut to 64 (`ThreeClicksBuildMode.findCoordinates`). Building Planner's own
box for Copy and the material list doesn't use these (see "Boxes for Copy and the material list"). The server applies
its own caps (below). The radial menu's own
buttons come from a fixed list (`RadialMenu.extractRenderState`, `buildmode/ModeOptions.java` `ActionEnum`), which is
why Building Planner adds widgets of its own rather than actions; they sit where the menu draws nothing, so its
`switchTo` and `doAction` stay empty when the player clicks one or lets the menu's key go over it.

Minecraft 26.3, read in the client jar Loom maps (`minecraft-clientonly-deobf-26.3.jar`), under `net/minecraft/client/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| M1 | `Minecraft.java`, `startAttack()` | `@Inject` at head, cancellable, returning false | While a box is drawn, the left click sets its next corner; while the copy is at the aim, it pastes the copy. Neither attacks or breaks. `handleKeybinds` calls it once for each press of the attack key. |
| M2 | `Minecraft.java`, `startUseItem()` | `@Inject` at head, cancellable | While a box is drawn or the copy is at the aim, the right click leaves that mode and uses nothing; during a zoop, after its first click, it drops the row and leaves the copy at the aim. `handleKeybinds` also calls `startUseItem` every 4 ticks while the use key stays down (`rightClickDelay`), so the use key does nothing more until it's let go. |
| M3 | `Minecraft.java`, `continueAttack(boolean)` | `@Inject` at head, cancellable | Holding the left click breaks nothing while a box is drawn or the copy is at the aim. |
| M4 | `Minecraft.java`, `pickBlockOrEntity()` | `@Inject` at head, cancellable | While the copy is at the aim, the middle click (the pick block key) mirrors the copy; while a box is drawn, it does nothing. |
| M5 | `MouseHandler.java`, `onScroll(long, double, double)` | `@WrapWithCondition` on `Inventory.setSelectedSlot`, reading the local `wheel` | While the copy is at the aim, each notch of the wheel turns the copy a quarter turn, instead of changing the hotbar slot: clockwise seen from above for a notch up, counterclockwise for a notch down, as Satisfactory's wheel turns a hologram. The method reaches that call only with no screen open and the player not a spectator. |

The hooks go by Minecraft's attack, use and pick block keys, so a player who moves one of them to another button finds
the copy's controls there too. Effortless Building's own hooks into `startUseItem`, `startAttack` and
`continueAttack` (`mixin/MixinMinecraft.java`) and its client tick ask `shouldInterceptPlacing` and
`shouldInterceptBreaking` first, which hook E14 answers false in these modes. The copy's own keys (see "Keys") need no
hook: Building Planner's client tick reads them, with no screen open, while the copy is at the aim, and empties their
clicks (`consumeClick`) otherwise, so a press made before the copy came to the aim doesn't act once it does.

Litematica, under `src/main/java/fi/dy/masa/litematica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| L1 | `util/SchematicUtils.java`: `setTargetedSchematicBlockState(BlockPos, BlockState)`, `setSchematicBlockStates`, `setAllStatesToAirExcept(SchematicPlacementManager, PlacementPart, BlockState, Level)`, `replaceAllIdenticalBlocks` | `@WrapWithCondition` on each `LitematicaBlockStateContainer.set(int, int, int, BlockState)` (the only four writes the ten Edit Schematic actions make, all called from `event/InputHandler.java`) | Skip a write into a tile's container, so Litematica's Edit Schematic actions never change the layer, even on a computer whose Litematica tool mode is still "Edit Schematic" (`InputHandler.handleUseKey` and `handleAttackKey` edit in that mode whatever the player holds). Writes to other placements, such as the copy, stay Litematica's own. |
| L4 | `schematic/placement/SchematicPlacementManager.java`, `setSelectedSchematicPlacement(SchematicPlacement)` | `@Inject` at head, cancellable | A tile or the copy is never selected: selecting one leaves the selection as it was, since selecting none would also clear Litematica's material list. Covers `addSchematicPlacement`, which selects a new placement when none is selected, `removeSchematicPlacement`, which then selects the first placement, selection by aim, and the placement list. Tiles and the copy are known as theirs before they're added. |
| L5 | `gui/widgets/WidgetListSchematicPlacements.java`, `getAllEntries()` | `@ModifyReturnValue` | Leave tiles and the copy out of Litematica's placement list. |
| L6 | `render/OverlayRenderer.java`, `updatePlacementCache()` | `@WrapOperation` on `SchematicPlacementManager.getAllSchematicsPlacements()` | Leave tiles, the copy and its zoop row out of the boxes and origin markers `renderBoxes` draws; Building Planner draws the copy's box itself, in its colours (see "Copy and paste"). This leaves Litematica's own switch for those boxes (`enablePlacementBoxesRendering`) as the player set it. |
| L7 | `render/schematic/ChunkRendererSchematicVbo.java`, `getOverlayType(BlockState, BlockState)` | `@ModifyReturnValue` | While Building Planner has a session, turn `EXTRA` into `NONE`. Litematica marks a real block as extra wherever a placement's box holds air there (`schematicOverlayTypeExtra`, on by default), and a tile's box is a whole chunk column's sections, so every real block in it would be marked. The layer holds only blocks to place, so no real block is extra. Missing and wrong blocks keep their colours. |
| L8 | `schematic/placement/PlacementManagerTaskRebuild.java`, `run()`; `PlacementManagerTask.java`'s constructors | `@WrapMethod`; a number for each task, in the order they're made (`RebuildOrder`) | Run the rebuilds of one chunk column one at a time. Litematica's background threads (at least two) rebuild a column from its placements' blocks as they are when the rebuild runs, so two rebuilds of one column at once could end with the older one's result, read before an edit's blocks were written. It also notes each finished rebuild, for the self-check ("Overlapping shapes"). |
| L9 | `schematic/placement/PlacementManagerDaemonHandler.java`, `removeRebuildTasksFor` and `removeDeferredTasksFor` | `@WrapMethod` | A running rebuild drops from the queue only the rebuilds of its column made before it began, whose edits it reads. Litematica's rebuild drops every queued rebuild of its column as it ends, and as it unloads a column with no placements, including one queued for an edit written after it read the column, which would leave that edit undrawn. Elsewhere (a new rebuild replacing the queued ones, an unload) every queued rebuild of the column still goes. |
| L10 | `gui/GuiMaterialList.java`, private `createButtons()` and `createButton(int, int, ButtonListener.Type)` | `@WrapOperation` on `addButton` in `createButton`, which adds "Save list" or "Saved lists" in place of the `EXPORT` or `EXPORT_TYPE` button, telling them by the listener's `type` (read by reflection, since `ButtonListener.Type` is private); `@WrapOperation` on `addButton` in `createButtons`, the call that adds the `ButtonListenerChangeMenu` | For Building Planner's list only (`GhostMaterialList`): no "Export" button, no export type button ("Write to file", which cycles), and no "Litematica menu" button, which `createButtons` adds at the bottom right; "Save list" and "Saved lists" go where "Export" and the export type were, in the same row, which the method moves to the bottom left when the screen is narrow (`isNarrow`). Other material lists keep their buttons. |

Public Litematica API the add-on calls: `LitematicaSchematic.createEmptySchematic`, `getSubRegionContainer`,
`getAreaSize`; `LitematicaBlockStateContainer`'s `get` and `set`; `SchematicPlacement.createFor`,
`setShouldBeSaved`, `getSchematic`, `getOrigin`, `setOrigin`, `getRotation`, `setRotation`, `getMirror`, `setMirror`;
`SchematicPlacementManager.getAllSchematicsPlacements`, `addSchematicPlacement`, `removeSchematicPlacement`,
`setSelectedSchematicPlacement`, `markChunkForRebuild`; `AreaSelection`'s constructor and `addSubRegionBox`; `Box`'s
constructor; `PositionUtils.getTransformedBlockPos`, `getMinCorner`, `getMaxCorner`;
`SchematicWorldHandler.getSchematicWorld`; `MaterialListBase` (extended, with `setMaterialListEntries`, and its
`getHudRenderer`, `getMaterialsFiltered`, `getMultiplier`, `setMultiplier`, `getSortCriteria`, `getSortInReverse`,
`toJson` and `fromJson`), `MaterialListHudRenderer` (extended), `MaterialListEntry`'s constructor and getters,
`MaterialListSorter`, `MaterialListUtils.getMaterialList` and `updateAvailableCounts`, `DataManager.setMaterialList`,
`GuiMaterialList`'s constructor and MaLiLib's `GuiBase.openGui` and `addMessage`; `BlockUtils.areStatesEqualIgnoringAge`,
`Configs.Generic.MATERIAL_LIST_IGNORE_STATE`, `Configs.Visuals.IGNORE_CROP_AGE`;
`Configs.Visuals.RENDER_BLOCKS_AS_TRANSLUCENT`, `GHOST_BLOCK_ALPHA`, `ENABLE_AREA_SELECTION_RENDERING`,
`Configs.Generic.TOOL_ITEM_ENABLED`, `Hotkeys.OPEN_GUI_MAIN_MENU` and `Configs.saveToFile`. From MaLiLib:
`RenderEventHandler.registerWorldLastRenderer` and `RenderUtils`' batched box and text-plate drawing. From Minecraft:
`KeyMapping.get` (Freecam's key by its name), `KeyMapping.isUnbound`, `KeyMapping.setKey`, `KeyMapping.resetMapping`,
`Options.save` and `InputConstants.KEY_M`, for Freecam's key and the code MaLiLib stores for Litematica's hotkey (in
Minecraft 26.3 it is 16, not GLFW's 77, which is `KEY_END`).

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
columns. An edit writes the changed blocks into the tile's container and calls `markChunkForRebuild` for the column;
hooks L8 and L9 keep that rebuild from being lost to one already running, so it draws the edit.
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

### Overlapping shapes

A shape drawn across ghost blocks already in the layer, such as a wall started on a floor, replaces them only at the
positions the two share: there the new shape's blocks stay (811 Q4 A), by rule 2 of "Order and conflicts" (see
"Payloads and order"), so a wall of stone bricks drawn across an oak floor puts stone bricks where they meet. Every
other ghost block of the older shape stays in the layer and stays drawn, for every player. The server's layer
(`LayerSync.edit`, `Layer.set`), the game's copy of it (`LayerReplica.receive`) and a tile's container
(`Tiles.columnChanged`) change only the new shape's positions.

Replacing a tile, when an edit reaches a section above or below it or empties its top or bottom section, keeps the
column's other ghost blocks drawn: the game adds the new tile before it removes the old one, so the column always has a
placement, and Litematica rebuilds it from the new tile rather than unloading it. Removing a column's last placement
makes Litematica unload the column (`SchematicPlacementManager.removeTouchedChunksFor`, `markChunkForUnload`, which
drops every queued task for it), and its background check after a schematic changes (`PlacementManagerTaskFixer`)
unloads a loaded column with no placements too.

Geo saw Building Planner 0.5.0 break this rule: most or all of an older shape's ghost blocks disappeared when a new
shape was drawn across it (811 Q1 C), with both shapes drawn in the same session, often floors about 20 blocks long,
and Geo thinks the older shape came back once the new one was removed (811 revision 2). `mod/clienttest/overlap.sh`
hasn't reproduced it: its 64 checks draw those cases with Effortless Building's own clicks, undo them, and compare
what Litematica's renderer draws with a fresh render, and they pass on 0.5.0. The layer, tile and replica code is the
same in 0.4.0, 0.4.1 and 0.5.0.

So the game checks for it itself. After it applies an edit, a column's snapshot or a tile swap, it compares each
touched column's ghost blocks outside the edit's own positions with what they were before, and the blocks Litematica
holds for the column with its own copy of the layer. On a difference it writes a warning to the game's log
(`latest.log`), at most once a minute for each column: the dimension, the column, what changed it, and the counts
before and after, with no player names. The next time it happens, Geo's log shows where and after what.

The self-check (`SelfCheck`, with the comparisons in `ColumnCheck`) reads only the columns a change touches. Before an
edit it copies the sections the edit's positions fall in, and after it compares those sections outside the edit's
positions and checks that the column's other sections are the same ones, holding as many blocks. Litematica fills its
schematic world from the tiles on its own threads, so the second comparison waits until a rebuild of the column that
began after the change queued its own rebuild has finished (hook L8 notes each one, `RebuildOrder`), or 30 seconds; it
then reads Litematica's schematic world over the tile's sections, leaving out the boxes of other placements (the
copy's, a player's own), which Litematica draws over the tile. Nothing is compared while ghost blocks are hidden or
Litematica's rendering is off, for a column whose chunk the game no longer has, or for one Litematica holds no chunk
for: a rebuild that can't handle the column yet leaves it out, and Litematica's own check around the camera
(`PlacementManagerTaskFixer`) loads it once it comes into view, so the check waits for a rebuild that leaves a chunk.
If the self-check fails with an error, it logs one warning with the error and stays off until the player joins again,
so it never stops the game. An edit of 20 blocks in a column of one section costs a copy and a comparison of 4096 positions, and one more read of
4096 once Litematica has rebuilt the column. A warning, one line in the log, reads for example:

```
Self-check: chunk column 0 0 (x 0 to 15, z 0 to 15) in minecraft:overworld, after an edit of 1 block in it: the game's
copy of the layer holds 256 ghost blocks (the column held 256 before), Litematica holds 255, and they differ at 1
position
```

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

Payloads (namespace `buildingplanner`, protocol version 5):

- Client to server: `watch(dimension, columns)`, `unwatch(dimension, columns)`, `edit(dimension, clientSeq, kind,
  changes)` where `kind` is shape or erase and `changes` is a list of world position and block state (air for an
  erase), `undo` and `redo`; and for material lists `save_list`, `delete_list`, `open_list` and `current_list` (see
  "The lists on the server").
- Server to client: `hello(protocolVersion, limits)` at join, where `limits` is the block cap for an edit;
  `columns(dimension, columns)`, each column its position and its sections, each section its height, palette and
  packed blocks; `applied(dimension, author, clientSeq, changes)`, whose author is the nil UUID for ghost blocks real blocks filled (see
  "Ghost blocks the real world fills"); `rejected(clientSeq, reason)`; `notice(message)`
  for messages players see; and for material lists `lists`, `list` and `current`.

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

The block cap is the server's config file `config/buildingplanner.properties` (`max-edit-blocks`): 200,000 blocks
an edit, the most an `edit` may carry (`SyncLimits.WIRE_MAX_EDIT_BLOCKS`), about 2.4 MB, which Fabric splits into
packets of 1 MiB (`registerLarge`). `hello` carries it, so a lower cap needs no new protocol version. The cap limits
the copy and its paste.

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

The server keeps an undo list for each player: each step is one edit (a shape, an erase or a paste), with its dimension
and each block's state before and after the edit. Undo takes the player's newest step and sets each of its blocks back
to its before-state only if the layer still holds that step's after-state there; blocks someone has changed since stay
as they are, and a `notice` says how many. The undo goes out as an ordinary `applied` edit with no `clientSeq`; an undo
that changes no block sends none, and with no steps left a `notice` says there's nothing to undo. Steps survive the
player leaving or changing dimension, keep the newest 50 per player (Effortless Building's own depth), and last until
the server restarts. Each block in a step takes about 64 bytes of the server's memory (expected, not measured), so 50
steps of 200,000 blocks would take about 640 MB of each player's share of its 6 GB. The list also drops its oldest steps
while the player's steps hold more than 2,000,000 blocks in all (about 128 MB a player), always keeping the newest. The
part of a step an undo changed goes onto the player's redo list; a redo applies it forward by the same rule, and the
player's next new edit clears the list.

### Ghost blocks the real world fills

A ghost block leaves the layer once the real block at its position fills it, so the built part of a plan stops showing
as planned, for everyone. A real block fills a ghost block when it's the same block placed the same way, whatever it
connects to (809 Q1 C): the same block, with the same value for each property a player sets by placing it (facing,
axis, top or bottom half, slab type, door hinge, bed part, attach face and rotation), and any value for the properties
the world sets (connections, stair shape, waterlogged, powered, open, lit, age, distance and the like). A shape takes
each block's connections from the real world as it was when the shape was drawn (`EffortlessShapes.place`, through
`getStateForPlacement`), so a built fence, wall or stair rarely has its ghost block's exact state. One pure function
decides it, for S1, the edits and the check at chunk load alike, with unit tests for each property.

The server learns of real block changes through one hook of its own, its first (S1): `@Inject` at the return of
`LevelChunk.setBlockState(BlockPos, BlockState, int)`, which every change to a block in a loaded chunk goes through,
acting only for the server's levels. Any change counts, whatever makes it (809 Q2 A): a player placing a block, a
command such as `/fill`, a piston, flowing water or lava, falling sand, a growing plant or an enderman. When the layer
holds a ghost block at that position (a lookup by column and section, with nothing more for a column without ghost
blocks) and the new state fills it, S1 notes the position. At the end of the server tick
(`ServerTickEvents.END_SERVER_TICK`), the server checks each noted position again and removes the ghost blocks still
filled, as one edit for each dimension, split at the block cap. The edit belongs to no player: no undo list gains a
step, and it reaches every player who watches its columns as an ordinary `applied`, with the nil UUID (all zeros) as
its author and no `clientSeq`. Games draw it without a glow: the player who placed the block sees it appear, and so
does anyone nearby. A 0.5.0 game would glow it with no name, so 0.6.0's protocol version is 5. The layer's files take
the change as they take any edit.

A ghost block, once removed, doesn't come back when the real block goes: breaking a finished wall leaves no ghost
blocks where it stood (809). Undo can bring it back only as it brings back any block someone else changed (below).

Edits never write a ghost block over a real block that fills it: before applying a shape, a paste, an undo or a redo,
the server drops each position whose real block fills the new ghost block, where the server has the chunk loaded
(`ServerChunkCache.getChunkNow`), so a paste of a copied house onto the house changes nothing. Where the server hasn't
loaded the chunk, the ghost block goes when the chunk loads.

Ghost blocks already filled, such as those built before 0.6.0, go as each chunk loads: on Fabric API's
`ServerChunkEvents.CHUNK_LOAD`, which also fires for the chunks the server loads as it starts (it reads the layer at
`SERVER_STARTING`, before any level loads), the server compares the real blocks with the ghost blocks in that chunk's
sections that hold ghost blocks, skipping a section of only air, and removes the filled ones at the end of the tick by
the same edit as S1's. Players see ghost blocks only in chunks loaded around them, which the server has loaded first, so no
player sees a filled ghost block. A section needs 4,096 comparisons, expected to take microseconds (not measured), and
a chunk without ghost blocks costs one lookup.

Undo and redo treat a filled ghost block as gone: an undo of a shape whose blocks have since been built leaves those
positions as they are, since the layer no longer holds the shape's blocks there. The undo's notice counts only the
blocks another player changed, not the ones built since: the server checks the real block at each position it leaves
and counts it only when it doesn't fill the step's ghost block. A redo skips the positions built since, like any edit.

The material list counts the same either way: a real block with no ghost block over it counts in its block's "Total"
and never in "Missing", as a filled ghost block did. A block built with other connections stops counting as "Missing"
once its ghost block goes.

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

### Settings Building Planner changes once

On the first client tick of a game run, after Litematica has loaded its config and Minecraft its options, Building
Planner reads `config/buildingplanner-client.properties` on the player's computer. Each group of settings it changes
once has a record of its own in that file. For each group whose record the file doesn't hold, it changes each
setting only while the setting still has its mod's default, saves that mod's config, and writes the record. When the
file holds a group's record, it does nothing for that group, so a player's own later change, even back to a default,
stays. A group added in a later version has a new record, so it reaches every player once, whatever earlier version
wrote the file. None needs a session, so they happen before the player joins any server. If changing or saving a group
fails, Building Planner logs a warning, writes no record for that group and tries again at the next start. A setting
the player already had at the value Building Planner would set looks the same as one it set.

| Record | Settings, with their defaults | Building Planner sets |
|---|---|---|
| `look-defaults-applied=true` | Litematica's `Visuals` options `renderBlocksAsTranslucent` (off) and `ghostBlockAlpha` (0.5), and its `Generic` option `toolItemEnabled` (on) (`Configs.java`) | on, 0.8 and off, then `Configs.saveToFile()` |
| `area-selection-off-applied=true` | Litematica's `Visuals` option `enableAreaSelectionBoxesRendering` (on) | off, then `Configs.saveToFile()` (see "Litematica's area selection") |
| `menu-key-applied=true` | Freecam's key "Config GUI" (`key.freecam.config.open`, unbound) and Litematica's hotkey `openGuiMainMenu` ("M") | M for Freecam's key, then `KeyMapping.resetMapping()` and `Options.save()`; no key for Litematica's hotkey, then `Configs.saveToFile()` (see "Keys"). Only with both Freecam and Litematica loaded. |
| `layer-keys-applied=true` | Litematica's hotkeys `layerNext` ("PAGE_UP") and `layerPrevious` ("PAGE_DOWN") | no key for each, then `Configs.saveToFile()`, so Page Up and Page Down reach the copy's nudge keys (see "Keys"). Only with Litematica loaded. |

With the tool item off, Litematica's tool (a stick by default) does nothing, since the stick and Litematica's tool modes
play no part in planning. `look-defaults-applied` is the record Building Planner 0.3.0 and 0.4.0 wrote for every
setting they changed once, so a computer that ran 0.3.0 holds it while `enableAreaSelectionBoxesRendering` is still on,
as Geo's did (784, image 1). A computer whose first version was 0.4.0 has the option off already, and gets
`area-selection-off-applied` without a change, unless its player turned the option back on, which the next version
turns off again, once.

### Show or hide

The switch is the "Show or hide ghost blocks" key (N) and a radial menu button. Hiding removes every tile
from Litematica; showing adds them back. While hidden, the game keeps watching columns and applying edits to its copy of
the layer, so showing needs no reload, and the edit glow draws nothing. A tile made while hidden is removed from
Litematica as soon as it's made: setting a placement up registers its chunks with Litematica's placement manager
(`SchematicPlacement.onModified`), which then draws it though it was never added. The state is kept in
`config/buildingplanner-client.properties` (`ghost-blocks-shown`) and starts as shown. When a shape starts (Effortless
Building's `getBuildState()` turns non-null, read once a client tick) or finishes (E1, E2), or a paste is made, while
ghost blocks are hidden, they show again first, with a message the first time in each session. Copy and the material
list leave them hidden, and leave out the ghost blocks in their box while they're hidden. Building Planner doesn't use
Litematica's own rendering switch (`enableRendering`): while that's off, each added tile would bring up Litematica's
status panel for 10 seconds (`SchematicPlacementManager.onPlacementAdded`, `StatusInfoRenderer.startOverrideDelay`), and
it hides the copy too. Litematica's own switch still turns all its drawing off.

The key and the button act only during a session; without one there are no ghost blocks to switch. Each time either
switches, it says so in chat: "Ghost blocks hidden." or "Ghost blocks shown." (`ClientSession.toggleGhosts`). The
button's icon shows the current state: a green block with a small blue eye at its lower right corner while ghost blocks
show, the same block crossed by a red slash in that corner, with no blue eye, while they're hidden. Showing
them again for a shape doesn't say "Ghost blocks shown."; the first time in a session it says "Ghost blocks shown
again. Press N to hide them.", with the key as the player has it bound (`ClientSession.showForDrawing`).

### Building Planner's mouse modes

Building Planner has two modes of its own that take over the mouse (`MouseMode`): drawing a box, for Copy, Delete or
the material list, and the copy at the aim. At most one is on at a time. Neither changes Effortless Building's mode, so
nothing needs putting back when one ends: while one is on, hook E14 keeps Effortless Building's clicks out and E15 its
longer aim, E22 hides its preview, E19 shows no mode selected in the radial menu, and hooks M1 to M5 take the clicks and
the wheel. A mode ends when the player:

- right-clicks (M2);
- chooses a mode in the radial menu (E21), even the one they had;
- shows or hides ghost blocks;
- presses the radial menu's Copy, Delete, "Material list", Paste or Block mix button, or the block mix key, each of
  which then does its own action (Copy, Delete and "Material list" start or switch a box, Paste brings the copy back);
- leaves the server or changes dimension.

Undo, redo and the speed sliders keep the mode, so a player can undo a paste and paste again, and so do the copy's own
keys (see "Keys").

Chat says nothing when a box ends before its third click: the controls line goes away and the box stops drawing.

Opening a screen (the inventory, chat, the radial menu) keeps the mode; its controls act only with no screen open,
since the hooks act only on the game's own clicks and wheel.

While a mode is on, with no screen open and the HUD shown (not hidden with F1), a line of its controls shows on screen
(`ModeHud`, drawn through `HudElementRegistry`), centred 58 above the screen's bottom, where Effortless Building draws
its own line during a shape (`RenderHandler.renderSubtitle`), in the same words for the clicks and the same colours:
white, with each action in dark aqua. The texts, as Geo approved them:

- a box for Copy: "Copy: Left-click to set the first corner, Right-click to cancel", then "the opposite corner", then
  "the height", followed by the box's size ("12 × 5 × 8");
- a box for the material list: the same, starting "Material list:".

A box for Delete has the same line as Copy's, starting "Delete:". The copy at the aim's line names its mode and its
keys, as the player has them bound. These lines are proposed, and go to Geo with the build's screenshots:

- Single, following the aim: "Left-click to paste, Scroll to turn, Middle-click to mirror, H to lock, R for Zoop,
  Right-click to close";
- locked: "Locked: Left-click to paste, Arrow keys to nudge, Page Up/Page Down to raise or lower, Scroll to turn,
  Middle-click to mirror, H to unlock, Right-click to close";
- Zoop before its first click: "Zoop: Left-click to set the start, Scroll to turn, Middle-click to mirror, H to lock,
  R for Vertical, Right-click to close", and Vertical the same, starting "Vertical:" and with "R for Single";
- Zoop or Vertical after its first click: "Zoop: Left-click to paste 4 copies, Right-click to cancel", with the row's
  count ("1 copy" for one);
- after the player's first paste in the dimension, "Hold Ctrl to line up" goes after "Middle-click to mirror";
- while the box is red, "Left-click to paste" (or the row's paste) gives way to the reason: "Can't paste: part of the
  copy is outside the world" or "Can't paste: too many blocks".

A line too wide for the screen wraps onto a second line above it, breaking only at spaces outside the box's size, which
goes after the last word that leaves it room, or alone on the line below. Effortless Building's own line during its
shapes stays as it is.

### Boxes for Copy and the material list

The radial menu's Copy, Delete and "Material list" buttons start Building Planner's box (`BoxDraw`) for that purpose;
there are no keys for them. Pressing the same button while its box is drawn ends the box; pressing another of them
switches the box to that purpose and keeps the corners set so far (752 Q4 B).

Only the left click draws the box (M1), with three clicks, as Effortless Building's "Cube" draws (`Cube.findSecondPos`,
which calls `Floor.findFloor`, and `ThreeClicksBuildMode.findHeight`), with Building Planner's own reach:

1. The first corner is the block the player aims at, real or ghost (as hook E9 aims, ghost blocks counting only while
   they show), or, when they aim at the block's top face, the space above it, so a box started on the ground holds what
   stands on it.
2. The second corner is the block where the line of sight crosses the first corner's level (`BuildModes.findYBound`).
3. The height is where the line of sight passes nearest the vertical line through the second corner.

Between clicks, the box follows the aim: its outline and see-through sides draw each frame through MaLiLib's renderer
(`RenderUtils.renderAreaOutline` and `renderAreaSides`), in white, with the corners set so far fixed. The player may
walk, or fly in creative mode, while the box is drawn, so a far corner is reached by walking toward it.

The box's limits: a corner is aimed at up to 256 blocks from the player's eyes; a box is at most 256 blocks a side, and
its height reaches the world's build limits; and every chunk the box touches must be loaded in the player's game
(held in `ClientChunkCache`; `ClientLevel.hasChunk` is always true), since the game knows the real blocks and the layer's columns only there. Where the aimed
corner would break a limit, the box draws red, the line on screen says which limit ("Too many blocks", "Part
of the box is too far away to read. Move closer."), and the click doesn't set the corner. A box never shrinks to fit a
limit.

While the box is drawn, the blocks in it show under the crosshair (`BoxList`, laid out by `BoxListLayout`): each block's
item, as Litematica's material list gives it (`MaterialCache`), with its count, most numerous first, 20 apart from 10
right of the screen's middle and 8 above it, drawn as Effortless Building draws its own list (`RenderHandler.
drawBreakingStacks`). A count shows at most three characters, so it stays within its cell: up to 999 as it is, then
rounded in thousands ("12k") and millions ("3M"). Where the next cell would pass 4 from the screen's right edge, the
list goes on in a new row 20 below, starting at the same place; the last row that clears the controls line ends with a
cell "+N" for the kinds of block left out. A box past a limit shows no list. The blocks are counted by the copy's rule
(`CopyRules`): at each position the ghost block, while ghost blocks show, and otherwise the real block, leaving out air.
The count (`BoxCount`) reads whole 16-block sections: a section the box takes in whole, with no ghost blocks, is counted
from its palette (`LevelChunkSection.getStates().count`), a section of only air (`hasOnlyAir`) is skipped, and only the
sections the box cuts or that hold ghost blocks are read position by position. It runs again when a corner moves, at
most once every 5 ticks.

The third click ends the mode and hands the box, by its lowest and highest corners, to Copy, Delete or the material
list (see "The Delete box").

### The Delete box

The radial menu's Delete button draws a box the way Copy does, then deletes every ghost block in it (808 Q1 A), so
erasing has the same limits as a box for Copy. It starts Building Planner's box for a third purpose, erasing
(`BoxDraw.Purpose.DELETE`). Everything "Boxes for Copy and the material list" and "Building Planner's mouse modes" say
of a box holds for it: a corner aimed at up to 256 blocks away, at most 256 blocks a side, the world's full height,
every chunk the box touches loaded, the three left clicks, the right click that leaves, and the switch between purposes
with the corners kept. Its controls line starts "Delete:". Its count and the list under the crosshair take only the
ghost blocks in the box (`GhostErase.counts`), since Delete leaves real blocks as they are, and the box draws red with
"Too many blocks" past the block cap (200,000; `BoxCorners.Limit.TOO_MANY_BLOCKS`). That check (`GhostErase.overCap`)
runs again whenever a corner moves, counts a section the box takes in whole from its number of ghost blocks, and stops
once the count passes the cap. Starting a Delete box, or switching a box to Delete, while ghost blocks are hidden shows
them first, as starting a shape does (see "Show or hide").

The third click sends one erase edit (`GhostErase.inBox`), air at each position in the box that holds a ghost block and
at the other half of each two-block ghost block among them, inside the box or not, so one undo brings them all back and
other players see them glow as they go. The player sees "Erased 120 ghost blocks." ("Erased 1 ghost block." for one),
counting the edit's positions, or, for a box without ghost blocks, "There are no ghost blocks in that box to erase."
and nothing changes. These texts, and the button's name and icon, go to Geo with the build's screenshots.

Effortless Building's shapes drawn with the break click keep Effortless Building's own limits (in survival, reach 32,
64 a side and 2,000 positions a shape, counted whether or not they hold ghost blocks), and erase only the ghost blocks
in the shape: hook E2 sends air only for the positions that hold a ghost block in the game's copy of the layer, and the
other halves of two-block ghost blocks, rather than every position of the shape. Raising Effortless Building's own
limits to the box's instead would freeze the game for a large shape: it lists and sorts every position of the shape
each frame while it follows the aim (`BuildPipelineClient.getPreviewBlocks`, `BlockSet.sortByDistance`), 16.7 million
for a cube 256 across (inferred from the code, not measured).

### The radial menu's buttons and sliders

Hooks E11 and E12 add Building Planner's widgets to Effortless Building's radial menu (opened by holding its key, left
Alt by default; it closes when the key is let go): two sliders, "Build speed" and "Break speed", just under the menu's
left-hand buttons (the wrench, undo and redo, and the row under them), and seven buttons: "Show or hide ghost blocks",
Copy, Delete, "Material list", Paste, "Block mix" and "Material lists". The
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

The widgets join the menu's opening animation (E20): before each frame, E12 places each widget at its distance from the
menu's middle times 0.8 + 0.2 × the menu's `visibility`, and sizes each button 20 times that factor, as the menu
places and sizes its own buttons' backgrounds, so they grow from 80% and move out with the ring over the first frames.
Icons stay 16 by 16, centred on their buttons, as the menu draws its own (`drawIcons`). The sliders move with the
factor and keep their size, as the menu's texts do.

Each slider shows its speed in blocks a second, to two decimals at most ("Build speed: 5 blocks/s", "Break speed: 3.33
blocks/s" at first), so each step reads differently. Its steps are the tick gaps 20 to 1 from left to right (1 to 20
blocks a second); a click or a drag sets it, and the handle settles on the chosen step when let go. Each step it
reaches is saved at once in `config/buildingplanner-client.properties` (`place-repeat-ticks`, `erase-repeat-ticks`; at
first 4 and 6, which a missing value, or one that isn't a whole number from 1 to 20, reads as), where hold to repeat
reads it each tick. The sliders show and save on any server, and take effect only where hold to repeat runs.

Building Planner's buttons (`RadialButton`) look and act like the menu's own: grey, light blue under the
pointer, with a 16-pixel icon centred in them, and, on hover, the button's name in aqua and its key, when it has one,
in grey. While a box is drawn for Copy, Delete or the material list, or the copy is at the aim, the button for that
mode (Copy, Delete, "Material list" or Paste) draws in the menu's blue for a selected option (`RadialMenu.selectedColor`), in place of the
mode segment E19 leaves unselected. As with Effortless Building 4.3's own buttons (`RadialMenu`), each acts when the
player lets go of the menu's key with the pointer over it (hook E16; Escape closes the menu the same way), and on a
click of any mouse button over it, which leaves the menu open; after a click has acted, letting the key go acts on
nothing. Each action plays the menu's quiet click (`RadialMenu.playRadialMenuSound`). "Show or hide ghost blocks" does
what its key does (see "Show or hide"), "Block mix" what the block mix key does (see "The block mix and its panel"),
Copy, Delete and "Material list" start a box (see "Boxes for Copy and the material list" and "The Delete box"), Paste
brings back the most recent
copy (see "Copy and paste"), and "Material lists" opens the current material list (see "The Material lists button").

The buttons sit in rows above the wrench, undo and redo, in their columns: "Show or hide ghost blocks", "Block mix"
and "Material list" 39 above the middle, Copy, Paste and "Material lists" 65 above it, and Delete 91 above it, over
Copy. They meet nothing the menu draws at 854 by 480, GUI scale 2, and at 1920 by 1080, GUI scales 2 and 4.

Each icon is a 16 by 16 image of its own in `mod/src/main/resources/assets/buildingplanner/textures/gui/radial/`, named
by what it shows: `ghost_blocks_shown`, `ghost_blocks_hidden`, `copy`, `paste` and `material_list`. Changing an icon
means replacing its file. Each draws at 16 by 16 GUI pixels with nearest-pixel filtering, as the menu draws its own
(`AllIcons.render`). Each icon is the one Geo picked from the generated candidate sets (inbox request 738), made with an
OpenRouter model, then edited by hand in Paint.NET: `ghost_blocks_shown` and `ghost_blocks_hidden` from set 14, and
`copy` from set 16, all `google/gemini-nano-banana-2.1`; `paste` from set 56a, `google/gemini-3.1-flash-image`; and
`material_list` from set 3, `google/gemini-3-pro-image`. The eye on `ghost_blocks_shown` and `ghost_blocks_hidden`, with
the slash over it, was then redrawn at 12 by 10 pixels, the size Geo picked (inbox request 768). The "Block mix" button
shows Effortless Building's own Randomizer Bag item texture (`effortlessbuilding:textures/item/randomizerbag.png`),
drawn from Effortless Building's jar, which copies none of it. The "Material lists" button shows Minecraft's Book and
Quill item texture (`minecraft:textures/item/writable_book.png`), drawn from the game's own (786 Q5 A), and the Delete
button the Barrier's (`minecraft:textures/item/barrier.png`), a red no-entry sign that reads as "remove" at 16 pixels,
unlike a tool such as the pickaxe, which would suggest breaking real blocks. Geo sees it in the build's screenshots.

### Keys

Building Planner's keys, each changeable in Controls under the heading "Building Planner": "Block mix" (B) and "Show or
hide ghost blocks" (N). Neither is a default key, or part of a default key combination, of Minecraft 26.3, Litematica,
MaLiLib, Effortless Building 4.3 or Freecam 1.5.1. Copy, Delete, the material list, Paste and the material lists have
buttons in the radial menu and no keys.

The copy at the aim is worked by the mouse and by eight keys of its own, under the same heading, which act
only while the copy is at the aim (see "Copy and paste"): "Lock or unlock the copy" (H), "Nudge the copy forward" (Up),
"Nudge the copy back" (Down), "Nudge the copy left" (Left), "Nudge the copy right" (Right), "Nudge the copy up" (Page
Up), "Nudge the copy down" (Page Down) and "Change copy mode" (R), Satisfactory's keys for the same controls. Lining
the copy up uses Minecraft's sprint key (Left Ctrl), as the copy's clicks and wheel use Minecraft's own keys, so
Controls shows no clash with sprinting. Checked against each mod's defaults:

- Minecraft 26.3 (its client's `Options`, and the Minecraft Wiki's
  [Controls](https://minecraft.wiki/w/Controls) page) binds none of H, R, the arrow keys, Page Up or Page Down; F3 with
  H shows advanced tooltips, which only matters while F3 is held. Its next version, 26.4, binds R to "Repeat
  Recipe/Trade", which acts in crafting and trading screens, where the copy's keys don't act.
- Litematica binds Page Up and Page Down to its hotkeys `layerNext` and `layerPrevious` (`config/Hotkeys.java`). With
  MaLiLib's default settings for a hotkey (`KeybindSettings.DEFAULT`, which cancels the key), Litematica uses those
  presses before Minecraft's keys see them, even though in its default layer mode, "All", the hotkeys change nothing
  (MaLiLib's `LayerRange.moveLayer`). Players can't change that layer mode, since its keys (M with Page Up or Page
  Down) and its screen are behind M, which opens Freecam's menu. Building Planner clears both hotkeys once (see
  "Settings Building Planner changes once").
- MaLiLib, Effortless Building 4.3 (left Alt, the keypad's +, Z and Y with Ctrl) and Freecam 1.5.1 (F4 to toggle, with
  its other keys unbound, `config/ModBindings.java`) bind none of them. Freecam flies the camera with Minecraft's
  movement, jump and sneak keys, and with W and the sprint key it flies faster (`FreeCamera.java`), so the arrow keys,
  Page Up and Page Down nudge the copy while Freecam flies the camera, and holding Left Ctrl to line the copy up also
  speeds the camera while W is held.

M opens Freecam's menu, as Geo wants: "M should be default bound to freecam's menu, not litematica. We shouldn't need
to be able to access Litematica's menu" (784, note c3). Freecam 1.5.1 has a key "Config GUI" under the heading
"Freecam" in Controls, with no key by default (`config/ModBindings.java`, `KEY_CONFIG_GUI`), which opens its settings
screen through the Cloth Config its jar bundles (`ClothConfigScreenProvider`). Litematica opens its menu with its own
hotkey `openGuiMainMenu`, "M" by default, which MaLiLib runs when M is let go with no other key pressed
(`KeybindSettings.RELEASE_EXCLUSIVE`) and which isn't in Minecraft's Controls. Building Planner sets both once (see
"Settings Building Planner changes once"): Freecam's key to M while it has no key, and Litematica's hotkey to none while
it's "M". A Freecam key the player bound already, and a Litematica hotkey the player moved, stay as they are. M is a
default key of no other mod in the pack. Changing Freecam's own default instead (a hook into its key's builder) would
reach no one who has played: Minecraft saves every key in `options.txt`, unbound ones too, and reads the saved key over
the default.

With M opening Freecam's screen, Litematica's other hotkeys that start with M ("M,L" for its material list, "M,C" for
its settings, "M,T", "M,R", "M,G" and the rest) can't be typed either: Freecam's screen opens on M's press, before the
second key, and those hotkeys act only with no screen open (`KeybindSettings.DEFAULT`'s `Context.INGAME`). So players
lose, with no way back in the game:

- Litematica's menu and every screen in it: its settings (the look of ghost blocks, the drawing of area selections,
  its colours, its hotkeys), loading schematics, the list of placements, area selections, the schematic verifier,
  Litematica's own material lists, and its tool mode, whose button is at the bottom of the menu (784, F11);
- Litematica's switches for all its drawing (M,R) and for schematics (M,G); N shows and hides ghost blocks instead.

None of these plays a part in planning with Building Planner, which keeps the settings it needs as it sets them.
Building Planner's material list has its own way back, in place of M,L (see "The material list"). A player who wants Litematica's menu back would edit `config/litematica.json` by hand.

Effortless Building's four keys ("Open Radial Menu", "Open Modifier Settings", "Undo" and "Redo") show under the raw
heading `key.category.effortlessbuilding.keybindings`: Minecraft 26.3 names a key category by the translation
`key.category.<namespace>.<path>` of its id (`KeyMapping.Category.label`), Effortless Building registers the id
`effortlessbuilding:keybindings` (`utilities/KeyBindings.java`), and its language file names
`key.categories.effortlessbuilding` instead. Hook E23 moves them under the "Building Planner" heading. Controls
sorts the keys of one heading by their names (`KeyMapping.compareTo`), so the heading lists "Block mix", "Change copy
mode", "Lock or unlock the copy", the six "Nudge the copy" keys (back, down, forward, left, right and up), "Open
Modifier Settings", "Open Radial Menu", "Redo", "Show or hide ghost blocks" and "Undo".

### The block mix and its panel

The mix is a list of up to nine blocks with a weight each, kept on the player's computer while the game runs: it
starts empty and off at each launch, and stays across servers. It applies while it's on, Building Planner has a
session, the main hand holds a `BlockItem`, and Effortless Building's mode isn't Disable. While it applies, hook E5
gives each shape position its block, chosen by a fixed hash of the world position over the weights, so the preview and
the layer agree, and hook E6 makes the preview draw those blocks. When it doesn't apply, shapes use the held block, as
in Effortless Building, and in Disable every click is vanilla's.

The mix key (B unless the player changes it in Controls) and the radial menu's "Block mix" button open the block mix
screen, the only way to choose the mix's blocks. It lists every block that has an item of its own, in the game's order,
with a search box (by name or id) and an "Only my inventory" switch that limits the list to the blocks in the player's
inventory. The switch starts on at each launch and keeps the player's setting while the game runs. A click on a block
in the list adds it to the mix with a share of 1, or takes it out when it's already in the mix, and blocks in the mix
show selected in the list; under each block in the mix is its share, which the player types, and its percentage;
clicking a block in the mix also takes it out. "Use mix" turns the mix on and closes the screen, with the message
"Block mix on. Press B to edit or clear it." (the key as the player has bound it); "Clear Mix" empties the mix
and turns it off at once, saying "Block mix off." when the mix was on, and leaves the screen open to pick again;
"Cancel" and Escape leave the mix as it was, or as "Clear Mix" left it. Opened while the mix is on, the screen shows the
mix as it is, to change it. The three buttons share the row's width.

While the mix applies, a panel at the right edge of the screen (`MixPanel`, drawn through `HudElementRegistry`),
centred top to bottom on a dark see-through box, shows the heading "Block mix", then each of the mix's blocks as its
icon and its percentage ("80%"), then the line "B: Edit mix", with the mix key as the player has bound it. The box is
as wide as its widest line. The whole panel, like the radial menu's
slider labels, draws at the smallest size at or above 70% of the game's at which each of the font's pixels covers a
whole number of screen pixels (`SmallText`), since at 70% they come out uneven and hard to read: ceil(GUI scale × 0.7)
/ GUI scale, which is full size at GUI scales 1 to 3, 75% at 4, 80% at 5 and 5/6 at 6. It hides while the mix doesn't
apply (`MixRule`), while a screen is open, and while
the game's HUD is hidden (F1). Without Effortless Building, whose mode the rule reads, it never shows.

### Copy and paste

The copy is a Litematica placement of a schematic kept in memory on the player's computer. Only that player sees it, and
Litematica doesn't save it, since it has no file; hook L4 keeps it from being selected, and L5 leaves it out of the
placement list, as for tiles.

When a box for Copy is drawn, Copy takes, at each position in the box, the ghost block from the player's copy of the
layer while ghost blocks show; where there is none, or while they're hidden, it takes the real block from the player's
world (the chunk sections the game holds), any block state but air, without block entity contents. So where a position
has both a ghost block and a real block, the copy takes the ghost block while ghost blocks show (`CopyRules.blocks`).
Each block keeps its world position relative to the box. The copy's box is the smallest box around the copied blocks,
and its schematic holds structure void in every position with no copied block, so the ghost blocks under its empty space
show. Copy counts the box's blocks first, with `BoxCount`, and takes them, skipping sections with none, only when the
count is neither 0 nor over the cap. When the box has no blocks to copy, the player gets a message and nothing changes. A copy of more
blocks than the server's block cap is refused with a message, since its paste would be. Otherwise the new copy replaces
any earlier one, the player gets the message "Copied 120 blocks.", and the copy shows at the
aim.

The copy at the aim is one of Building Planner's mouse modes (see "Building Planner's mouse modes"). It is placed as
Satisfactory 1.x's build gun places a building (Geo's request for 0.6.0: "mirror exactly how they work in the game
Satisfactory (especially how we can snap and nudge things)"), within what Minecraft's block grid allows: it moves a
whole block at a time and turns a quarter turn at a time. "Satisfactory's controls" below lists each of the build gun's
controls and what it is here. Geo chose each of these (inbox request 806).

**Where the copy sits.** Each client tick the copy rests against the face the player aims at, real or ghost (as hook
E9 aims: ghost blocks count only while they show, and the copy itself never does, or it would creep toward the player),
within 64 blocks: on top of a top face, under a bottom face, and against a side face, with the middle of the copy's
side that touches the face on the block beside it, as a Satisfactory hologram sits on the surface under the crosshair
(806 Q1 B), so a turn spins it in place.
For a side an even number of blocks across, the middle is the block before it in the direction of lower coordinates.
When the player aims at nothing, the copy stays where it was, and a copy made then starts over the blocks it was copied
from. Each client tick Building Planner sets the placement's origin so that the copy, as it's turned and mirrored,
sits there.

**Snapping.** Every position is on Minecraft's block grid, so the copy always lines up with blocks, as Satisfactory's
buildings line up with the grid on a foundation. Beyond the grid, the copy snaps to the player's own pastes, as a
Satisfactory foundation snaps edge to edge to the foundation it's aimed at (806 Q3 C): while the aimed block lies in the box of
one of the player's 16 most recent pastes in this dimension (`PasteBoxes`, whatever has happened to their blocks since),
the copy sits flush against the face of that box the player aims at, without overlapping it, with its edges in line
with the box's edges at the corner nearest the aim. While the player holds Minecraft's sprint key (Left Ctrl unless they
change it), which is the key of Satisfactory's "Snap To Guide Lines", the copy stays in line with the most recent paste:
on its level and on its row or its column, whichever the aim is nearer, moving only along that line as the aim moves.
The sprint key still sprints, as the copy's other controls go by the game's own keys (see "Keys"), so a player who walks
forward holding it also sprints, and in Freecam the camera flies faster. Leaving the server or changing dimension
forgets the pastes.

**Controls.** With the copy at the aim:

- A left click (M1) pastes it. Paste sends each block of the copy other than air and structure void as one shape edit,
  at the world position where Litematica draws it, with its state mirrored and turned as the copy is, so the server's
  order, the glow for other players, undo, redo and the block cap work as they do for shapes. The copy's empty space
  leaves the layer's blocks as they are. The copy stays at the aim, and stays locked if it was, for more pastes.
- Each notch of the scroll wheel (M5) gives the copy a quarter turn around its middle: clockwise, seen from above, for
  a notch up, and counterclockwise for a notch down, as Satisfactory's "Rotate Hologram Right" is the wheel up (806 Q2
  B).
- A middle click (M4) flips it left to right as the player's view faces it, around its middle: across the north-south
  line when the view faces north or south, and across the east-west line when it faces east or west. The view is the
  Freecam camera's while Freecam flies it, and the player's otherwise. Satisfactory's middle click samples the building
  aimed at instead, and its build gun has no mirror; the middle click still mirrors here (806 Q4 A), and Minecraft's
  pick block samples whenever the copy isn't at the aim.
- H ("Lock or unlock the copy") locks the copy where it is: it stops following the aim, so the player can walk around it, or fly
  the Freecam camera, to see it from every side, as Satisfactory's "Lock Hologram" does (806 Q5 A). While it's locked, the arrow
  keys move it one block: Up away from the player's view, Down toward it, and Left and Right to the view's sides, along
  the compass direction nearest the way the view faces (the Freecam camera's while Freecam flies it); Page Up moves it
  one block up and Page Down one block down. A key held down moves the copy again after 0.2 seconds, then every 4 ticks
  (5 blocks a second), the game's own pace for a held use button. Nudging has no distance limit. Pressing H again
  unlocks the copy, and it follows the aim again. Satisfactory's half step (Ctrl with a nudge key) has no equivalent,
  since the copy moves a whole block at a time.
- R ("Change copy mode") switches the copy between Single, Zoop and Vertical, in that order, as R cycles
  Satisfactory's build modes (806 Q6 A; below).
- A right click (M2), or anything else that ends the mode, puts the copy away: Building Planner removes its placement
  from Litematica and keeps the schematic, with its turn and mirror, as the most recent copy. During a zoop, after the
  first click, a right click drops the row and leaves the copy at the aim, as a right click in Satisfactory cancels the
  step it's on.

**Build modes and zoop.** The copy's mode starts as Single each time the game starts and stays as the player last set
it while the game runs, as Satisfactory keeps a build mode until R changes it. Satisfactory gives foundations these
three modes; its wheel, shown while R is held, has no equivalent here, since tapping R reaches each of the three.

- Single: each left click pastes one copy, as above.
- Zoop: the first left click fixes the first copy where it is. The row then follows the aim: copies end to end from the
  first, each the copy's own length (as it's turned) from the one before, so they touch without overlapping, along
  whichever of the four compass directions from the first copy the aim is farthest along, up to 10 copies in all, as
  Satisfactory zoops up to 10. The second left click pastes the whole row as one shape edit, which one undo takes back,
  and the copy follows the aim again for a new row.
- Vertical: as Zoop, with the row going up or down from the first copy, whichever way the aim is.

The row's other copies are placements of the same schematic, which Litematica draws as it draws the copy and Building
Planner adds and removes as the row grows and shrinks (hooks L4 and L5 treat them as the copy). While the row is
fixed, the wheel and the middle click turn and mirror every copy in it, H and the nudge keys act on nothing, and a
right click drops the row. A row whose blocks together pass the block cap draws red (below), and its click pastes
nothing.

**Colours.** The copy's box is drawn by Building Planner through MaLiLib's renderer, as the box for Copy is
(`RenderUtils.renderAreaOutline`), in Satisfactory's hologram colours (806 Q7 B): blue (#00D4FF) where a paste would go in as it
shows; yellow (#FFE100) where some of its blocks would replace ghost blocks already there, which the paste still does,
as Satisfactory's yellow "soft clearance" still builds; and red (#FF7070) where the server would refuse the paste: part
of the copy outside the world's build height or its border, or a zoop row over the block cap. While it's red, the
controls line says why and a left click pastes nothing. Hook L6 leaves the copy and its row out of Litematica's own
placement boxes, as it does tiles. The copy's blocks draw as Litematica draws them.

Turning and mirroring set the placement's rotation and mirror, which Litematica applies mirror first, then rotation;
the mirror combines its flip with the turn already set, and the origin then moves so the copy keeps its place (see
"Where the copy sits"). The radial menu's Paste button brings the most recent copy back to the aim, unlocked, as it was
turned and mirrored, in the mode the player last set; with no copy since the player joined, it gives the message
"Nothing to paste. Use Copy first.". Leaving the server or changing dimension drops the most recent copy.

#### Satisfactory's controls

What each of the build gun's controls in Satisfactory 1.x (keyboard and mouse) is for the copy at the aim, from the
official wiki's pages [Build Gun](https://satisfactory.wiki.gg/wiki/Build_Gun),
[Controls](https://satisfactory.wiki.gg/wiki/Controls), [Settings](https://satisfactory.wiki.gg/wiki/Settings) and
[Foundations](https://satisfactory.wiki.gg/wiki/Foundations), and the patch notes for
[0.8.0.0](https://satisfactory.wiki.gg/wiki/Patch_0.8.0.0), [1.1.0.0](https://satisfactory.wiki.gg/wiki/Patch_1.1.0.0),
[1.2.2.2](https://satisfactory.wiki.gg/wiki/Patch_1.2.2.2) and [1.2.4.0](https://satisfactory.wiki.gg/wiki/Patch_1.2.4.0):

| Satisfactory's control | Its key | What it is here |
|---|---|---|
| Hologram that follows the aim, up to 100 m | (aim) | The copy, resting against the aimed face, up to 64 blocks away. |
| Place | Left click | Paste. |
| Back to the build menu | Right click | Put the copy away; during a zoop, drop the row. |
| Rotate Hologram Right / Left: 90° or 45° steps; foundations 45°, and 5° on a foundation with Ctrl | Wheel up / down | A quarter turn clockwise / counterclockwise; no finer step. |
| Hologram Rotation Mode (Static by default, or facing the player) | Setting | Static only: the copy keeps its turn. |
| Snapping to foundations and walls, edge to edge, and a 1 m grid on a foundation | Automatic | The block grid always; flush to the player's recent pastes. |
| Snap To Guide Lines, and the world grid off a foundation | Hold Left Ctrl | In line with the most recent paste, with Minecraft's sprint key (Left Ctrl). |
| Lock Hologram | H | Lock the copy; H again unlocks it. |
| Nudge Forward, Backward, Left, Right: 1 m | Arrow keys | One block. |
| Nudge Up, Down (since 1.1): 1 m | Page Up, Page Down | One block. |
| Half step | Ctrl with a nudge key | None: a block is the smallest step. |
| Change Build Mode; held, a wheel of modes | R | Single, Zoop and Vertical, cycled; no wheel. |
| Zoop: first click fixes, aim to extend, second click builds, up to 10 in one line | Left click twice | The same, for copies end to end. |
| One-Click Build: hold to place a multi-step building (off by default) | Setting | None: two clicks. |
| Sample Building or Blueprint, with its settings | Middle click | Minecraft's pick block outside the mode; in the mode the middle click mirrors. |
| Build menu, Quick Switch, hotbars, Save Hologram to Hotbar, Quick Search | Q, E, 0–9, Left Alt with a number, N | None: one copy at a time; the radial menu (Left Alt) starts Copy and Paste. |
| Dismantle, mass dismantle, Dismantle Filter, Customizer | F, Ctrl, G, X | None for the copy: the break click erases with Effortless Building's shapes. |
| Hologram colours: blue, yellow (soft clearance), red (hard clearance) | Automatic | The copy's box in the same colours. |
| Toggle HUD (build mode's text) | T | Minecraft's F1, which hides the controls line. |

The wiki's pages disagree in places. Its Build Gun page and the 1.1.0.0 notes say nudging has no distance limit, and
its Controls page still says the distance is limited; the copy has no limit. The Build Gun page gives fine rotation on
foundations in 10° steps, and its Foundations and Controls pages give 5°; neither applies here. The Controls page says
pressing H again "will reset the origin", which the copy reads as unlocking. Whether a Satisfactory hologram stays
locked after it's placed isn't stated; the copy stays locked, as it stays at the aim. Since 1.2.2.2 a "Remember
Snapping Toggle" option keeps "the last used snap mode", which suggests snapping can be toggled, but the wiki doesn't
say how on a keyboard, and its "Hold To Snap Building" setting is for controllers only; the copy's guide lines follow
the key held.

### The material list

When a box for the material list is drawn, Building Planner counts the blocks in it against the player's world
(`MaterialCount`, with `BoxCount`'s reading of sections), by the rule Litematica's own list uses for a placement
(`TaskCountBlocksPlacement.countAtPosition`) for each ghost block: the block's total; missing where the world has air;
missing and the wrong block where it has another block, unless Litematica's "materialListIgnoreState" (the same block
in another state) or "ignoreCropAge" options count it as built. While ghost blocks are hidden, the box's ghost blocks
count as if there were none, and they stay hidden. The counts become a Litematica material list of Building Planner's
own (`GhostMaterialList`, a `MaterialListBase` filled through `MaterialListUtils.getMaterialList`), titled "Material
list for the blocks in the box". It becomes the current list: Building Planner sets it as Litematica's material list
(`DataManager.setMaterialList`), keeps it as "Last counted" (see "Saved lists"), and opens its screen
(`GuiMaterialList`). Counting the layer rather than Litematica's schematic world leaves out the copy, which Litematica
also draws; the copy, if it was at the aim, was put away when the box started. A box with no blocks opens the list
with nothing in it.

Litematica's Refresh button counts the list's box again, and keeps the new counts as "Last counted", while Building
Planner has a session, the player is in the box's dimension, and the game holds every chunk the box touches, as a box
needs when it's drawn, with the server's snapshot of its column (see "The layer and its column watching"). Otherwise the
counts stay as they were, and the screen says "Move closer to the box to count it again." (`GuiBase.addMessage`), since
a chunk the game doesn't hold would count as air and every ghost block in it as missing, and a column whose snapshot
hasn't arrived, as a chunk just loaded may not have, would count none of its ghost blocks. A list never reaches another
server: each server keeps its own (see "The lists on the server").

Litematica's list has four columns (`gui/widgets/WidgetMaterialListEntry.java`): "Item"; "Total", the count for the
plan; "Missing", how many are still to place, in green at 0, in gold when "Available" covers it, and in red otherwise;
and "Available", how many the player carries, with the ender chest's contents Litematica last saw when its option
"materialListCountEnderCache" is on (`MaterialListUtils.getMaterialList`). Its "Hide available" button hides the rows
whose "Available" covers "Missing".

The list counts the real blocks in the box as well, in the same list: a real block at a position with no ghost block
counts in its block's "Total" and never in "Missing", since it's already built; a real block under a ghost block counts
only through that ghost block, as built or as the wrong block. For a box with 10 ghost oak planks, 6 of them already
built as real oak planks, and 20 real stone blocks with no ghost blocks over them, for a player carrying 3 oak planks:

| Item | Total | Missing | Available |
|---|---|---|---|
| Oak Planks | 10 | 4 (red) | 3 (red) |
| Stone | 20 | 0 (green) | 0 (green) |

The screen's "Multiplier" field multiplies the counts, as for any Litematica list (`WidgetMaterialListEntry`): "Total"
times the multiplier, and "Missing" the same as that whenever the multiplier isn't 1, since Litematica then counts for
that many new copies of the build. Clicking a column's heading sorts the list by it (`MaterialListSorter`), by "Total"
at first, most first.

#### The screen's buttons

Geo kept Litematica's buttons on the screen except three (784, note c3: "We can remove the "Litematica menu", export,
and "write to file" buttons, and instead replace them with some simple custom controls to save and load material
lists"). Hook L10 takes "Export", its export type button ("Write to file") and "Litematica menu" off the screen for
Building Planner's lists, and puts "Save list" and "Saved lists" where the first two were. The screen keeps "Refresh",
"Hide available", "Info HUD", "Clear ignored", "Clear cache" and "Multiplier". Escape closes it, as it does every
Litematica screen.

#### The material list panel

Geo: "I want Info HUD to show a similar pinned visual box like the block mix box, but with the list of blocks, along
with total, available, and missing, sorted just like the full view is, with the multiplier the full view has" (784,
note c3). The screen's "Info HUD" button turns the panel on and off (`MaterialPanel`, drawn through
`HudElementRegistry`, as the mix panel is), for whichever list is current: counting a new box or opening a saved list
keeps the panel on, showing that list. Litematica's own Info HUD for the list draws nothing: `GhostMaterialList`
overrides `getHudRenderer()` with a `MaterialListHudRenderer` (`PanelSwitch`) whose switch is the panel's and whose
`render` draws nothing, so the button's label ("Info HUD: ON" or "OFF"), which reads `getShouldRenderCustom()`, shows
the panel's state. The switch belongs to the session (`ListSync`), not to a list. `DataManager.setMaterialList` turns
the old list's Info HUD off when another list becomes current, so Building Planner puts the switch back after it.

The panel draws as the mix panel does: a dark see-through box, at `SmallText`'s size, hidden while a screen is open and
while the HUD is hidden (F1). It sits in the top-left corner, 4 from each edge (786 Q6 A), where the game draws nothing
outside the debug screen (F3). It shows the heading "Material list", with the multiplier after it when it isn't 1
("Material list ×4"), then the column headings "All", "Hold" and "Need" (Geo's words, for the screen's "Total",
"Available" and "Missing"; Litematica's own list screen keeps its headings), then a row for each of the list's
rows in the screen's order, leaving out what the screen leaves out ("Hide available" and the rows the player ignored).
Each row is the block's icon and its three counts, with no name, so the panel is as narrow as the counts allow (786 Q7
A). The panel shows at most 10 rows, Litematica's own Info HUD limit (`materialListHudMaxLines`), then "+N more", and
only the rows that fit above the hotbar: "+N more" counts every row left out, those past the 10 and those that don't
fit, and its line fits above the hotbar too (a list that fits whole has no such line). The panel's bottom stays above
the strip the game keeps for the hotbar, `ContextualBar.MARGIN_BOTTOM` GUI pixels (the bars above the hotbar are laid
out from it), so it holds at every GUI scale and window size, and `MaterialPanelRows.within` takes the rows that fit
the room. At 854 by 480 and GUI scale 2, a list of 13 kinds of block shows 9 rows and "+4 more"; at 1920 by 1080 it
shows 10 and "+3 more". Each count reads as the screen shows it, with the multiplier, and "Need" in the screen's
colours: green at 0, gold when "Hold" covers it, red otherwise; the other counts are white. "Hold" counts the player's
inventory again every 2 seconds, as Litematica's own Info HUD does (`MaterialListUtils.updateAvailableCounts`). A list
with no rows to show has "No blocks to list." under the heading, with no column headings.

The rows come from a pure function (`MaterialPanelRows`) of the list's rows without the ignored ones (Litematica's
`materialListPreFiltered`), its sort, reverse, "Hide available" and multiplier, which follows Litematica's code: its
sort (`MaterialListSorter`: by the column's count without the multiplier, most first, equal counts by name; by name Z to
A, or A to Z reversed), its filter (`recreateFilteredList`: "Hide available" hides a row whose "Available" covers its
"Missing" with the multiplier) and its counts (`WidgetMaterialListEntry`). The column headings draw in grey, each
count right-aligned under its heading; each row is 18 high, as the mix panel's are.

While the panel is on and the current list is Building Planner's, Building Planner counts its box again every 2
seconds, as Refresh does when it can, so "Need" ("Missing" on the screen) drops as the group builds, on the panel and the
screen alike. The recount updates only the panel and the screen: only drawing a box and pressing Refresh replace "Last
counted" (Geo, replacing 786 Q8 A), so the recount never sends "Last counted" to the server, and a saved list opened
with the panel on leaves "Last counted" as it was. Where the box can't be counted (the player is too far from it, or
in another dimension), only "Hold" is counted again. A recount that leaves every count as it was doesn't build the
screen again, which would take the focus from its "Multiplier" field every 2 seconds. Counting reads whole sections
(`BoxCount`), so a house-sized box should take about a millisecond; a box of 256 a side full of ghost blocks could take
about half a second, a pause the player would see every 2 seconds, so a box of more than 1,000,000 blocks counts again
only on Refresh.

The panel stays on across game restarts, with the same list, until the player turns it off (786 Q9 A). The server
keeps the current list and the panel's switch with the player's lists, and sends them back when the player joins (see
"The lists on the server"), so the panel also returns on another computer.

#### Saved lists

Geo: "save and load material lists, which should be global per player and persistent across game closes. There's
also an autosave material list for the last one collected (not necessarily the last one loaded)" (784, note c3).

A saved list keeps its name, when it was saved, its box (dimension and two corners), its counts (each row's item, its
"Total", its "Missing" and how many of those are the wrong block), and the screen's multiplier, sort and "Hide
available". "Available" isn't kept: it is counted from the player's inventory whenever the list shows. The server keeps
each player's lists (786 Q1 B), as "The lists on the server" describes, so they're the same from any computer the
player joins from, on that server only, and whoever runs the server can read them. A player keeps as many lists as
they save (786 Q2 A).

"Save list" opens a small screen titled "Save list" with a name field labelled "Name", filled in to start with the date
and time ("Nov 19, 6:21 PM", the pattern `MMM d, h:mm a` kept in the language file), which the player may type over
(786 Q3 A), and "Save" and "Cancel". The field accepts at most 64 characters, and "Save" is greyed out while the name
is empty or only spaces; spaces at either end are dropped. Saving with the name of a kept list replaces it. "Saved
lists" opens a screen listing the kept lists, newest first, with "Last counted" at the top whenever the player has
one: each row shows its name, then under it when it was saved and its box's size ("Nov 19, 6:21 PM · 12 × 5 × 8"), with
"Open" and, except on "Last counted", "Delete". "Delete" asks again ("Delete the list "Planks"?", with "Delete" and
"Cancel") before it deletes, and comes back to "Saved lists" either way. With no lists, the screen says "No saved lists
yet." The mouse wheel scrolls the rows. Escape on either screen goes back to the screen it opened from. The screen
lists what the server last sent, which it sends again after each save and delete. "Open" asks the server for the list,
and when it arrives makes it current, with its counts as kept, and opens its screen, where Refresh counts its box
again when the game holds it. Changes to an opened list (Refresh, the
multiplier, the sort) aren't kept in that saved list until the player saves it again.

"Last counted" holds the latest list counted from a box: each box drawn for the material list, and each Refresh,
replaces it. Neither opening a saved list nor the panel's recount does. It can't be deleted, and "Save list" keeps a copy of it under a name.

#### The lists on the server

The server keeps each player's lists in a file of their own, `<world>/building-planner/material-lists/<UUID>.dat`,
named by the player's UUID so that a change of player name keeps them. The file is gzip-compressed NBT, as the layer's
files are:

- `version`: the format's version, 1.
- `data_version`: the game's data version when the file was written.
- `saved`: each saved list, with its `name` and `time` (when it was saved, in milliseconds since 1970).
- `last_counted`: "Last counted", when there is one, with its `time` (when it was counted).
- `current`: the list the screen and the panel show, when there is one, with its `name` when it was opened from a saved
  list.
- `panel`: 1 while the panel is on and 0 otherwise.

Each list has `dimension` (its id), `min` and `max` (the box's lowest and highest corners, each x, y and z),
`multiplier`, `sort` (the name of Litematica's `MaterialListBase.SortCriteria`: `NAME`, `COUNT_TOTAL`, `COUNT_MISSING`
or `COUNT_AVAILABLE`), `reverse` and `hide_available` (1 or 0), and `rows`, each with `item` (its id, such as
`minecraft:oak_planks`), `total`, `missing` and `wrong`.

The server reads a player's file on the server thread when they first join after it starts, and keeps their lists in
memory until it stops, so a read never meets a write still under way. It writes a player's file whole: it builds the
contents on the server thread, and one background thread writes them through a temporary file and a rename
(`LitematicFormat.writeFile`), at most every 30 seconds while that player's lists have changed, and when the server
stops, as for the layer's files. A crash loses at most the last 30 seconds of changes, as it does for ghost blocks. A
file that can't be read (cut short, not compressed NBT, another version, or a missing entry) is logged as an error and
renamed with `.unreadable` at its end, replacing an older one, and the player starts with no lists. An item the game
doesn't know drops from the list it's in.

A list travels as its dimension, its corners, its multiplier, sort, reverse and "Hide available", and its rows, each
with its item as an id in the item registry (both sides run the same vanilla items) and its three counts, with its name
and time when it has them. The game sends:

- `save_list(name, list)`: keep the list under the name, replacing a kept list of that name.
- `delete_list(name)`: delete the kept list of that name.
- `open_list(name)`: send the kept list of that name, or "Last counted" when the name is empty.
- `current_list(list, counted, panel)`: the current list, or none; whether it was just counted from a box (a new box,
  or Refresh, not the panel's recount), which makes it "Last counted" too; and whether the panel is on. The game sends it when any of
  these changes, at most once every 2 seconds, with the latest state, so a change in the last 2 seconds before the
  player leaves isn't kept.

The server gives a saved list, and "Last counted", the time by its own clock when the message arrives.

The server sends:

- `lists(lastCounted, saved)`: the index of the player's lists: "Last counted", when there is one, and the saved lists,
  newest first, each with its name, its time and its box's size. It goes at join and after each save and delete. The
  game keeps the "Last counted" entry up to date itself as it counts.
- `list(list)`: the answer to `open_list`. For a name no longer kept, the server sends `lists` again instead.
- `current(list, panel)`: at join, the current list as the player left it, or none, and the panel's switch.

At join, after `hello`, the server reads the player's file if it hasn't yet, and sends `lists` and `current` to a game
that can receive them. The game makes the `current` list current, with its counts as kept, and turns the panel on when
it was on; once the box's chunks load, the recount every 2 seconds brings the counts up to date. So the panel shows
the same list after a restart (786 Q9 A), and on another computer.

The server protects itself with these limits:

- A name is 1 to 64 characters, with no line breaks or other control characters.
- A list is at most 64 KiB as sent (`registerLarge` for `save_list` and `current_list`), with at most one row for each
  item. In each row, no count is below 0, "Missing" is at most "Total" and the wrong blocks are at most "Missing". The
  box is at most 256 blocks a side, as a drawn box is, in a dimension the server has, and the multiplier is at least 1.
- The server accepts at most 20 of a player's list messages in any 10 seconds, of all four kinds.
- The number of lists has no limit (786 Q2 A). Each list is a few kilobytes, and only the player's own saves add to
  their file.

The server drops a message that breaks a limit, changes nothing, and logs a warning with the player's name. The game
keeps to the same limits itself, so a player never meets them, and they have no text.

Each server with Building Planner keeps its own lists, so a player starts with none on another such server, and their
lists and panel come back when they return. On a server without Building Planner, or with another protocol version,
Building Planner stays off (see "When the server lacks the mod"): the panel doesn't show, and the radial menu's
buttons do nothing. A singleplayer world runs no server side, so it has no lists either.

A game on another version than the server's, such as a 0.5.0 game on the 0.6.0 server, has Building Planner off with
no message, as on a server without it: no ghost blocks, no lists, and Effortless Building's shapes draw nothing. One
protocol version gives the game and the server one version of their messages to build and check, which is the
simplest; Geo left this to the agent (788).

#### The Material lists button

Litematica's material list hotkey (M,L) reopens the current list, but M opens Freecam's menu (see "Keys"), so the
radial menu has a sixth button, "Material lists" (786 Q4 A), in the free slot over the redo button, 65 above the menu's
middle beside Copy and Paste. It opens the current list's screen, or the "Saved lists" screen when there's no current
list; like the other buttons, it acts when the menu's key is let go over it or on a click. It shows Minecraft's own
Book and Quill (786 Q5 A; see "The radial menu's buttons and sliders"). The server sends the current list at join, so
after a restart the button opens the list the player left.

### Editing from Freecam

Whenever Freecam's camera is on, the player edits ghost blocks from the camera as they would standing there, whatever
Freecam's "Allow interaction" option says: Effortless Building's shapes, their preview and erasing, hold to repeat,
Building Planner's boxes, and the copy at the aim all aim from the camera, and each block of a shape faces as it would for a player at the camera facing the same way (a stair
drawn while the camera faces north faces north). The copy's middle click mirrors it by the way the camera faces. The
radial menu, the block mix, undo and redo, show or hide, and the material list's screens and panel don't aim, and work
as without Freecam. Real blocks stay as Freecam's own options leave them: with its "Allow interaction" off, as by
default, a click with an item that doesn't place a block, or in Disable, doesn't reach the world. That option guards
real blocks, which Building Planner never touches, so ghost block editing doesn't wait on it, and players needn't find
and change it.

Freecam 1.5.1 moves the camera as an entity of its own (`FreeCamera`, a client-side player entity), which it makes
the game's camera entity (`Minecraft.setCameraEntity`), and leaves the player standing where they were. Building
Planner's aim (`Aim`) is the eye position and look direction of the camera entity (`Minecraft.getCameraEntity()`),
which is the player whenever Freecam's camera is off, so nothing changes then. While Freecam's "Player control" is on,
which moves the player and keeps the camera still, the aim is the player's; `Aim` reads that option from Freecam's
`Freecam.isPlayerControlEnabled()` by reflection, since Building Planner never compiles against Freecam, and the rule
itself is `AimSource`. `GhostAim`, `BoxDraw`, the copy's aim and mirror, and E1's block states (each placed with a
`BlockPlaceContext` for the camera entity) read it. Reaches count
from the camera: 256 for a box's corner, 64 for the copy, and Effortless Building's own for shapes. Freecam loads no
chunks, so ghost blocks, boxes and edits reach only the chunks the game has loaded around the player: a box that
touches an unloaded chunk draws red, and the server refuses a shape outside them with its usual message. The server checks no
reach, so it needs no change.

Effortless Building 4.3 aims from the player in each of these places, and hooks E24 to E26 give it the aim instead,
while Building Planner has a session and the player asked about is the one at this computer:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| E24 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `render/BlockPreviewRenderer.java`, `getPlacementState(BlockItem, Minecraft, ItemStack)` | `@WrapOperation` on `Player.getEyePosition()` and `getLookAngle()`; in `getPlacementState`, also `@ModifyArg` on the player of the `OpenBlockPlaceContext` it builds | A shape's first click, its preview, and the preview's first block aim from the camera, and that block faces as E1's would. |
| E25 | `buildpipeline/BuildPipeline.java`, `getPlayerLookVec` | `@WrapOperation` on `Player.getLookAngle()` | The modes' later clicks follow the camera's look. |
| E26 | `buildmode/buildmodes/Floor.java` `findFloor`, `Line.java`, `Wall.java`; `buildmode/ThreeClicksBuildMode.java` `findHeight` | `@ModifyExpressionValue` on the `new Vec3` each builds from the player's position and eye height | The later clicks' lines of sight start at the camera. |

Freecam's own hooks cancel `Minecraft.startAttack`, `continueAttack` and `pickBlockOrEntity` at their head while its
"Allow interaction" is off (`MinecraftMixin`), the methods where hooks M1, M3 and M4 work the box and the copy, so M1,
M3 and M4 (`MinecraftMouseModeMixin`) have priority 900, below Freecam's default 1000: a mixin of lower priority is
applied first, and its hooks at a method's head run first. M2 is in a mixin of its own (`MinecraftUseItemMixin`) at the
default, since Freecam doesn't cancel `startUseItem`. Effortless Building's shapes start from its own client tick, which
reads the keys, so Freecam's cancelling doesn't stop them.

### Litematica's area selection

Litematica keeps drawing an area selection a player made with its tool: `OverlayRenderer.renderBoxes` draws the current
selection's boxes, with see-through sides (`renderAreaSelectionBoxSides`, white at a low alpha by default), whenever
Litematica's rendering and its option "enableAreaSelectionBoxesRendering" (on by default) are on, whatever the player
holds and whether or not the tool is on. Litematica saves the selection for each server and dimension
(`DataManager.savePerDimensionData`: the simple selection, its default mode, in `area_simple`, and a normal selection by
its file) and restores it when the player comes back. A selection may also have an origin of its own, which Litematica
draws under the same option as a one-block outline in orange (`colorAreaOrigin`, #FF9010), or in cyan with orange
sides while it's selected. Since Building Planner turned Litematica's tool item off, a player who left a selection on
the server has no tool to change or clear it, and it stays drawn there.

That is what Geo saw in the walkthrough of 0.4.0 (784). Litematica's file for the server's overworld on Geo's computer
holds a simple selection of one box, 10 by 4 by 1 blocks (image 1, with its corners' red and blue outlines and its axes'
red, green and blue lines), and an origin 26 and 19 blocks from the box's corner (image 2): the one-block box's lines
in the screenshot are exactly #FF9010, with no tint inside it. Litematica's other orange outlines don't match: a
placement's box takes a colour from a fixed list in which no orange is #FF9010 (`OverlayRenderer.KELLY_COLORS`), and
Geo's file for the server lists no placements; the schematic overlay's "wrong state"
(#FF9010 too) needs a real block in the position and tints the block's sides (`schematicOverlayEnableSides`, on by
default). Geo's Litematica config still has "enableAreaSelectionBoxesRendering" on, and Building Planner's client config
holds `look-defaults-applied=true`, which 0.3.0 wrote, so 0.4.0 changed nothing (see "Settings Building Planner changes
once").

Building Planner turns off "enableAreaSelectionBoxesRendering" once on each player's computer, under its own record:
only while the option is still at Litematica's default, and never again. Every selection and its origin, on every server
and in singleplayer, then stops drawing. The selections stay in Litematica's files, unused.

### When the server lacks the mod

On joining, the client checks with `ClientPlayNetworking.canSend` that the server takes all nine of its payloads. When
it doesn't, Building Planner stays off for that server, silently: the player sees no message, and its keys, its radial
menu buttons and its sliders do nothing, and neither the mix panel nor the material list panel shows. While it's off, Effortless Building's shapes
place nothing: its place and break packets (hooks E1 and E2) and its undo and redo packets (E4) aren't sent, so a
server that runs Effortless Building without Building Planner never changes real blocks for them. The same holds from
joining until the server's `hello` arrives. A `hello` with another protocol version leaves it off the same way, so a
player whose game still has Building Planner 0.5.0 (protocol version 4) sees no ghost blocks and no lists on a server
with 0.6.0 until their next launch updates it. A
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
- The mix key opens the mix screen listing only the inventory's two blocks, with "Only my inventory" on; turned off,
  it lists every block, and stays off when the screen opens again. Clicking both, clicking one again to take it out
  and once more to add it back, typing their shares and "Use mix" turns the mix on and closes the screen, with its
  message, and the panel shows both blocks' percentages ("80%", "20%") and "B: Edit mix". F1 hides the panel and
  shows it again. The screen and the panel are shot at GUI scale 2 and, in a window of 1920 by 1080, at 4. The radial
  menu's "Block mix" button opens the same screen.
- With the mix on and a block held, each block of a 20-block shape is the mix's block at its position, on both
  clients. With the mix on and a pickaxe or an apple held, the panel hides, the place click draws no ghost block and
  places no real one, and an apple's break click breaks the real block as without mods. In Disable, with the mix on and
  a block held, the panel hides, the place click puts a real block and no ghost block, and the use click opens a door.
- The mix key while the mix is on opens the screen with the mix in it, and the panel hides while the screen is open;
  "Clear Mix" empties the mix and turns it off, with its message, and the screen stays open; "Cancel" closes it, and
  the panel stays hidden.
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
- A client without the add-on joins that server, stays connected and places a real block, and the server sends it no
  list message: the server notes each list message it sends in its debug log (`ServerSync`, at DEBUG, which the dev
  server writes to `logs/debug.log`), and the log notes them for both clients with the add-on and none for this one.
- A client with the add-on and Effortless Building but without Litematica and MaLiLib loads with Building Planner off,
  joins that server and stays connected.
- On the same server with Building Planner left out, both clients start no session and show no Building Planner
  message; the block mix key opens nothing; an Effortless Building shape adds no ghost blocks and places no real ones, with the
  player staying connected; and, by the driver's counter of the custom payloads leaving the client (a mixin in the
  `clienttest` source set), no place, break, undo or redo packet of Effortless Building is sent after a shape, Ctrl+Z,
  Ctrl+Y and the break click.
- A `hello` with protocol version 4, as Building Planner 0.5.0 sends, which the driver hands to the client, starts no
  session and shows no message.
- A client without the add-on joins the server with it, and the server sends it no list message and logs no error.

`step9.sh`, show or hide, the radial menu's buttons, the mouse modes, boxes, copy and paste and the material list.
Boxes are drawn with three real left clicks, aimed with the driver's `lookat`, while the players fly so they stay where
they're teleported; xdotool sends the wheel and the middle button (`xdo` clicks 4, 5 and 2).

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
- At 854 by 480, GUI scale 2, and at 1920 by 1080, GUI scales 2 and 4, the seven buttons meet nothing the menu draws
  in any mode (its ring, buttons, mode options, mode names, descriptions and corner text), the sliders, each other or
  the screen's edges (the driver's `radialclear`), in their rows' slots (the driver's `slots`), each with its hover
  text; each is shot, with the buttons' boxes in the log, and so is Delete's hover text. Shots taken 1 and 2 frames
  after the menu opens show the buttons nearer the middle and smaller than when it's open, as the menu's own are. With
  Effortless Building's mode Line, no mode segment shows selected while the copy is at the aim or a box is drawn, and
  Paste, Copy, "Material list" or Delete does; with the box ended, Line's segment does, and no button.
- The menu's corner text reads "Building Planner", and Controls shows Building Planner's keys and Effortless Building's
  under the "Building Planner" heading, with no raw key name; each is shot.
- The Copy button starts a box: the controls line shows "Copy: Left-click to set the first corner, Right-click to
  cancel", a right click and a use of the place click places no block, and Effortless Building's preview and mode are
  as they were. Aiming at a block's top face, the box's first corner is the space above it; aiming at its side, the
  block itself. The box follows the aim between clicks and the list under the crosshair counts its blocks. The third
  click makes the copy, with its message, places no real block, doesn't reach the other player, and leaves
  Effortless Building's mode as it was. The copy's corner sits on the aimed block and follows the aim, and Litematica
  neither selects nor lists it.
- At 854 by 480, GUI scale 2, the list counts a small box's blocks, most numerous first, and the material list box's
  controls line wraps onto two lines with the box's size whole. A box of 144 by 3 by 144 (as wide as the chunks the dev
  server's view distance of 6 loads around the player allows) over a floor of 40 kinds of block shows the list in rows
  of 10 that end inside the screen and above the controls line, with a "+11" cell; it's shot. A break shape drawn in
  Line mode over real grass has an empty break list (E8). A box past the side limit, or over a chunk not loaded, draws
  red, says which limit, and its click sets no corner.
- With the copy at the aim, the controls line shows its controls. A notch down turns it a quarter turn clockwise, a
  notch up back, and the hotbar slot stays; a middle click, facing south, flips it east to west, with its stairs
  turning with it, and picks no block.
- Over other ghost blocks, the copy's blocks show where they overlap, and the ghost blocks show in its empty space.
- A left click pastes it: the other player sees its blocks at once, turned and mirrored as the copy showed them,
  glowing with the paster's name; the copy's empty space leaves the ghost blocks under it, and the copy stays. One
  Ctrl+Z undoes the whole paste, and Ctrl+Y redoes it.
- A right click puts the copy away, and the radial menu's Paste button brings it back to the aim, turned and mirrored
  as it was. Choosing a mode in the radial menu, or the Block mix button, puts it away too.
- A box around a real house with no ghost blocks copies its real blocks. With a ghost gold block added inside the house
  and ghost stone over one of its real oak planks, the same box copies the ghost blocks and the real blocks together,
  taking the ghost stone over the oak plank; with ghost blocks hidden, it copies the house's real blocks only, and
  ghost blocks stay hidden.
- A box with no blocks in it (its first corner, a real block, taken away before the third click) gives its message; a
  copy over the block cap gives the message with the server's limit and keeps the earlier copy; Paste with no copy
  gives its message; a box of one block has its own message.
- The Copy button during its own box ends it; the "Material list" button during a Copy box switches it, keeping its
  corners, and its line starts "Material list:".
- The Delete box ("The Delete box"): Copy, Delete and "Material list" each switch a box from the others, keeping its
  corners, and Delete's line starts "Delete:", its list counting only ghost blocks; Delete during its own box ends it.
  The Delete button with ghost blocks hidden shows them and starts its box. A Delete box of 40 by 40 by 10 over 16,000
  ghost oak planks, with a ghost door in its top layer whose upper half stands above the box and a real stone among the
  planks, lists the planks and the door and not the stone, and is shot at 854 by 480 and 1920 by 1080; its third click
  says "Erased 16001 ghost blocks.", sends one edit, takes the door's upper half too and leaves the real stone, and the
  other player sees them go, glowing with Planner1's name. One Ctrl+Z brings them all back. A Delete box over grass
  without ghost blocks gives its message and sends no edit. A Delete box over 200,001 ghost blocks draws red with "Too
  many blocks", and its click sets no corner and sends no edit.
- A box for the material list around 10 ghost oak planks, 6 of them built as real oak planks, and 20 real stone
  blocks, with ghost blocks shown and a copy at the aim, opens Litematica's material list as "The material list"
  describes, places and erases nothing, and puts the copy away. With ghost blocks hidden, the same box lists only the
  planks and stone that are built, and ghost blocks stay hidden. Litematica's list and the button's hover text are shot
  at 1920 by 1080, GUI scales 2 and 4. A box with no blocks opens an empty list.
- The material list's screen shows "Refresh", "Hide available", "Info HUD", "Clear ignored", "Clear cache", "Save
  list", "Saved lists" and "Multiplier", and no "Export", "Write to file" or "Litematica menu" (the driver's
  `ListScreenProbe` reads the buttons' labels), at 854 by 480 and 1920 by 1080, GUI scale 2, each shot.
- "Info HUD" shows the panel once the screen closes, with the list's rows in the screen's order and their counts, and
  the label reads "Info HUD: ON"; with the multiplier at 4 the heading and every count change as the screen's do;
  sorting by "Missing" on the screen reorders the panel; "Hide available" leaves out the rows it hides on the screen,
  and turned off brings them back; picking up 3 oak planks raises "Hold" within 2 seconds. A second box keeps the
  panel on with the new list. F1 hides it and shows it again. "Info HUD" turns it off.
- The panel sits 4 from the screen's top and left edges, and each row shows the block's icon and its three counts.
  With the panel on and its box loaded, placing 2 of the missing oak planks as real blocks lowers "Need" ("Missing" on
  the screen) by 2 within 2 seconds, on the panel and the screen. Taking them away lowers "All" by 2, since building
  them removed their ghost blocks, and drawing the ghost blocks again raises "All" and "Need" back.
- The panel's headings read "All", "Hold" and "Need". A list of 13 kinds of block, one all built, one covered by what
  Planner1 carries and the rest missing, shows its first rows in green, gold and red. With the mix panel on too, at 1920
  by 1080 (GUI scales 2 and 4) it shows 10 rows, then "+3 more". At 854 by 480 and GUI scale 2 it shows 9 rows, then
  "+4 more", with the panel's bottom above the hotbar's top. It's shot at those three sizes, and at 854 by 480 with the
  multiplier at 2 ("Material list ×2"); a box with no blocks shows "No blocks to list.", which is shot too.
- "Save list" with the name "Planks" keeps the list; "Saved lists" shows "Last counted" first, then "Planks", with
  their sizes. A new box replaces "Last counted" and leaves "Planks"; opening "Planks" doesn't change "Last counted".
  "Delete" asks again, then removes it. A name of 65 characters can't be typed, and "Save" is greyed out with an empty
  name.
- A box Planner1 counts is in `run/server/world/building-planner/material-lists/<Planner1's UUID>.dat` as
  `last_counted` within 30 seconds, and after the server and Planner1's game restart, Planner1's game has it as the
  current list (read by a driver probe).
- Planner1's lists are in `run/server/world/building-planner/material-lists/<Planner1's UUID>.dat` within 30 seconds
  of a save, and no file in Planner1's game folder contains them. After Planner1's game restarts, and again after the
  server restarts, the panel, on with "Planks" before, shows the same list with the same multiplier, with "Hold"
  counted again; "Saved lists" shows the same lists, with "Last counted" still the row's box, and "Open" on "Planks"
  shows its counts as kept; each is shot.
- With the panel on, opening a saved list and waiting 6 seconds, which is three recounts, leaves `last_counted` in the
  player's file as it was; drawing a box and pressing Refresh still replace it.
- Planner1 playing from client 2's game folder sees the same lists and the panel; Planner2 sees none of Planner1's
  lists, and no panel.
- With Planner1's lists file cut short and the server restarted, Planner1's join logs an error, the file is renamed
  with `.unreadable`, Planner1 has no lists, and a new save writes a new file.
- A `save_list` with a name of 65 characters, one with a row whose "Missing" is over its "Total", and the 21st list
  message in 10 seconds, each sent by the driver, change nothing on the server and log a warning, and Planner1 stays
  connected.
- Refresh on a list whose box is in chunks the game doesn't hold (Planner1 teleported 300 blocks away) keeps the counts
  and shows its message; back at the box, Refresh counts again and replaces "Last counted".
- The radial menu's "Material lists" button opens the current list's screen, also after Planner1's game restarts, and
  for Planner2, with no current list on the server, the "Saved lists" screen.
- A simple area selection with an origin, which Litematica saved for the server before the session, with its drawing
  of selections on and Building Planner's client config holding `look-defaults-applied=true` without
  `area-selection-off-applied`, as 0.3.0 and 0.4.0 leave it on a computer that ran 0.3.0, shows as a large box and an
  orange one-block box in the view, which is shot. After the next start, the option is off,
  `area-selection-off-applied=true` is written, the selection and its origin are still in Litematica's data, and
  neither shows in the same view.
- On the same server with Building Planner left out, N makes no message and leaves ghost blocks shown, the mouse
  works as without the add-on, and the material list panel doesn't show; with Building Planner back, the panel shows
  again with the same list.

`step10.sh`, hold to repeat, editing from Freecam and the settings Building Planner changes once, with Freecam 1.5.1 (Modrinth version
`lwrkvqym`, the pack's) on both clients. The driver counts the ticks each mouse button was down and
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
  holding either sends its first repeat 4 ticks after the button goes down and then one every tick. The rows held over
  with attack after the first are four ghost blocks high, since a break that meets no ghost block sends no edit.
- The radial menu's wrench button opens Effortless Building's Modifiers screen.
- Holding use in Line mode (which needs two clicks) and with the mode disabled sends no edit.
- Editing from Freecam: the driver switches Freecam's camera on (`Freecam.toggle`, as its key does), puts its eyes 10
  blocks north of Planner1, who faces east looking up at the sky, and turns it as a mouse move would (`camlookat`);
  Freecam's "Allow interaction" is off, as by default. A Single shape goes where the camera aims, on both clients, with
  no real block; a Line shape goes from the camera's first click to its second; a stair drawn with the camera facing
  north faces north; the break click erases the ghost block the camera aims at and leaves the real block under it. A
  box for Copy starts at the camera's aim, follows it after the first and second left clicks, and the third copies its
  ghost blocks; the copy goes to the camera's aim and follows it, and a middle click mirrors it by the camera's facing
  (`FRONT_BACK`, where Planner1's east would give `LEFT_RIGHT`). The radial menu opens and its show or hide button
  hides ghost blocks, N shows them again, a Line shape with the block mix on takes the mix's blocks, and Ctrl+Z and
  Ctrl+Y undo and redo it. A left click with a stick aimed at a real stone breaks nothing. A shape's preview, a box and
  the copy are shot from the camera at 854 by 480 and 1920 by 1080, GUI scale 2.
- Each client's first start leaves Litematica's options at on and 0.8 (and the tool item and the drawing of area
  selections off), Freecam's "Config GUI" key at M, Litematica's `openGuiMainMenu`, `layerNext` and `layerPrevious`
  with no key, saved in Litematica's config and Minecraft's `options.txt`, and the four records in Building Planner's
  client config.
- A player's own change (off and 0.3) survives a restart. With the records gone and the server stopped, so the client
  joins nothing, a start sets only the option still at its default (on) and keeps the 0.3. With only
  `area-selection-off-applied` gone and the drawing of area selections turned back on, a start turns it off and
  changes nothing else. With only `look-defaults-applied` gone and the look at Litematica's defaults, a start sets the
  look and leaves the drawing of area selections the player turned on; with every record left, nothing changes.
- With Freecam's key bound to K by the player, Litematica's hotkey "M" and the `menu-key-applied` record gone, a start
  leaves it at K and still takes M from Litematica's menu, and writes the record.
- With `layerNext` moved to K by the player, `layerPrevious` at Page Down again and the `layer-keys-applied` record
  gone, a start leaves `layerNext` at K, takes Page Down from `layerPrevious`, and writes the record; with every
  record left, hotkeys back on Page Up and Page Down stay through a restart.
- A Page Up press, a real key press with no screen open, reaches a key mapping of the driver's bound to Page Up once
  both layer hotkeys have no key. As controls, it doesn't while `layerNext` is on Page Up again (the driver refreshes
  MaLiLib's table of used keys after setting it, as its config screen does), and does once the hotkey has no key again.
- In the game, M opens Freecam's settings screen (the driver's `screenclass` reads the open screen's class, Cloth
  Config's) and no Litematica screen; M held with L pressed opens no Litematica screen either. Escape closes Freecam's
  screen. Both screens are shot.

`overlap.sh`, overlapping shapes and the self-check ("Overlapping shapes"). "Drawn" means Litematica's schematic world
holds the block (the driver's `countbox`), and some checks also compare what Litematica's renderer last built of each
column with a fresh render (`rendered` and `rerender`):

- A wall from a floor into the section above, a second floor crossing the first and a wall in the floor's section,
  each sent as an edit, leave the floor's other ghost blocks in both clients' copies and drawn, at once, a second later
  and five seconds later; the wall's columns get taller tiles. The floor is all there after moving away and back, and
  after a rejoin.
- Effortless Building's Floor and Wall modes, with real clicks, draw a floor and a wall across it, and the floor's other
  ghost blocks stay.
- A floor over 64 columns stays drawn through five rounds of a second floor in the section above and its erasing,
  each of which replaces every column's tile.
- Floors of 20 blocks, as Geo drew them: a floor across, a floor on top, a line and a wall, each drawn across floor A
  with real clicks, leave A's other blocks at once and five seconds later, Litematica draws each column as a fresh
  render does, and Ctrl+Z brings all of A back; a floor erased with the break click and lines of 18 kinds of block
  written into the tiles in place leave A's other blocks too.
- Neither client's log has a self-check warning, checked before client 1 restarts and at the end.
- With one block taken out of client 1's tile alone (the driver's `unhold`) and a block put elsewhere in the column,
  client 1's log has the warning, with the column, the edit and the counts (256 ghost blocks, Litematica 255, 1
  position), and client 2's has none.

The self-check's unit tests (`ColumnCheckTest`) cover the comparisons as pure functions: an edit's own positions, set
or erased, are ignored; a block lost or changed outside them, in the edit's section or another, is reported; Litematica
missing a block or holding nothing is reported, and other placements' boxes are left out; and a column gets at most one
warning a minute, with each of 300 columns keeping its own minute.

`filled.sh`, ghost blocks the real world fills ("Ghost blocks the real world fills"), more than 16 blocks from the
world's spawn, where Planner2, who isn't an operator, may build:

- Real blocks placed by hand, by Planner1 and by Planner2 in Effortless Building's Disable mode, by `/fill`, by a piston
  and by water flowing into ghost water remove their ghost blocks: a tick after the real block shows, first on the
  client that learns of it from the server, then on the other, the ghost block is gone, and neither client shows a
  glow. The ghost blocks beside them stay.
- A stair placed facing the other way leaves its ghost block; one facing the same way, waterlogged, fills it.
- Breaking filled blocks brings no ghost block back.
- A paste onto built blocks (the driver's `pastehere`, which copies a box and pastes it where it was copied from)
  changes nothing, with no glow.
- Ghost blocks saved over matching real blocks while the server ran without the add-on are gone once their chunk
  loads, and a ghost stair under a real stair facing the other way stays.
- Neither client's log has a self-check warning, checked before the restarts and at the end.
- Planner2 is shot at 854 by 480 and 1920 by 1080, GUI scale 2, looking at a ghost wall before and after real blocks
  fill half of it.

The fill rule's unit tests (`FillRuleTest`) cover each property it compares or ignores: a stair facing another way or
upside down, a log on another axis, a door with the other hinge or its other half, a bed's other part, a button on
another face, a sign turned another way and a hopper, piston, dripstone tip and crafter facing another way stay; a
stair of another shape or waterlogged, a fence or wall with other connections, a waterlogged slab, a crop of another
age, an open or powered door, an occupied bed, a lit furnace and an extended piston fill it; a top or double slab
doesn't fill a bottom one; air fills nothing. `LayerFillTest` covers the end-of-tick edit (by the nil author with no
client sequence, to every watcher, only for blocks still filled when the tick ends, split at the block cap), an edit's
filter (positions a real block fills are dropped, unless the server hasn't loaded their chunk; a paste onto built blocks
changes nothing and leaves no undo step), undo and redo skipping positions built since, the undo notice counting only
blocks another player changed, and the check at chunk load reading only the sections that hold ghost blocks.

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
- How long counting a box takes: about 30 nanoseconds a position read one by one (expected, not measured), so a box of
  256 by 256 by 256 read whole would take about half a second; `BoxCount` reads only sections the box cuts or that hold
  ghost blocks position by position.
- Editing from Freecam (see "Editing from Freecam"): that hooks M1, M3 and M4 run before Freecam's cancelling hooks at
  the head of the same methods once given a priority, that Effortless Building's clicks still start shapes with
  Freecam on, and that a stair placed from the camera faces as the camera does.
- The cause of an older shape's ghost blocks disappearing under a new shape in 0.5.0, which Geo saw (811 Q1 C), and
  whether the older shape came back once the new one was removed (see "Overlapping shapes").
- That hook S1 sees every real block change the server makes in a loaded chunk, and how long its lookups and the
  check at chunk load take on the real server (expected microseconds a section, not measured).
- That the copy's keys reach Building Planner while Freecam's camera is on: Freecam 1.5.1 wraps `KeyMapping.set` and
  `setAll` (its `KeyMappingMixin`, seen in its jar, its source not read) to hold key states for its camera.
- Satisfactory's controls as the wiki describes them, not played: the wiki's pages leave out whether a locked hologram
  stays locked after it's placed, and how its snapping toggle works on a keyboard (see "Satisfactory's controls").
- That no MaLiLib or Litematica hotkey bound to the scroll wheel or the middle button acts while a mouse mode takes
  them; the defaults bind none.
- The real server's view distance, which sets how far a box may reach in loaded chunks; the dev server's is 6.
- That the orange one-block box in Geo's screenshot (784, image 2) is the selection's origin: its colour and Geo's
  Litematica file say so, and Geo seeing it gone after the next version settles it.
- How long counting a list's box again every 2 seconds takes in the game, and whether a player notices it; the 1,000,000
  block limit on it is expected, not measured.
- That Freecam's settings screen opens on M's press before MaLiLib reads a second key, so Litematica's M hotkeys can't
  be typed; `step10.sh` checks M then L.
- That 64 KiB fits a list with a row for every item in Minecraft 26.3: a row is at most 15 bytes as sent, so 64 KiB
  fits about 4,300 rows, and the item count is expected, not counted, to be well under that.
- How long the server takes to read a player's lists file at join, on the server thread: expected to be a few
  milliseconds for a few hundred lists, not measured.
- A real 0.5.0 game on a 0.6.0 server: `step8.sh` hands the client a made-up `hello` of protocol version 4.
