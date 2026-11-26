# Building Planner

Building Planner is a Fabric add-on for Minecraft 26.3. With it, the group plans builds together as one shared layer of
ghost blocks that everyone on the server sees and edits live, then builds them by hand. Players draw with Effortless
Building; Litematica draws the ghost blocks, out of sight. It hooks into unchanged copies of both. Geo's decisions
behind it are in [plan.md](plan.md) ("Decisions").

**What's built.** The server and the player pack run Building Planner 0.7.0, which has everything this spec describes
except 0.8.0's changes, from Geo's trial of 0.7.0 (1359, comments c1 to c7; "Building Planner 0.8.0" in
[plan.md](plan.md) "Decisions"):

- Litematica's status panel ("Layer Mode: All", "Renderers: …") kept off players' screens (see "Litematica's status
  panel");
- every ghost block within 32 blocks shown through walls, light blue where its block is missing (see "Ghost blocks
  through walls");
- a lock for each step of a shape: H locks the step the aim is setting, and the nudge keys then move that step's
  position, or the whole shape along a direction the step doesn't use (see "Locking and the first click");
- before the first click, the block a left click would start from framed in red beside the block a right click would
  place (see "Locking and the first click": "The first click's blocks");
- "Build speed" and "Break speed" as sections on the radial menu's right side, shown only in Single (see "The radial
  menu's buttons and sliders");
- "Visual Settings" renamed "Settings" (see "Settings");
- Delete's controls line saying "toggle mark" (see "Delete").

0.8.0 keeps protocol version 5: it changes nothing the server does, so a 0.7.0 game and a 0.8.0 server work together.
The build steps are in [plan.md](plan.md) ("Agent-ready work"). `OPEN:` marks a choice still Geo's, with the inbox
request that asks it.

**Proposed for 0.9.0.** The setting "Place real blocks", with which the server places the real blocks of a shape or a
paste (Geo's comment c4 on 1359, 1368 Q2 A, 1372), is designed in "Place real blocks" and not built; every part of
this spec that mentions it is marked as proposed for 0.9.0. Its build steps follow 0.8.0's in [plan.md](plan.md).

**Proposed for 0.10.0.** Building Planner on servers that don't run it, where the player's ghost blocks are saved on
their computer and only they see them (Geo's comment c4 on 1359, 1368 Q1 A), and in the player's own worlds, where
they're shared with the world's LAN guests (1374 Q4 C), is designed in "Servers without Building Planner" and not
built; every part of this spec that mentions it is marked as proposed for 0.10.0. Its build steps
follow 0.9.0's in [plan.md](plan.md).

**Proposed for 0.11.0.** Saved copies, which keep each player's recent and renamed copies on their computer and open
with a key the copy's controls line shows (Geo's comment on 1375 Q1), are designed in "Saved copies" and not built;
every part of this spec that mentions them is marked as proposed for 0.11.0. Their build steps follow 0.10.0's in
[plan.md](plan.md).

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
  placed them. Effortless Building never places or breaks real blocks, except that with the setting "Place real
  blocks" on (proposed for 0.9.0), the place click's shape places real blocks instead (see "Place real blocks"). A
  shape can start on a ghost block as well as on a real block. Ghost blocks are drawn and erased only with an item
  that places a block in hand: a block, or the likes of redstone dust, fences, doors, signs, seeds and string; a door
  or a bed gets both its halves. With anything else in hand (a tool, food, an empty hand, or an item that places an
  entity or a fluid, such as a bucket, an item frame, an armor stand or a boat), every click acts as without mods, in
  every mode, breaking and using real blocks, and holding a button repeats as the game does.
- **Locking and the first click.** In every build mode but Disable, with a block in hand, the block a right click
  would place shows at the aim before the first click, and the block a left click would start erasing from shows
  framed in red (in Single, only on a ghost block). H locks the position the aim is setting: the
  first click's before it, and during a shape the step's under way, such as a wall's far corner. The arrow keys, Page
  Up and Page Down then move that position, or move the whole shape along a direction that step doesn't use, such as
  across a wall; each click sets its step's position, the next step follows the aim until H locks it, and the lock ends
  once the shape is placed. A line of each mode's controls shows at the bottom of the screen, before the first click
  too (see "Locking and the first click").
- **Turning and flipping.** In every mode, with a block in hand, while Left Ctrl is held, the wheel turns the block
  and the middle click flips it to the other ways it can sit (a log's axis, a stair's top or bottom half). Every shape
  and its preview take that turn, in each mode but Disable, until the player switches to another block. In Disable,
  holding Left Ctrl shows the block where a right click would place it, turned, and a right click with Left Ctrl held
  places the real block that way (see "Turning real blocks in Disable"). These controls show at the bottom of the
  screen, in each mode's line (see "Locking and the first click").
- **Hold to repeat.** In Single mode, holding the place button keeps placing ghost blocks and holding the break button
  keeps erasing them. Two sliders on the right side of Effortless Building's radial menu, under the headings "Build
  speed" and "Break speed", shown while Single is the mode, set how fast, from 1 to 20 blocks a second; at first they're the game's own pace for a held button, five a second to place and
  just over three to erase. The first repeat waits at least 0.2 seconds, the game's own wait before a held click
  repeats, so a quick click places or erases one block at every speed. The other modes act once for each click, and so
  does a click in Single mode that uses a locked position (see "Locking and the first click").
- **Disable.** With "Disable" chosen in Effortless Building's radial menu, clicks act as they do without mods, placing
  and breaking real blocks, even with the block mix on, except that a right click with Left Ctrl held places the block
  turned (see "Turning and flipping"), and, with the setting "Match ghost blocks" on, a block placed into a ghost block
  of the same block takes the ghost block's facing (see "Settings").
- **Esc.** With no screen open, Esc steps out of one mode a press at a time before it pauses the game: a copy's row,
  then the copy, a box, Delete's marks, then Delete, a shape under way, then the build mode, back to "Disable" (see
  "Esc").
- **The build mode's name.** While a build mode other than Disable is chosen, its name shows just left of the hotbar;
  while a box for Copy or the material list is drawn, the copy is at the aim, or Delete is on, the tool's name ("Copy",
  "Material list", "Paste", "Delete") shows there in its place, in Disable too.
  The held item's name, which the game shows for a moment after the player picks an item, shows above the controls
  lines, where the build mode's name used to show (see "The build mode's name and the held item's name").
- **Ghost blocks through walls.** Every ghost block within 32 blocks shows through walls in the colour Litematica
  draws it: light blue where its block is missing, red where a different real block is in it, and orange where the
  same block is in it in another state (see "Ghost blocks through walls").
- **Block mixes.** The block mix key (B unless the player changes it in Controls) and the Block mix button in the
  radial menu open the block mix screen, the only place to choose a mix's blocks: it lists the blocks in the player's
  inventory at first ("Only my inventory" starts on), or every block in the game with that switch off, with search. A
  click on a block adds it to the mix, and a second click takes it out. The player picks up to nine blocks, each with
  a share (say 80 cobblestone to 20 mossy cobblestone), and turns the mix on. The screen closes, and the mix then
  applies to Effortless Building's shapes, in every mode but Disable, while the player holds an item that places a
  block; with anything else in hand, clicks act as without mods. While the mix applies, a small panel at the right edge
  of the screen shows each block's icon and share, and "B: Edit mix" under them. "Clear Mix" empties the mix and turns
  it off. A mix of one block lets a survival player plan with a block they don't have.
- **Undo and redo.** Undo takes back only that player's own changes to ghost blocks, newest first: a whole shape or
  paste at a time, in any dimension. A block someone else has changed since stays as they left it. Real blocks placed
  with the setting "Place real blocks" (proposed for 0.9.0) aren't undone: they come out by hand.
  Effortless Building's Ctrl+Z (and the undo button in its radial menu) undoes, and its Ctrl+Y (and the redo button)
  redoes the newest undone change, until the player's next new change.
- **Others' edits.** When someone else changes ghost blocks, the changed blocks glow for a few seconds, with the
  editor's name beside them.
- **Built ghost blocks go.** Once a real block fills a ghost block (the same block, placed the same way), the ghost
  block leaves the layer for everyone, however the real block got there, including ghost blocks built before 0.6.0.
  Breaking the real block later doesn't bring the ghost block back (see "Ghost blocks the real world fills").
- **Delete.** The radial menu's Delete button starts Delete, as in Satisfactory's dismantle: holding Left Ctrl and
  sweeping the aim marks each ghost block the crosshair passes over ("toggle mark" in the controls line), and holding Left Ctrl while pressing the left
  button on one block and letting it go on another marks every ghost block in the box between the two. A sweep or a box
  that starts on a marked ghost block unmarks instead. Holding the left button alone for a second erases the marked
  ghost blocks as one change, which one undo takes back, with a progress bar in the middle of the screen (see
  "Delete").
- **Overlapping shapes.** A shape drawn across older ghost blocks replaces them only where the two meet; the older
  shape's other ghost blocks stay (see "Overlapping shapes").
- **Freecam.** With Freecam's camera on, shapes, erasing, boxes and the copy aim from the camera, as if the player
  stood there (see "Editing from Freecam").
- **Boxes.** Copy and the material list each start Building Planner's own box mode, which only the left click works:
  three left clicks set a corner, the opposite corner on the same level, and the height, and a right click or Esc
  leaves the mode. The first corner is the space above the aimed block when the player aims at its top, and the aimed
  block otherwise, whose top face then shows shaded. The box is white for Copy and blue for the material list. The
  screen shows what the box is for, the click that comes next, the box's size, and the blocks in it on a dark
  background, on as many lines as they need. Pressing the other of Copy and "Material list" while a box is drawn
  switches the box to that purpose. A box may be up to 256 blocks a side, and it never shrinks to fit a limit (see
  "Boxes for Copy and the material list").
- **Copy and paste.** The player presses the Copy button in the radial menu and draws a box: the blocks inside, ghost
  blocks and real blocks alike, become a copy, which only that player sees; where a ghost block stands in place of a
  real block, the copy takes the ghost block. While ghost blocks are hidden, the copy takes only real blocks. The copy
  then shows where the player aims, resting on the aimed face with its middle under the crosshair, and its controls,
  those of Satisfactory's build gun, show on screen: a right click pastes it into the layer as one change, which one
  undo takes back, and the copy stays for more pastes; the scroll wheel turns it; a middle click mirrors it; H locks it
  in place, and the arrow keys, Page Up and Page Down then nudge it a block at a time; R switches its mode between Off,
  Line, Wall and Floor, which lay copies side by side as Effortless Building's Line, Wall and Floor lay blocks, with two
  right clicks; the copy snaps flush to the player's recent pastes, and holding Left Ctrl keeps it in line with the
  latest; its box shows blue, yellow where it would replace ghost blocks, and red where it can't be pasted; Esc and a
  left click put it away. These take the place of what the clicks and
  the wheel do otherwise, such as changing the hotbar slot (see "Copy and paste"). The empty space in the copy leaves
  the ghost blocks under it as they are. Doing anything else in Building
  Planner or Effortless Building, such as choosing "Wall", pressing "Material list", or showing or hiding ghost
  blocks, also puts the copy away; undo, redo and the speed sliders keep it at the aim. The radial menu's Paste button
  brings back the most recent copy at the aim. With the setting "Place real blocks" on (proposed for 0.9.0), a right
  click pastes real blocks from the player's inventory instead, by the same rules as a shape (see "Place real
  blocks").
- **Saved copies.** As proposed for 0.11.0, each copy is saved on the player's computer as Copy makes it. V, which the
  copy's controls line shows, opens "Saved copies", listing the 10 most recent copies and every copy the player renamed,
  each with "Paste", "Rename" and "Delete"; a renamed copy stays until the player deletes it, and the copies are the
  same on every server (see "Saved copies").
- **Material list.** The player presses the "Material list" button in the radial menu and draws a box: Litematica's
  material list screen opens for the blocks inside, ghost blocks and real blocks alike, with how many of each block the
  ghost blocks still need. While ghost blocks are hidden, it lists only the real blocks and leaves the ghost blocks
  hidden. A box with no blocks opens an empty list. The screen's "Info HUD" button pins the list to the screen as a
  panel like the block mix's, with each block's "All", "Hold" and "Need", in the screen's order and with its
  multiplier. "Save list" keeps the list under a name, and "Saved lists" opens a kept list again. The latest list
  counted from a box is always kept too, as "Last counted". The server keeps each player's lists, so they're the same
  from any computer, on that server only. The panel stays on across restarts, with the same list, until the player
  turns it off. The radial menu's "Material lists" button opens the current list. Each of the list's screens has a
  "Close" button at its bottom right, and its (i) icon explains Building Planner's list (see "The material list").
- **Settings.** Effortless Building's "Visual Settings" screen, and the radial menu's button that opens it, are named
  "Settings". The setting "Match ghost blocks" in it, off at first, places a real block turned as the ghost block it
  fills (see "Settings"). The setting "Place real blocks", proposed for 0.9.0 and off at first, makes the server
  place the real blocks of a shape or a paste instead of ghost blocks (see "Place real blocks").
- **Building Planner's name.** Where players see Effortless Building's name in the game, the corner of the radial
  menu, it reads "Building Planner". Effortless Building's four keys in Controls show under the "Building Planner"
  heading, with Building Planner's own.
- **Litematica's area selection.** On each player's computer, once, Building Planner turns off Litematica's drawing of
  area selections, so a selection a player left in Litematica, with its corners and its origin, doesn't show (see
  "Litematica's area selection").
- **Litematica's status panel.** On each player's computer, once, Building Planner turns off Litematica's option that
  shows its status panel ("Layer Mode: All", "Renderers: …") for 10 seconds at a time, so the panel stays off the
  screen (see "Litematica's status panel").
- **M opens Freecam's menu.** On each player's computer, once, Building Planner binds M to Freecam's menu and takes M
  from Litematica's menu, which players then have no key to open (see "Keys").
- **How ghost blocks look.** On each player's computer, once, Building Planner turns on Litematica's "render blocks as
  translucent" and sets its ghost block alpha to 0.8, so ghost blocks draw see-through. It sets each only if the player
  hasn't changed it from Litematica's default (off, and 0.5), and never again, so a player's own later change stays.
  This happens whether or not the player joins a server with Building Planner.
- **Messages.** Building Planner's messages show in the player's chat. Every text players see (messages, key names,
  button and slider labels, the panel) is in the wording Geo approved, which this spec quotes (0.7.0's in 1313).
- **Other servers.** As proposed for 0.10.0, on a server that doesn't run Building Planner, Building Planner works
  from the player's computer alone: their ghost blocks are saved on their computer for that server, and only they see
  them. This server still shares its ghost blocks (see "Servers without Building Planner"). In the player's own world,
  singleplayer or opened to LAN, Building Planner works as on this server, sharing the ghost blocks with the world's
  LAN guests (see "The player's own worlds").

## Geo's goals, and what meets each

| Goal (402 r1, r2) | Met by | Gap |
|---|---|---|
| Measure layouts | Effortless Building shows a shape's size while it's drawn; Building Planner's box for Copy, Delete and the material list shows its size and the blocks in it before anything is placed. | Measures are each player's own until drawn as ghost blocks. MiniHUD-style personal guides were rejected (402 r2, c2). |
| Plan room sizes and shapes before committing | Effortless Building's shapes drawn as ghost blocks, erased with the break click. | None. |
| See and judge a build (free or fixed camera) | Litematica draws the ghost blocks; Freecam moves the camera away from the player; the switch hides them to compare with the real world; with Freecam's camera on, every edit aims from the camera (see "Editing from Freecam"). | None. |
| Lay out walls, shapes and components quickly | Effortless Building's eleven build modes; hold to repeat in Single mode at the player's chosen speed. | None. |
| Copy and paste | Copy takes the ghost blocks and real blocks in a box; a right click pastes the copy into the layer, placed with the controls of Satisfactory's build gun. Effortless Building's array repeats while drawing. | None. |
| Symmetry | Effortless Building's mirror and radial mirror. | None. |
| Randomized block mixes | Building Planner's block mix (Effortless Building's Randomizer needs the server copy, which Geo kept off the server). | None. |
| A plan everyone on the server sees, built for real by hand | The shared layer reaches everyone live; Litematica's ghost blocks, its missing-block colours and the material list for a marked area guide the build. | None. |

## Technical design

### Parts

One jar, mod id `buildingplanner`, installed on the server and in the player pack. It registers no blocks, items or
menus, so it adds nothing to the registries Fabric API syncs at login, and players without mods can still join.

- **Common:** the layer's column storage and its file format, the network payloads, the material lists' format and
  limits, the hold-to-repeat timer, the look defaults' rules, the turns of the block in hand (`BlockTurn`), the turn
  of a real block placed in Disable (`PlaceTurn`, and hook S2, which both sides run) and Delete's marks
  (`DeleteMarks`).
- **Server:** the layer store and its files, column watching, edits and their order, per-player undo, each player's
  material lists and their files, each player's turn for Disable (`PlaceTurns`), and the one-time import of the plans
  Syncmatica shared. They run on a dedicated server, which is how the group plays. As proposed for 0.10.0, all but
  the import also run inside the game while a world of the player's own is open (see "The player's own worlds").
- **Client:** the player's copy of the layer and its column watching, the tiles that hand it to Litematica and the
  hooks that hide them, the show or hide switch, the Effortless Building capture, hold to repeat, the radial menu's
  buttons and sliders, the block mix and its panel, copy and paste, the build modes' lock and first click
  (`ShapeLock`), Esc's steps (`EscSteps`), Disable's turn and its preview (`DisableTurn`, `DisablePreview`), the
  "Match ghost blocks" row and its rule (`GhostMatch`), the ghost blocks drawn through walls
  (`ThroughWalls`), Delete, the material list with its panel and saved lists, the edit glow, the settings it changes
  once, and the check for a server without the add-on.

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
| E1 | `buildpipeline/BuildPipelineClient.java`, `handleClick` | `@WrapWithCondition` on `PacketHandler.sendToServer(PlaceBuildModePacket)`, reading the local `BlockSet blocks` | Take the finished shape (every entry, after mode, modifiers, mix and constraints) into the layer; don't send the packet. `getFirstClickHit()` is still set here, so block states use the first click's face. In every mode each state takes the turn of the block in hand (`BlockTurn.apply`, see "Locking and the first click") after its placement rules and before the entry's mirror and turn from the modifiers (`BlockEntry.applyTransforms`). |
| E2 | same method | `@WrapWithCondition` on `PacketHandler.sendToServer(BreakBuildModePacket)` | Erase the ghost blocks among the shape's positions, in the game's copy of the layer, with the other half of each two-block ghost block (`GhostErase.of`); don't send. A shape over no ghost blocks sends no edit. |
| E3 | same method | `@WrapWithCondition` on `PlacedBlockTracker.clientTrackAll` | Don't record ghost positions as real blocks the player placed. |
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo and redo to the layer. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While the mix applies, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While the mix applies, treat the held block as Effortless Building's Randomizer, so the preview draws each entry's own block. In `shouldInterceptPlacing` this holds only while the mode isn't Disable (`BuildModes.CLIENT.getBuildMode() != BuildModeEnum.DISABLED`), so clicks in Disable stay vanilla's. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not ghost blocks. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapOperation` on both `ItemUsageTracker.compute` calls and on `BreakDisplayTracker.compute`, clearing the tracker instead | No red "missing items" tint: ghost blocks don't use up items. No list of the real blocks under a shape drawn with the break click either (`render/RenderHandler.java`, `drawBreakingStacks`, which draws them in one row from the screen's middle that never wraps): that shape erases ghost blocks and leaves those real blocks. Disable's preview marks its one block missing after this clear (hook E29). |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `resolveFirstClickPos` | `@WrapOperation` on `ClientLevel.clip`; on `Level.getBlockState` | Let shapes start on ghost blocks: aim at the nearer of the real block and a ghost block (`GhostAim`, which traces the ray through the player's copy of the layer while ghost blocks show, not through Litematica's schematic world, which also holds the copy and shows an edit only after its rebuild; so a held click's repeat aims at the ghost blocks the click before it placed, as on real blocks), and read the ghost block's state, where the real block can be replaced, when picking the start position, so the shape goes beside it. |
| E10 | `buildpipeline/BuildPipelineClient.java`, public static `handleRightClick` and `handleLeftClick`, with `shouldInterceptPlacing`, `shouldInterceptBreaking` and `getBuildState`; `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem`; `buildmode/BuildModes.java`, `getBuildMode`; the click checks copy `fabric/.../EffortlessBuildingClient.java`'s client tick | No mixin: Building Planner's own `END_CLIENT_TICK` calls the two public methods again while a button stays down | Hold to repeat in Single mode (see "The client"). Effortless Building's own tick acts only when a button goes down (`rightJustPressed`, `leftJustPressed`). |
| E11 | `screen/RadialMenu.java`, `init()` | `@Inject` at tail, calling `Screen.addRenderableWidget` | Add Building Planner's buttons and sliders to the radial menu each time it opens (Minecraft clears a screen's widgets and calls `init` on each open). They're children of the screen, so `RadialMenu.mouseClicked`, which calls `super.mouseClicked` after its own `performAction`, routes clicks and drags to them. |
| E12 | `screen/RadialMenu.java`, `extractRenderState(GuiGraphicsExtractor, int, int, float)` | `@Inject` before its call to `drawTexts` | Place those widgets for the opening animation (E20) and draw them: the method draws the menu itself and never calls `Screen`'s own drawing, which would draw them. Each frame, the sliders and their headings show only while the mode is Single (`BuildModes.CLIENT.getBuildMode() == BuildModeEnum.SINGLE`), so choosing another mode in the open menu hides them at once. Drawn before the menu's texts, they leave a hovered mode's name on top. |
| E13 | `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem` | `@ModifyReturnValue` | On a server (with a session, or with Building Planner off), an item starts shapes only if it places a block (`ClickRule.placeable`: Minecraft's `BlockItem`, the same test as the mix's; `ClickRule` uses none of Litematica's classes, so these hooks can ask it while Building Planner is off). Effortless Building's own rule also takes buckets and tools with a `TOOL` component, and leaves out doors, beds and tall plants. |
| E14 | `buildpipeline/BuildPipelineClient.java`, `shouldInterceptPlacing` and `shouldInterceptBreaking` | `@ModifyReturnValue` | While the player draws a box or the copy is at the aim (`MouseMode`), neither click is Effortless Building's in any mode, so hooks M1 to M5 take them. With the same rule as E13, while the main hand holds no item that places a block and no shape is under way (`getBuildState()` null), neither click is Effortless Building's either: its client tick, its `AttackBlockCallback`, and its cancelling of vanilla's `startUseItem`, `startAttack` and `continueAttack` all ask these first, and so does hold to repeat, so both clicks and holding them are vanilla's. A shape under way still takes the click that finishes or cancels it. In every build mode, a click on a locked position stays Effortless Building's, and hook E32 hands it the locked position (see "Locking and the first click"). |
| E15 | `Minecraft.pick(float)`, into which Effortless Building's `MixinGameRenderer` adds, at its tail, a reach of 32 for an empty hand, aiming through entities; `buildmode/BuildModes.java`, `getBuildMode` | `@WrapMethod` on `pick`; `@ModifyReturnValue` on `getBuildMode` | While the main hand holds no item that places a block and no shape is under way, or while a box is drawn or the copy is at the aim, `getBuildMode` reads Disable during the pick, which that code skips, so the hand aims as without mods, at an entity before the block behind it. |
| E16 | `screen/RadialMenu.java`, `onClose()` | `@Inject` at head, finding the button under the pointer, and at tail, acting | Let Building Planner's buttons act when the menu closes, as the menu's own do: its `tick` calls `onClose` once its key is let go (Escape calls it too), and `onClose` acts on the menu's button under the pointer unless a click acted since the menu opened (`performedActionUsingMouse`). Building Planner's button is found as `onClose` begins, before closing grabs the mouse and moves the pointer to the window's middle, and acts after the menu has closed, like the menu's own, so "Block mix" can open its screen. |
| E18 | `screen/RadialMenu.java`, `drawTexts` | `@ModifyConstant` on the string `"Effortless Building"` | The menu's corner text, which the method draws right-aligned from the string's width, reads "Building Planner" (`buildingplanner.ui.radial.credits`). It is the only place the game shows Effortless Building's name to a player on this server: its welcome message and its build mode hint come from its server side (`config/WelcomeMessageStorage.java`, `config/BuildModeHintStorage.java`), which the server doesn't run, and its screens' titles ("Build Modes", "Modifier Settings") don't name it. |
| E19 | `screen/RadialMenu.java`, `drawRadialButtonBackgrounds` | `@ModifyExpressionValue` on `BuildModeEnum.ordinal()` | While a box is drawn or the copy is at the aim, no mode segment shows selected (the method compares each segment's index with the mode's `ordinal()`), since the player is in Building Planner's mode, whose button shows selected instead. |
| E20 | `screen/RadialMenu.java`, private field `visibility` | `@Shadow`, read by E12 | The menu's opening animation: `extractRenderState` adds 0.4 × the frame's partial tick to `visibility` each frame, up to 1, and places its ring and its buttons at 0.8 + 0.2 × `visibility` of their distance and size from the middle, while the screen darkens. E12 places and sizes Building Planner's widgets by the same factor before drawing them. |
| E21 | `buildmode/BuildModes.java`, `setBuildMode` | `@Inject` at head | Choosing a mode (the radial menu's `performAction` is its only caller outside `BuildModes`) ends a box and puts the copy away, even when the player chooses the mode they had. |
| E22 | `render/BlockPreviewRenderer.java`, `render` | `@Inject` at head, cancellable | No shape preview while a box is drawn or the copy is at the aim: the method draws the current mode's first block at the aim each frame (`BuildPipelineClient.getPreviewBlocks`). |
| E23 | `utilities/KeyBindings.java`, `<clinit>` | `@ModifyArg` on the category of each of the four `KeyMapping` constructors | Effortless Building's keys go under Building Planner's heading in Controls (see "Keys"). |
| E28 | `render/BlockPreviewRenderer.java`, `getPlacementState(BlockItem, Minecraft, ItemStack)` | `@ModifyReturnValue` | In every mode, the preview's block takes the turn of the block in hand (`BlockTurn.apply`), as E1's states do, so the player sees the block as it will be placed. `BlockPreviewRenderer.render` makes one state through this method for the held block, or one for each of the mix's blocks, and applies each entry's mirror and turn to it after (`BlockEntry.applyTransforms`), so every block of a shape's preview takes the turn. In Disable, while Disable's preview shows, it returns the state a right click would place (`DisablePreview.state`, see "Turning real blocks in Disable"). While a first position is locked, it returns the state a right click places there: the placement rules with the hit it was locked with, moved by the nudges, and the player's facing now (`EffortlessShapes.previewState`, see "Locking and the first click"). |
| E31 | `render/RenderHandler.java`, private static `renderSubtitle` | `@WrapOperation` on `GuiGraphicsExtractor.text(Font, Component, int, int, int, boolean)`, with `require = 0` | During a session, draw Effortless Building's line during a shape in Building Planner's rectangles instead, where it draws it (`EffortlessLine`, see "Building Planner's mouse modes"); a text of another form goes to the original call. It's the one hook that requires no match, since it changes only how the line looks: an Effortless Building whose method no longer makes that call keeps its own line, and Building Planner loads and works as before. The other mixin hooks require their match (`defaultRequire` 1 in `buildingplanner.mixins.json`), so a change under one of them stops the game as it loads. |
| E32 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks` | `@WrapOperation` on private static `resolveFirstClickPos`, beside E9's wrap of `ClientLevel.clip` | While a first position is locked (`ShapeLock`), the first click and the first click's preview take the locked position, and E9's aim returns the hit it was locked with, moved by the nudges, so `getFirstClickHit()` gives E1 that hit's face (see "Locking and the first click"). |
| E33 | `buildmode/TwoClicksBuildMode.java` field `firstBlockEntry`; `buildmode/ThreeClicksBuildMode.java` fields `firstBlockEntry` and `secondBlockEntry`; `buildpipeline/BuildPipelineClient.java` private static field `firstClickHit` | `@Accessor` on each (accessor mixins, getter and setter) | During a shape, a nudge that moves the whole shape moves the positions its clicks have set, and the first click's hit with them, by replacing each with a `BlockEntry` (or hit) one block over; `findCoordinates` reads them each frame, so the preview and the finished shape follow (see "Locking and the first click"). |
| E37 | `buildmode/TwoClicksBuildMode.java`, `findCoordinates`; `buildmode/ThreeClicksBuildMode.java`, `onClick` and `findCoordinates` | `@WrapOperation` on each call to the abstract `findSecondPos(Player, BlockPos, boolean)`, and on the call to `findThirdPos(Player, BlockPos, BlockPos, boolean)` in `ThreeClicksBuildMode.findCoordinates` | While the step under way is locked (`ShapeLock`), each call returns the step's locked position, moved by the nudges, instead of the position the aim finds, so the preview, the click that sets a three-click mode's second position (`onClick` at its second click) and the finished shape take it. These are the only places a mode finds its second and third positions (see "Locking and the first click"). |
| E38 | `render/BlockPreviewRenderer.java`, `render` | `@Inject` at tail; `@Invoker` on private static `renderBoundingBoxFaces`, `renderEdgeQuads` and `computeBorderEdges`, whose `Set<EdgeKey>` of a private record the invokers name as a raw `Set` | Before the first click, draw the positions a left click would start erasing from in Effortless Building's own erase look, red sides and a red outline, beside the first click's block (see "Locking and the first click": "The first click's blocks"). |
| E39 | `screen/ClientConfigScreen.java`, the constructor; `screen/RadialMenu.java`, `MenuButton`'s constructor | `@ModifyArg` on `Component.translatable` in the constructor; `@ModifyArg` on `I18n.get` in `MenuButton`'s constructor when its key is `effortlessbuilding.action.open_client_config` | The screen's title and the radial menu's button read "Settings" (`buildingplanner.ui.settings`) instead of Effortless Building's "Visual Settings", on any server (see "Settings"). |
| E34 | `screen/RadialMenu.java`, `extractRenderState` | `@ModifyExpressionValue` on the `hasPermission(Permissions.COMMANDS_GAMEMASTER)` check before the "Functional Settings" button | During a session, the button isn't added, for operators too: it changes Effortless Building's settings on the server, which this server doesn't run, and "Show or hide ghost blocks" takes its place under the wrench (see "The radial menu's buttons and sliders"). |
| E35 | `screen/RadialMenu.java`, private `performAction` | `@WrapWithCondition` on `LocalPlayer.sendOverlayMessage` after `BuildModes.setBuildMode` | During a session, choosing a mode doesn't put its name in the action bar, since the name shows beside the hotbar (see "The build mode's name and the held item's name"). |
| E36 | `screen/ClientConfigScreen.java`, `init`, `extractRenderState`, private `renderRowTooltips` and private `save` | `@ModifyExpressionValue` on each read of the static `LABEL_KEYS`; `@Inject` at the tail of `init` and at the head of `save` | The "Match ghost blocks" row in "Settings": its label key after the screen's own, so the screen places, labels and explains one row more; its switch, with "Save" and "Cancel" a row down; and the setting saved with the screen's own (see "Settings"). |

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
| M1 | `Minecraft.java`, `startAttack()` | `@Inject` at head, cancellable, returning false | While a box is drawn, the left click sets its next corner; while the copy is at the aim, it drops the copy's set, or puts the copy away when no set is fixed (1238 Q2 A); in Delete, it starts the box with Left Ctrl held, and the one-second hold that erases the marks without it. None attacks or breaks. `handleKeybinds` calls it once for each press of the attack key. |
| M2 | `Minecraft.java`, `startUseItem()` | `@Inject` at head, cancellable | While the copy is at the aim, the right click pastes it, fixes its set's first copy, or pastes the set; while a box is drawn, it leaves that mode and uses nothing. In Delete it clears the marks, or leaves the mode when there are none. `handleKeybinds` also calls `startUseItem` every 4 ticks while the use key stays down (`rightClickDelay`), so the use key does nothing more until it's let go. |
| M3 | `Minecraft.java`, `continueAttack(boolean)` | `@Inject` at head, cancellable | Holding the left click breaks nothing while a box is drawn, the copy is at the aim, or Delete is on. In Delete, the client tick reads the attack key's state each tick (`KeyMapping.isDown`) for the box's second corner, which it sets when the key is let go, and for the erase's hold. |
| M4 | `Minecraft.java`, `pickBlockOrEntity()` | `@Inject` at head, cancellable | While the copy is at the aim, the middle click (the pick block key) mirrors the copy; while a box is drawn or Delete is on, it does nothing. While the turning controls are active (every mode, Disable's as "Turning real blocks in Disable" describes), with a block in hand that flips and Minecraft's sprint key held, it flips the block (see "Locking and the first click"). |
| M5 | `MouseHandler.java`, `onScroll(long, double, double)` | `@WrapWithCondition` on `Inventory.setSelectedSlot`, reading the local `wheel` | While the copy is at the aim, each notch of the wheel turns the copy a quarter turn, instead of changing the hotbar slot: clockwise seen from above for a notch up, counterclockwise for a notch down, as Satisfactory's wheel turns a hologram. While the turning controls are active (every mode, Disable's as "Turning real blocks in Disable" describes), with a block in hand that turns and Minecraft's sprint key held, each notch turns the block the same way. The method reaches that call only with no screen open and the player not a spectator. |
| M7 | `KeyboardHandler.java`, `keyPress` | `@WrapWithCondition` on its call to `Minecraft.pauseGame(boolean)`, which it makes for Esc with no screen open | During a session, Esc takes the next step out of Building Planner's and Effortless Building's modes (`EscSteps`) and doesn't pause; with no step left, it pauses as usual (see "Esc"). The game's other calls to `pauseGame` (when the window loses focus) stay as they are. |
| M8 | `gui/Hud.java`, `extractSelectedItemName` | `@ModifyArg` on the `y` of `GuiGraphicsExtractor.textWithBackdrop` | While Building Planner draws a line above the hotbar, the held item's name draws 2 above the top of its highest row instead of over it (see "The build mode's name and the held item's name"). |

The hooks go by Minecraft's attack, use and pick block keys, so a player who moves one of them to another button finds
the copy's controls there too. Effortless Building's own hooks into `startUseItem`, `startAttack` and
`continueAttack` (`mixin/MixinMinecraft.java`) and its client tick ask `shouldInterceptPlacing` and
`shouldInterceptBreaking` first, which hook E14 answers false in these modes. The copy's own keys (see "Keys") need no
hook: Building Planner's client tick reads them (`CopyKeys`), with no screen open, while the copy is at the aim or a
build mode's lock controls are active, and empties their clicks (`consumeClick`) otherwise, so a press made before
then doesn't act once they act.

Litematica, under `src/main/java/fi/dy/masa/litematica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| L1 | `util/SchematicUtils.java`: `setTargetedSchematicBlockState(BlockPos, BlockState)`, `setSchematicBlockStates`, `setAllStatesToAirExcept(SchematicPlacementManager, PlacementPart, BlockState, Level)`, `replaceAllIdenticalBlocks` | `@WrapWithCondition` on each `LitematicaBlockStateContainer.set(int, int, int, BlockState)` (the only four writes the ten Edit Schematic actions make, all called from `event/InputHandler.java`) | Skip a write into a tile's container, so Litematica's Edit Schematic actions never change the layer, even on a computer whose Litematica tool mode is still "Edit Schematic" (`InputHandler.handleUseKey` and `handleAttackKey` edit in that mode whatever the player holds). Writes to other placements, such as the copy, stay Litematica's own. |
| L4 | `schematic/placement/SchematicPlacementManager.java`, `setSelectedSchematicPlacement(SchematicPlacement)` | `@Inject` at head, cancellable | A tile or the copy is never selected: selecting one leaves the selection as it was, since selecting none would also clear Litematica's material list. Covers `addSchematicPlacement`, which selects a new placement when none is selected, `removeSchematicPlacement`, which then selects the first placement, selection by aim, and the placement list. Tiles and the copy are known as theirs before they're added. |
| L5 | `gui/widgets/WidgetListSchematicPlacements.java`, `getAllEntries()` | `@ModifyReturnValue` | Leave tiles and the copy out of Litematica's placement list. |
| L6 | `render/OverlayRenderer.java`, `updatePlacementCache()` | `@WrapOperation` on `SchematicPlacementManager.getAllSchematicsPlacements()` | Leave tiles, the copy and its set out of the boxes and origin markers `renderBoxes` draws; Building Planner draws the copy's box itself, in its colours (see "Copy and paste"). This leaves Litematica's own switch for those boxes (`enablePlacementBoxesRendering`) as the player set it. |
| L7 | `render/schematic/ChunkRendererSchematicVbo.java`, `getOverlayType(BlockState, BlockState)` | `@ModifyReturnValue` | While Building Planner has a session, turn `EXTRA` into `NONE`. Litematica marks a real block as extra wherever a placement's box holds air there (`schematicOverlayTypeExtra`, on by default), and a tile's box is a whole chunk column's sections, so every real block in it would be marked. The layer holds only blocks to place, so no real block is extra. Missing and wrong blocks keep their colours. |
| L8 | `schematic/placement/PlacementManagerTaskRebuild.java`, `run()`; `PlacementManagerTask.java`'s constructors | `@WrapMethod`; a number for each task, in the order they're made (`RebuildOrder`) | Run the rebuilds of one chunk column one at a time. Litematica's background threads (at least two) rebuild a column from its placements' blocks as they are when the rebuild runs, so two rebuilds of one column at once could end with the older one's result, read before an edit's blocks were written. It also notes when each rebuild begins and ends, for the self-check ("Overlapping shapes"). |
| L9 | `schematic/placement/PlacementManagerDaemonHandler.java`, `removeRebuildTasksFor` and `removeDeferredTasksFor` | `@WrapMethod` | A running rebuild drops from the queue only the rebuilds of its column made before it began, whose edits it reads. Litematica's rebuild drops every queued rebuild of its column as it ends, and as it unloads a column with no placements, including one queued for an edit written after it read the column, which would leave that edit undrawn. Elsewhere (a new rebuild replacing the queued ones, an unload) every queued rebuild of the column still goes. |
| L10 | `gui/GuiMaterialList.java`, private `createButtons()` and `createButton(int, int, ButtonListener.Type)` | `@WrapOperation` on `addButton` in `createButton`, which adds "Save list" or "Saved lists" in place of the `EXPORT` or `EXPORT_TYPE` button, telling them by the listener's `type` (read by reflection, since `ButtonListener.Type` is private); `@WrapOperation` on `addButton` in `createButtons`, the call that adds the `ButtonListenerChangeMenu` | For Building Planner's list only (`GhostMaterialList`): no "Export" button, no export type button ("Write to file", which cycles), and no "Litematica menu" button, which `createButtons` adds at the bottom right; "Save list" and "Saved lists" go where "Export" and the export type were, in the same row, which the method moves to the bottom left when the screen is narrow (`isNarrow`), and "Close" goes where "Litematica menu" was. Other material lists keep their buttons. |
| L12 | `gui/GuiMaterialList.java`, private `createMultiplier()` | `@ModifyArg` on the translation key of the `WidgetInfoIcon` it adds at the top right (`litematica.info.material_list`) | For Building Planner's list, the (i) icon shows Building Planner's text (`buildingplanner.info.material_list`, see "The screen's buttons") instead of Litematica's, which is about Litematica's hotkey, its placements and its schematic browser. |
| L11 | `util/WorldUtils.java`, `doSchematicWorldPickBlock(boolean, Minecraft)` | `@Inject` at head, cancellable, returning false | While a mouse mode is on, Litematica picks no block from its placements, so its "pick block first" hotkey (the middle click by default), which runs before the game sees the click and keeps the click when it picks, leaves the click to the game's pick block key and hook M4. The copy sits under the crosshair, so the click would otherwise pick one of the copy's blocks and never mirror it. The same holds while Minecraft's sprint key is held with the turning controls active and a block in hand that flips, so the middle click on a ghost block flips the block rather than picking the ghost block. Otherwise Litematica's pick block works as it does without Building Planner. |

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
`Configs.InfoOverlays.STATUS_INFO_HUD_AUTO`, `Configs.Generic.TOOL_ITEM_ENABLED`, `Hotkeys.OPEN_GUI_MAIN_MENU` and `Configs.saveToFile`. From MaLiLib:
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
began after the change queued its own rebuild has finished (hook L8 notes when each one begins and ends,
`RebuildOrder`), or 30 seconds. It reads the column only while no rebuild of it runs. A rebuild unloads the column's
chunk and loads an empty one in its place, fills a new chunk from the placements, and puts that in only as it ends
(`PlacementManagerTaskRebuild`). Another rebuild of the column can begin as soon as one ends, such as one that
Litematica's own check around the camera made before the tile came and queued at the next tick. If a rebuild of the
column begins while the self-check reads it, the self-check reads it again at a later tick. It reads Litematica's
schematic world over the tile's sections, leaving out the boxes of other placements (the copy's, a player's own),
which Litematica draws over the tile. Nothing is compared while ghost blocks are hidden or
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
  erase), `undo` and `redo`; for material lists `save_list`, `delete_list`, `open_list` and `current_list` (see
  "The lists on the server"); `place_turn(item, properties)`, the turn of the real block a right click places in
  Disable (see "Turning real blocks in Disable"); and, proposed for 0.9.0, `place(dimension, face, hit, blocks)`, the
  real blocks of a shape or a paste (see "Place real blocks"), which protocol version 5 takes as one more payload.
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
player's next new edit clears the list. A shape or a paste of real blocks, proposed for 0.9.0, makes no step on either
list (see "Place real blocks": "Undo").

### Ghost blocks the real world fills

A ghost block leaves the layer once the real block at its position fills it, so the built part of a plan stops showing
as planned, for everyone. A real block fills a ghost block when it's the same block placed the same way, whatever it
connects to (809 Q1 C): the same block, with the same value for each property a player sets by placing it (facing,
axis, top or bottom half, slab type, door hinge, bed part, attach face and rotation), and any value for the properties
the world sets (connections, stair shape, waterlogged, powered, open, lit, age, distance and the like). A block that
goes several to a spot (candles, sea pickles, snow layers, turtle eggs, pink petals and leaf litter) fills its ghost
block only at the same count (823 Q1 B): the ghost block of 4 candles stays until the 4th candle goes in. A shape takes
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

Edits never write a ghost block over a real block that fills it, and they remove any older ghost block there (823 Q2
B): in a shape, a paste, an undo or a redo, each position whose real block fills the new ghost block, where the server
has the chunk loaded (`ServerChunkCache.getChunkNow`), takes air instead of the new ghost block, as if the new ghost
block had been drawn first and built afterwards. A ghost stone block under real oak planks goes when ghost oak planks
are drawn there, as part of the same edit and the same undo step, so one undo brings the stone back and a redo removes
it again; where no older ghost block is there, the position changes nothing, so a paste of a copied house onto the
house changes nothing. Where the server hasn't loaded the chunk, the new ghost block is written and goes when the chunk
loads. The end-of-tick edit and the check at chunk load only remove ghost blocks, so this changes nothing for them.

Ghost blocks already filled, such as those built before 0.6.0, go as each chunk loads: on Fabric API's
`ServerChunkEvents.CHUNK_LOAD`, which also fires for the chunks the server loads as it starts (it reads the layer at
`SERVER_STARTING`, before any level loads), the server compares the real blocks with the ghost blocks in that chunk's
sections that hold ghost blocks, skipping a section of only air, and removes the filled ones at the end of the tick by
the same edit as S1's. Players see ghost blocks only in chunks loaded around them, which the server has loaded first, so no
player sees a filled ghost block. A section needs 4,096 comparisons, expected to take microseconds (not measured), and
a chunk without ghost blocks costs one lookup.

Undo and redo treat a filled ghost block as gone: an undo of a shape whose blocks have since been built leaves those
positions as they are, since the layer no longer holds the shape's blocks there. A redo of a shape over a ghost block
whose real block now fills the shape's block removes that older ghost block, as an edit would. The undo's notice counts only the
blocks another player changed, not the ones built since: the server checks the real block at each position it leaves
and counts it only when it doesn't fill the step's ghost block. A redo skips the positions built since, like any edit.

The material list counts the same either way: a real block with no ghost block over it counts in its block's "Total"
and never in "Missing", as a filled ghost block did. A block built with other connections stops counting as "Missing"
once its ghost block goes.

### Ghost blocks through walls

Litematica draws each ghost block as a see-through block with a coloured overlay
(`ChunkRendererSchematicVbo.getOverlayType`, with Litematica's default colours in `Configs.Colors`): light blue where
the real world has air or a fluid there (missing, #33B3E6), red where it has a different block (wrong block, #FF3333),
and orange where it has the same block in another state (wrong state, #FF9010). The overlay draws only where the
player can see the ghost block. Red shows where a shape was drawn over real blocks it didn't fill, or a player built
the wrong block into a ghost block; orange shows where a block was built facing another way, or a block that goes
several to a spot has fewer than planned. Litematica's own "Render Through" option draws every overlay through walls
at every distance.

Building Planner draws every ghost block within 32 blocks of the camera (1241 Q2 C) through walls itself, in its
overlay's colour (Geo's comment c3 on 815 revision 2, 1241 Q3 B, for red and orange; comment c2 on 1359 for light
blue: "ghost blocks should also appear through walls in the same way (if within the 32 blocks)"). By Litematica's
rule, a ghost block whose real block is air or a fluid is light blue (#33B3E6), one whose real block is a different
block is red (#FF3333), and one whose real block is the same block in another state is orange (#FF9010). A ghost block
whose real block is the same block in the same state draws nothing: the server takes it out of the layer (see "Ghost
blocks the real world fills"). Each gets an outline and faint sides through any block in front of it, the outline at
0.8 alpha (1313 Q2 B) and the sides at 0.1, drawn through MaLiLib's renderer with the depth test off, after the world
(`ThroughWallsOutline`). Faces between two such blocks of the same colour aren't drawn, as for Delete's marks
(`OutlineMesh`, the shape `MarksOutline` draws too), so each colour shows as one shape: a planned wall with nothing
built shows as one light blue box. Where the player sees the ghost block itself, the faint colour draws over
Litematica's overlay of the same colour. Nothing draws while ghost blocks are hidden or without a session.

The positions come from a pure function of the player's copy of the layer and the real blocks (`ThroughWalls`), worked
out again every 10 ticks over the 16-block sections within reach that hold ghost blocks, so a block built, broken,
drawn or erased shows within half a second. In the test harness, a scan with a ghost block at all 274,625 positions in
range takes 4.5 ms at the median of 20 scans, and 26.9 ms at the slowest. Missing blocks make most ghost blocks part of
a shape, so a scan's result changes with each edit nearby. `ThroughWallsOutline` therefore rebuilds only the 16-block
sections whose positions or colours changed since the last scan, as Delete's marks rebuild only the sections a mark
changed, instead of both shapes whole: in the harness, building the shape of 200,000 marks whole takes 59 ms to 546
ms, and building it again after one mark changed 0.6 ms on average (see "Unverified").

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
| `status-panel-off-applied=true` | Litematica's `InfoOverlays` option `statusInfoHudAuto` (on) | off, then `Configs.saveToFile()` (see "Litematica's status panel"). Only with Litematica loaded. |

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

Building Planner has three modes of its own that take over the mouse (`MouseMode`): drawing a box, for Copy or the
material list; the copy at the aim; and Delete (`DELETE`, see "Delete"). At most one is on at a time. None changes
Effortless Building's mode, so nothing needs putting back when one ends: while one is on, hook E14 keeps Effortless
Building's clicks out and E15 its longer aim, E22 hides its preview, E19 shows no mode selected in the radial menu, and
hooks M1 to M5 take the clicks and the wheel. The build modes' lock (see "Locking and the first click") isn't a mouse
mode: it acts only while no mouse mode is on. A mode ends when the player:

- right-clicks (M2) during a box, or in Delete once no ghost block is marked;
- left-clicks (M1) with the copy at the aim and no set fixed (1238 Q2 A);
- presses Esc, one step at a time (see "Esc");
- chooses a mode in the radial menu (E21), even the one they had;
- shows or hides ghost blocks;
- presses "Paste" in "Saved copies" (proposed for 0.11.0), which then brings that copy to the aim;
- presses the radial menu's Copy, Delete, "Material list", Paste or "Block mix" button, or the block mix
  key, each of which then does its own action (Copy and "Material list" start or switch a box, Delete starts or ends
  Delete, Paste brings the copy back);
- leaves the server or changes dimension.

Undo, redo and the speed sliders keep the mode, so a player can undo a paste and paste again, and so do the copy's own
keys (see "Keys").

Chat says nothing when a box ends before its third click: the controls line goes away and the box stops drawing.

Opening a screen (the inventory, chat, the radial menu) keeps the mode; its controls act only with no screen open,
since the hooks act only on the game's own clicks and wheel.

While a mode is on, with no screen open and the HUD shown (not hidden with F1), a line of its controls shows on screen
(`ModeHud`, drawn through `HudElementRegistry`), centred 58 above the screen's bottom, where Effortless Building draws
its own line during a shape (`RenderHandler.renderSubtitle`), above the hotbar. Each control is a rectangle of its
own, "key: action", with the key in white and the action in dark aqua, Effortless Building's colours (Geo's choice,
inbox request 1155). A reason a control can't act ("Can't paste: too many blocks") is a rectangle of its own in red,
in that control's place, and a box's size ("12 × 5 × 8") is the last rectangle, in white. A line has no label (1313
[c1]: "we don't need these statuses, like "Turned:" and "Locked:", those should either be evident from how the preview
looks or is indicated on the control hint itself, like "Lock (On)""): the lock's state and the copy's mode show in
their controls, a turned block in its preview, and the tool's name beside the hotbar (see "The build mode's name and
the held item's name"). A warning above the line ("Too many blocks") is a line of one rectangle, in red. Below, each
rectangle is written in [ ], in the form Geo chose (inbox request 1155): "[Left-click: set the first
corner][Esc/Right-click: cancel]".

Esc goes in each line with the click that does the same, as "Esc/" before the click's name (Geo's comment c13): a
control that closes or cancels reads "[Esc/Right-click: close]" or "[Esc/Left-click: cancel]". A control that switches
a state names the state it's in, in brackets after its action (Geo's comment c6): "[R: Mode (Off)]", "[H: Lock
(On)]". These are the texts, as Geo approved them (1313):

- a box for Copy: "[Left-click: set the first corner][Esc/Right-click: cancel]", then "set the opposite corner", then
  "set the height", each followed by the box's size: "[Left-click: set the opposite corner][Esc/Right-click:
  cancel][12 × 1 × 8]";
- a box for the material list: the same.

The copy at the aim's line names its mode and its keys, as the player has them bound. The game's texts use the fewest
words that stay clear, and one name for each action, as in Geo's edits: "Too many blocks", the panel's column headings
"All", "Hold" and "Need", and "[Arrow keys + Page Up/Down: nudge]", where "nudge" names what all six keys do. R's
control names what it changes, "Mode", the key's name being "Change copy mode". The copy's close is its
left click (1238 Q2 A):

- Off, following the aim: "[Right-click: paste][Scroll: turn][Middle-click: mirror][H: Lock (Off)][R: Mode
  (Off)][Esc/Left-click: close]";
- locked: "[Right-click: paste][Arrow keys + Page Up/Down: nudge][Scroll: turn][Middle-click: mirror][H: Lock
  (On)][R: Mode (Off)][Esc/Left-click: close]";
- Line, Wall or Floor before its first click: "[Right-click: set the start][Scroll: turn][Middle-click: mirror][H:
  Lock (Off)][R: Mode (Line)][Esc/Left-click: close]", and Wall and Floor the same with their names in R's control;
  locked, the locked line with "[Right-click: set the start]" and R's control naming the mode;
- Line, Wall or Floor after its first click: "[Right-click: paste 4 copies][Scroll: turn][Middle-click: mirror][H:
  Lock (Off)][R: Mode (Line)][Esc/Left-click: cancel]", with the count ("paste 1 copy" for one), and locked, with the
  nudges and "[H: Lock (On)]";
- after the player's first paste in the dimension, "[Hold Left Control: line up]" goes after "[Middle-click: mirror]"
  in the lines of a copy that follows the aim. A locked copy doesn't line up, so no locked line has it;
- as proposed for 0.11.0, "[V: saved copies]" goes before "[Esc/Left-click: close]" or "[Esc/Left-click: cancel]" in
  every line of the copy at the aim (see "Saved copies");
- while the copy is red, "[Right-click: paste]" (or the set's paste) gives way to the reason, in red: "[Can't paste:
  part of the copy is outside the world]" (the build height or the border) or "[Can't paste: too many blocks]". In
  Line, Wall and Floor before the first click the reason goes in the same place, before "[Right-click: set the
  start]", which stays since that click still fixes the first copy: "[Can't paste: too many blocks][Right-click: set
  the start][Scroll: turn]...".

Each key goes by its name as Controls shows it ("H", "Page Up", "Left Control"), and a key held down as "Hold" and its
name ("Hold Left Control"). The six nudge keys make one control, "<keys>: nudge": the four along the ground show as
"Arrow keys" while they're on Up, Down, Left and Right in that order, and otherwise by their names one by one, forward,
back, left and right ("I/Down Arrow/Left Arrow/Right Arrow"); the two up and down show as one name and the second
name, which drops the first word the two names share ("Page Up/Down", "Keypad 8/2"), or both names in full when they
share none ("U/J"). The two parts are joined with " + " ("[Arrow keys + Page Up/Down: nudge]", "[I/J/U/O + Y/N:
nudge]"). A key with no key bound leaves its control out, and the nudges' control goes when all six are unbound. The
clicks and the wheel keep Effortless Building's names for them. The line is a pure function of the copy's state and the
keys' names (`ControlsLine`), as a box's lines are.

Each rectangle is its text with 2 pixels more at each side, one line high, on a dark see-through background, black at
0.5 opacity, the game's chat background at its default opacity, so white words stay readable over light ghost blocks
(white wool, glowstone, snow); a gap of 4 separates the rectangles in a row (`ControlsRows`). Each row is centred. A
line too wide for the screen, less 4 at each side, goes on in a row below, 2 below the row above, moving whole
rectangles, never splitting one; the last row starts 58 above the screen's bottom. `ModeHud` fills every background
with `GuiGraphicsExtractor.fill` before drawing the text.

Effortless Building's own line during a shape, which it draws there as plain text, is drawn in the same rectangles
during a session (hook E31; Geo's choice, inbox request 1155, Q1 A), so both lines above the hotbar look alike. It draws
one of three texts (`RenderHandler`'s `PLACING_TEXT`, `INTERACTING_TEXT` and `BREAKING_TEXT`), and each clause "key to
action" becomes the control "[key: action]", in Effortless Building's own words, so a line it adds or rewords in that
form shows too (`ControlsLine.effortless`):

- during a shape drawn with the place click: "[Esc/Left-click: cancel][Right-click: place]";
- with a tool in hand during that shape (an item with Minecraft's `TOOL` component, such as a pickaxe):
  "[Esc/Left-click: cancel][Right-click: interact]";
- during a shape drawn with the break click: "[Left-click: break][Esc/Right-click: cancel]".

The key and its colon are white, and each action keeps Effortless Building's colour: dark aqua, and red during a shape
drawn with the break click, where Effortless Building draws both actions red and the keys white. The rectangles are
laid out as the controls' are, with the same background, padding and gap of 4, in a centred row starting 58 above the
screen's bottom, where Effortless Building draws its line. A text of another form, or in other colours, is drawn by
Effortless Building as it is, and so is the line wherever Building Planner is off (see "When the server lacks the
mod").
The game's "Text Background" accessibility setting ("Chat" or "Everywhere") doesn't change it: the setting works only
through `Options.getBackgroundColor`, which the game's own text (the action bar, subtitles, the player list) calls and
the controls line doesn't, so the background is always there at that opacity. Effortless Building's "Left-click to
cancel" reads "[Esc/Left-click: cancel]", and its "Right-click to cancel" "[Esc/Right-click: cancel]", since Esc
cancels the shape too (see "Esc"); Effortless Building writes its line in English in its code, so
`ControlsLine.effortless` knows the action "cancel" by its word.

#### Esc

With no screen open, Esc steps out of one mode a press at a time, and pauses the game only when no step is left
(Geo's comments c10 and c17: "Esc will progressively take you out of modes, one press at a time, before it acts like
pause"). Hook M7 runs the first step that applies (`EscSteps`, a pure function of the modes' state):

1. a box for Copy or the material list ends, as its right click ends it;
2. in Delete: a box being drawn with Left Ctrl ends and marks nothing; otherwise the marks clear, as the right click
   clears them; with no marks, Delete ends;
3. the copy at the aim: its set, after its first click, drops, and the copy follows the aim in the same mode (1238
   Q3 A); with no set fixed, the copy is put away;
4. a shape under way in a build mode is cancelled, as the click that cancels it in Effortless Building's line does
   (the left click for a shape drawn with the place click, the right click for one drawn with the break click;
   `BuildPipelineClient.cancelCurrentSequence`), keeping a position locked before its first click, as that click
   does;
5. a build mode other than Disable becomes Disable (`BuildModes.setBuildMode`), which also ends its lock (E21);
6. the game pauses.

So during a copy's Line, Esc drops the set, Esc again puts the copy away, and Esc a third time pauses, or first
returns the build mode to Disable when one is chosen. Each step ends with the same sounds and messages as the click
or button that takes it. Esc held down takes one step, and its repeats pause only when no step is left; F3 with Esc
pauses without the menu, as it does without Building Planner. Esc with a screen open closes the screen, as it does
without Building Planner, and without a session Esc pauses at once.

#### The build mode's name and the held item's name

Effortless Building shows a mode's name in the action bar for a few seconds after the player chooses it
(`RadialMenu.performAction`), and the game shows the held item's name, after the player switches to it, 59 above the
screen's bottom in survival mode (`Hud.extractSelectedItemName`), over the controls line's rectangles, which start 58
above it (Geo's comment c17, image 1: "Oak Stairs" over "[Hold Left Control: turn or flip]", with "Line" above).

During a session, while Effortless Building's mode isn't Disable and the HUD shows, `ModeHud` draws the mode's name
(Effortless Building's own, such as "Line" or "Diagonal Wall") just left of the hotbar, on the controls' dark
background in a rectangle of the same padding, white, ending 4 left of the hotbar, or 4 left of the off hand's slot
when the off hand holds an item and the slot shows on the hotbar's left, and centred on the hotbar's height
(`ModeName`). While a box for Copy or the material list is drawn, the copy is at the aim (in Off, Line, Wall and
Floor, locked or not), or Delete is on, the tool's name, "Copy", "Material list", "Paste" or "Delete", shows there in
the mode's name's place, in the same look, in Disable too (1313 revision 2, Q3: "I thought these should be on the left
side of the hotbar, like the names for the placement modes?"; 1335 [c1]: "why does this say "Single"? If this text is
supposed to be the currently selected action, shouldn't it be "Paste"?"; `ControlsLine.toolName`); once the box ends,
the copy is put away or Delete closes, the mode's name comes back, or none in Disable. Hook E35
keeps the name out of the action bar. Hook M8 draws the held item's name 2 above the top of the highest row of
Building Planner's lines, about where the action bar's text sits above a line of one row; with no line showing, the
game draws it in its own place.

### Boxes for Copy and the material list

The radial menu's Copy and "Material list" buttons start Building Planner's box (`BoxDraw`) for that purpose; there
are no keys for them. Pressing the same button while its box is drawn ends the box; pressing the other switches the
box to that purpose and keeps the corners set so far (752 Q4 B). Delete draws its own box, with two corners, and
marks rather than erases (see "Delete").

Only the left click draws the box (M1), with three clicks, as Effortless Building's "Cube" draws (`Cube.findSecondPos`,
which calls `Floor.findFloor`, and `ThreeClicksBuildMode.findHeight`), with Building Planner's own reach:

1. The first corner is the block the player aims at, real or ghost (as hook E9 aims, ghost blocks counting only while
   they show), or, when they aim at the block's top face, the space above it, so a box started on the ground holds what
   stands on it. Until the first click, while the player aims at a block's top face, that face shows shaded: a black
   square at 0.35 alpha drawn just above it (`BoxOutline`), so the player sees which block the corner sits on (Geo's
   comment c8).
2. The second corner is the block where the line of sight crosses the first corner's level (`BuildModes.findYBound`).
3. The height is where the line of sight passes nearest the vertical line through the second corner.

Between clicks, the box follows the aim: its outline and see-through sides draw each frame through MaLiLib's renderer
(`RenderUtils.renderAreaOutline` and `renderAreaSides`), with the corners set so far fixed, in the colour of its
purpose (`BoxLook`; Geo's comment c8): white for Copy, and blue for the material list, the copy's blue (#00D4FF).
Delete's box is red (see "Delete"). The player may walk, or fly in creative mode, while the box is drawn, so a far
corner is reached by walking toward it.

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
cell "+N" for the kinds of block left out. Each row of cells sits on the controls' dark background (black at 0.5
opacity), 2 wider than its cells at each side, so icons and counts stay readable over bright blocks (Geo's comments c11
and c18). A box past a limit shows no list. The blocks are counted by the copy's rule
(`CopyRules`): at each position the ghost block, while ghost blocks show, and otherwise the real block, leaving out air.
The count (`BoxCount`) reads whole 16-block sections: a section the box takes in whole, with no ghost blocks, is counted
from its palette (`LevelChunkSection.getStates().count`), a section of only air (`hasOnlyAir`) is skipped, and only the
sections the box cuts or that hold ghost blocks are read position by position. It runs again when a corner moves, at
most once every 5 ticks.

The third click ends the mode and hands the box, by its lowest and highest corners, to Copy or the material list.

### Delete

Delete is one mode, like Satisfactory's dismantle: the player marks ghost blocks, by sweeping the aim or by drawing a
box, and a confirm erases the marked ones (Geo's note on 972, inbox request 981, and Geo's comment c16 on 815
revision 2: "The delete box modes should be merged with the delete by aim modes into a single mode"). The radial
menu's Delete button starts it, as the mouse mode `DELETE`, and ends it when it's on; Copy and "Material list" start
their own box. While it's on, the Delete button draws in the menu's blue for a selected option. Starting Delete while
ghost blocks are hidden shows them first, as starting a shape does (see "Show or hide"). R does nothing in Delete.

**Marking by sweeping.** While the player holds Minecraft's sprint key (Left Ctrl unless they change it) without the
left button, each ghost block the crosshair passes over is marked (981 Q2 A, Q3 A; 1240 Q1 A). While a box is drawn,
the sweep marks nothing (Geo's comment on 1240 Q1: "if the user is actively drawing the delete box, it does *not*
mark ghost blocks that the crosshair passes over").
Each frame, Building Planner aims as hook E9 aims (`GhostAim`, ghost blocks only, up to 256 blocks from the eyes, as a
box's corner): a real block in front of a ghost block stops the aim, and only ghost blocks are marked. So that a quick
sweep leaves no gap, it also aims along the look directions between the last frame's and this frame's, from eye
positions between the two, in as many steps as keep the aimed points at most half a block apart at the farther of the
two frames' aims, up to 64 steps a frame (`DeleteMarks.swept`, run by `DeleteAim` once a frame, after the world is
drawn). The sprint key still sprints, so a player who walks forward while marking also sprints, and in Freecam the
camera flies faster, as when lining up the copy.

**Marking with a box.** While the player holds the sprint key, pressing the left button (M1) sets the box's first
corner at the block the crosshair is on, ghost or real, as E9 aims; the box then follows the aim, its outline and
see-through sides drawn in the marks' red (#FF7070, sides at 0.2 alpha) as a Copy box is drawn, and letting the left
button go sets the second corner at the block the crosshair is on then (Geo's comment c16). Every ghost block in the
box between the two corners, at every height between them, is then marked. The box has a Copy box's limits (a corner
up to 256 blocks from the eyes, at most 256 blocks a side, every chunk it touches loaded), and while the box would
break one, it draws red with the reason above the controls line, as a Copy box does, and letting go marks nothing.
Pressing the left button while aiming at nothing starts no box; while the box is drawn, aiming at nothing keeps the
second corner where it was. Letting go of the sprint key during the box doesn't end it; the left button's release
does. The sweep doesn't mark while the left button is down, so the box marks only what's in it. The box's ghost blocks
show under the crosshair while it's drawn, as a Copy box's blocks do.

**Marking or unmarking.** Each hold of the sprint key either marks or unmarks, decided at its first ghost block: the
first ghost block its sweep passes over, or its box's first corner, whichever comes first. If that ghost block is
marked, the hold unmarks each ghost block its sweep passes over and its boxes take in, until the player lets go of the
key (981 Q4 A, and Geo's comment c16: the box "should also respect the inversion behavior of looking if you started it
by looking at a block that's already marked"), as Satisfactory's mass dismantle deselects.

Only the ghost block is marked, not the other half of a two-block ghost block (a door, a bed), which the erase takes
with it (`GhostErase.of`). Up to 200,000 ghost blocks may be marked, the server's limit for one edit (981 Q5 A),
counting the other halves the erase would take; once the marks reach it, a sweep marks nothing more, a box that would
pass it draws red and marks nothing, and the controls line shows "Too many blocks" above its controls. A mark drops when
its ghost block leaves the player's copy of the layer, for any reason: another player's edit, the player's own undo, a
real block filling it, or its column unloading (`DeleteMarks.retain`, run when the layer's version changes). Leaving
the server, changing dimension, and anything else that ends Delete clear the marks.

**Look.** Each marked ghost block has a red outline and see-through red sides (981 Q6 A), in the red of the copy's box
(#FF7070, sides at 0.2 alpha), drawn through MaLiLib's renderer as the copy's box is (`MarksOutline`). Only the faces
that don't touch another marked block are drawn, and only the edges where the shape turns, so a marked wall shows as
one red shape (1060 Q1 A). The shape is built in sections of 16 blocks a side, as the game builds its terrain, and a
section is built again only when a mark in or beside it changes (`DeleteMarks.takeChanged`), so a frame of a sweep
builds only the few sections it touches; each frame draws each section where it is from the camera. The marked ghost
blocks show under the crosshair as a box's blocks do (`BoxList`): each kind of block with its count, most numerous
first.

**Erasing.** Holding the left button without the sprint key for one second (20 ticks) erases the marked ghost blocks
(981 Q2 A), as holding the left button for a second dismantles in Satisfactory: M1 starts the count when the button
goes down, the client tick counts each tick it stays down, and letting go before the 20th tick erases nothing and
starts the count again at the next press (`EraseHold`). Holding on past the erase erases nothing more until the button
is let go and pressed again. With no marks, the left button alone does nothing, and a hold whose marks all go before
its 20th tick (a right click, another player's edit) erases nothing. The erase sends one erase edit (`GhostErase.of`
over the marks), air at each marked position and at the other half of each two-block ghost block among them, so one
undo brings them all back, and other players see them glow as they go. The player sees "Erased 120 ghost blocks."
("Erased 1 ghost block." for one), counting the edit's positions, written with digit grouping from 1,000 ("Erased
16,001 ghost blocks."); Geo approved these texts and the button's name (inbox request 972). The marks go as the edit is
sent, and Delete stays on to mark more (981 Q7 A); an undo brings the ghost blocks back unmarked. A right click, or
Esc, clears the marks; with no marks, it ends Delete.

**The progress bar.** While the left button is held to erase, a bar in the middle of the screen shows how far the hold
has gone (Geo's comment c16: "a prominent progress bar in the center of the screen,
just above the controls text hints"): 182 wide, the hotbar's width, and 5 high, centred across the screen, its bottom 4
above the top row of the controls line, on the controls' dark background, filling from the left in the marks' red
(#FF7070) one twentieth a tick, full at the erase (`ModeHud`, from `EraseHold`'s count). It goes when the button is let
go.

**Controls line.** Delete's line shows as the other mouse modes' lines do (see "Building Planner's mouse modes"), with
the keys as the player has them bound, and names only the controls that work at that moment (`ControlsLine.delete`);
a key with no key bound is left out, as in the copy's lines. "Delete" shows beside the hotbar while Delete is on (see
"The build mode's name and the held item's name"). These are the texts, as Geo approved them (1313), with "toggle
mark" in place of "mark" (Geo's comment c6 on 1359: "the text that says "mark" in the control hints should say "toggle
mark""), since a sweep that starts on a marked ghost block unmarks:

- with no marks: "[Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a box][Esc/Right-click:
  close]";
- with marks: "[Hold Left-click: erase][Hold Left Control: toggle mark][Hold Left Control + Left-click: mark a
  box][Esc/Right-click: clear]";
- during a hold that unmarks, "unmark" and "unmark a box" in place of "toggle mark" and "mark a box";
- while the box is drawn: "[Release Left-click: mark the box][Esc: cancel][12 × 1 × 8]", with "unmark the box"
  for a hold that unmarks, and the box's size;
- once the marks reach the cap, the warning line "Too many blocks" above the controls.

Effortless Building's shapes drawn with the break click keep Effortless Building's own limits (in survival, reach 32,
64 a side and 2,000 positions a shape, counted whether or not they hold ghost blocks), and erase only the ghost blocks
in the shape: hook E2 sends air only for the positions that hold a ghost block in the game's copy of the layer, and the
other halves of two-block ghost blocks, rather than every position of the shape. Raising Effortless Building's own
limits to the box's instead would freeze the game for a large shape: it lists and sorts every position of the shape
each frame while it follows the aim (`BuildPipelineClient.getPreviewBlocks`, `BlockSet.sortByDistance`), 16.7 million
for a cube 256 across (inferred from the code, not measured).

Satisfactory's dismantle, from the official wiki's [Build Gun](https://satisfactory.wiki.gg/wiki/Build_Gun) and
[Controls](https://satisfactory.wiki.gg/wiki/Controls) pages: holding the left button for one second dismantles the
building aimed at; holding Left Ctrl marks up to 50 buildings at once, and letting go of Ctrl and pressing it again
while aiming at a marked building deselects instead. A player's post in the Steam discussion
["HOW DO I REMOVE BUILDINGS"](https://steamcommunity.com/app/526870/discussions/0/2577697615749987269/) (reply #5,
July 2nd, 2020) describes moving the mouse with Ctrl held marking everything the cursor touches. Satisfactory marks
whole buildings, such as an 8-metre foundation, where Delete marks single blocks, so Delete's limit is the server's,
not 50. Satisfactory's Dismantle Filter (G) and its blueprint mode have no equivalent here.

### The radial menu's buttons and sliders

Hooks E11 and E12 add Building Planner's widgets to Effortless Building's radial menu (opened by holding its key, left
Alt by default; it closes when the key is let go): seven buttons, "Show or hide ghost blocks", Copy, Paste, Delete,
"Block mix", "Material list" and "Material lists", by the menu's left-hand buttons, and, while Single is the mode, two
sliders on its right side, under the headings "Build speed" and "Break speed". The widgets sit by the menu's middle,
as its own buttons do (`RadialMenuWidgets`): the menu's left-hand buttons are 20 across with their middles 26 apart,
the leftmost 157 left of the middle, in two rows 13 above and below it, so they span 167 to 95 left of the middle and
end 23 below it.

**The sliders.** The sliders sit where a mode's own options sit, as "Build speed" and "Break speed" are Single's
options (Geo's comment c7 on 1359: "displayed just like the mode-specific controls for the cube mode's "filling"
section (with that header and the controls under it, in that position)"). Effortless Building draws each section of
a mode's options (`RadialMenu.extractRenderState`, `drawTexts`) as a heading in `optionTextColor` (#EEEEEE),
left-aligned 105 × the opening factor − 9 right of the middle, with the section's buttons in a row under it, their
middles 26 apart from 105 right of the middle. The first section's heading is 37 above the middle and its buttons'
middles 13 above it, and each next section is 39 lower. Cube's "Filling" is its only section, with three buttons, and
Circle shows two, "Start Point" and "Filling". Single has no
options of its own (`BuildModeEnum.SINGLE`, whose build speed option is commented out), so the right side is empty in
Single. Building Planner draws "Build speed" as the first section's heading and "Break speed" as the second's, in the
menu's font, colour and place, and each slider in its section's row of buttons: 20 high and centred on that row,
starting 95 right of the middle, where a section's first button starts, and as wide as the larger of 72 (three
buttons' row) and the widest value either slider shows plus 4 on each side. The headings and sliders show only while
Single is the mode, so the controls for hold to repeat show only where hold to repeat runs. On the right side they
meet nothing the menu draws, at every GUI scale where the menu's own option buttons fit. Each slider draws as the
game's own sliders do (`RepeatSlider`), with its text at full size, as the menu's own option headings are.

The widgets join the menu's opening animation (E20): before each frame, E12 places each widget at its distance from the
menu's middle times 0.8 + 0.2 × the menu's `visibility`, and sizes each button 20 times that factor, as the menu
places and sizes its own buttons' backgrounds, so they grow from 80% and move out with the ring over the first frames.
Icons stay 16 by 16, centred on their buttons, as the menu draws its own (`drawIcons`). The sliders and their headings
move with the factor and keep their size, as the menu's texts do.

Each slider shows its speed in blocks a second, to two decimals at most ("5 blocks/s" under "Build speed", "3.33
blocks/s" under "Break speed" at first), so each step reads differently. Its steps are the tick gaps 20 to 1 from left to right (1 to 20
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
Copy and "Material list" start a box (see "Boxes for Copy and the material list"), Delete starts or ends Delete (see
"Delete"), Paste brings back the most recent copy (see "Copy and paste"), "Material lists" opens the current material
list (see "The Material lists button").

The buttons sit in the columns of the wrench, undo and redo, as Geo arranged them (comment c2 on 815 revision 2):
"Show or hide ghost blocks" under the wrench, 13 below the middle; Copy, Paste and Delete in the row above the wrench,
39 above the middle; and "Block mix", "Material list" and "Material lists" in the row above that, 65 above it. Building
Planner adds no settings button to the radial menu (1241 Q1; see "Settings"). Under the wrench, Effortless Building shows its "Functional Settings" button
to operators, which changes its settings on the server; this server doesn't run Effortless Building, so during a
session hook E34 leaves that button out, for everyone, and "Show or hide ghost blocks" takes its place. They meet
nothing the menu draws at 854 by 480, GUI scale 2, and at 1920 by 1080, GUI scales 2 and 4.

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
unlike a tool such as the pickaxe, which would suggest breaking real blocks.

### Keys

Building Planner's keys, each changeable in Controls under the heading "Building Planner": "Block mix" (B) and "Show or
hide ghost blocks" (N). Neither is a default key, or part of a default key combination, of Minecraft 26.3, Litematica,
MaLiLib, Effortless Building 4.3 or Freecam 1.5.1. Copy, Delete, the material list, Paste and the material lists have
buttons in the radial menu and no keys. As proposed for 0.11.0, a third key, "Saved copies" (V), opens the "Saved
copies" screen, which has no button (see "Saved copies").

The copy at the aim is worked by the mouse and by eight keys of its own, under the same heading: "Lock or unlock"
(H), "Nudge forward" (Up), "Nudge back" (Down), "Nudge left" (Left), "Nudge right" (Right), "Nudge up" (Page Up),
"Nudge down" (Page Down) and "Change copy mode" (R), Satisfactory's keys for the same controls. R acts only while the
copy is at the aim (see "Copy and paste"); H and the nudge keys act then, and in every build mode but Disable while
the lock's controls are active (see "Locking and the first click"). Geo approved the first seven names (1155);
and "Change copy mode" for 0.7.0 (1313), since R changes only the copy's mode. The ids stay `key.buildingplanner.lock_copy`, the `nudge_*` ids and `key.buildingplanner.copy_mode`.
Esc's steps (see "Esc") take the game's Esc, which closes screens and pauses the game, and which Controls doesn't
list, so players can't move them to another key.
Lining the copy up, turning and flipping the block in hand, and marking in Delete use Minecraft's sprint key (Left
Ctrl), as the copy's clicks and wheel use Minecraft's own
keys, so Controls shows no clash with sprinting. Checked against each mod's defaults:

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
mode", "Lock or unlock", the six "Nudge" keys (back, down, forward, left, right and up), "Open Modifier
Settings", "Open Radial Menu", "Redo", "Show or hide ghost blocks" and "Undo", with "Saved copies" after "Redo" as
proposed for 0.11.0.

### The block mix and its panel

The mix is a list of up to nine blocks with a weight each, kept on the player's computer while the game runs: it
starts empty and off at each launch, and stays across servers. It applies while it's on, Building Planner has a
session, the main hand holds a `BlockItem`, and Effortless Building's mode isn't Disable. While it applies, hook E5
gives each shape position its block, chosen by a fixed hash of the world position over the weights, so the preview and
the layer agree, and hook E6 makes the preview draw those blocks. When it doesn't apply, shapes use the held block, as
in Effortless Building, and in Disable every click is vanilla's, with a right click turned while Left Ctrl is held
(see "Turning real blocks in Disable").

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
placement list, as for tiles. As proposed for 0.11.0, Building Planner also saves each copy in a file of its own on the
player's computer (see "Saved copies").

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
sits there (`CopyPlacement`). A turn or a mirror keeps the copy's middle block where it was, and the copy then rests
against the aimed face again, unless it's locked (below), when it spins in place.

**Snapping.** Every position is on Minecraft's block grid, so the copy always lines up with blocks, as Satisfactory's
buildings line up with the grid on a foundation. Beyond the grid, the copy snaps to the player's own pastes, as a
Satisfactory foundation snaps edge to edge to the foundation it's aimed at (806 Q3 C): while the aimed block lies in the box of
one of the player's 16 most recent pastes in this dimension (`PasteBoxes`, whatever has happened to their blocks since;
each copy of a Line, Wall or Floor set counts as a paste of its own),
the copy sits flush against the face of that box the player aims at, without overlapping it, with its edges in line
with the box's edges at the corner nearest the aim. The most recent of those pastes counts when the aimed block lies in
several. "Nearest" compares the aimed block with the box's two edges along each side of the face, and a tie takes the
lower coordinates. While the player holds Minecraft's sprint key (Left Ctrl unless they change it), which is the key of
Satisfactory's "Snap To Guide Lines", the copy stays in line with the most recent paste: on its level and on its row or
its column, whichever the aim is nearer, moving only along that line as the aim moves. Nearer compares the copy's middle
with the paste's: a row runs east to west and a column north to south, and a tie takes the row. On a row or a column,
the copy's edge lines up with the paste's on the side the copy's middle is on, so a copy as wide as the paste lines up
with both its edges. The copy goes where the aim and the flush snap put it, and then into line, so in line it can
overlap the paste.
The sprint key still sprints, as the copy's other controls go by the game's own keys (see "Keys"), so a player who walks
forward holding it also sprints, and in Freecam the camera flies faster. Leaving the server or changing dimension
forgets the pastes.

**Controls.** With the copy at the aim:

- A right click (M2) pastes it, as a right click places blocks in the build modes (Geo's comment c14: "copy should
  place using right click"). Paste sends each block of the copy other than air and structure void as one shape edit,
  at the world position where Litematica draws it, with its state mirrored and turned as the copy is, so the server's
  order, the glow for other players, undo, redo and the block cap work as they do for shapes. The copy's empty space
  leaves the layer's blocks as they are. The copy stays at the aim for more pastes, and a locked copy unlocks once
  pasted, as a build mode's lock ends once its blocks are placed (Geo's comment c5; 1239 Q2 A). With the setting
  "Place real blocks" on (proposed for 0.9.0), the paste places real blocks instead (see "Place real blocks": "A
  paste").
- Each notch of the scroll wheel (M5) gives the copy a quarter turn around its middle: clockwise, seen from above, for
  a notch up, and counterclockwise for a notch down, as Satisfactory's "Rotate Hologram Right" is the wheel up (806 Q2
  B).
- A middle click (M4) flips it left to right as the player's view faces it, around its middle: across the north-south
  line when the view faces north or south, and across the east-west line when it faces east or west. The view is the
  Freecam camera's while Freecam flies it, and the player's otherwise. Satisfactory's middle click samples the building
  aimed at instead, and its build gun has no mirror; the middle click still mirrors here (806 Q4 A), and Minecraft's
  pick block samples whenever the copy isn't at the aim.
- H ("Lock or unlock") locks the copy where it is: it stops following the aim, so the player can walk around it, or fly
  the Freecam camera, to see it from every side, as Satisfactory's "Lock Hologram" does (806 Q5 A). While it's locked, the arrow
  keys move it one block: Up away from the player's view, Down toward it, and Left and Right to the view's sides, along
  the compass direction nearest the way the view faces (the Freecam camera's while Freecam flies it); Page Up moves it
  one block up and Page Down one block down. A key held down moves the copy again after 0.2 seconds, then every 4 ticks
  (5 blocks a second), the game's own pace for a held use button (`KeyRepeat`); the computer's own key repeat, which
  the game counts as more presses, doesn't count while the key stays down. The direction is taken at each move
  (`CopyNudge`), so turning while holding a key turns the way the copy moves. While the copy follows the aim, the
  nudge keys do nothing. Nudging has no distance limit. Pressing H again unlocks the copy, and it follows the aim
  again. Satisfactory's half step (Ctrl with a nudge key) has no equivalent,
  since the copy moves a whole block at a time.
- R ("Change copy mode") switches the copy's mode between Off, Line, Wall and Floor, in that order (Geo's comment c15:
  "Instead of zoop, just call it "Mode" (Off, Line, Wall, Floor)"; below).
- A left click (M1), Esc, or anything else that ends the mode, puts the copy away: Building Planner removes its
  placement from Litematica and keeps the schematic, with its turn and mirror, as the most recent copy. During a set of
  copies, after its first click, a left click or Esc drops the set and leaves the copy at the aim, as Effortless
  Building's left click cancels a shape (1238 Q2 A, Q3 A).

**The copy's mode.** The copy's mode starts as Off each time the game starts and stays as the player last set it
while the game runs, as Satisfactory keeps a build mode until R changes it. Line, Wall and Floor lay copies as
Effortless Building's Line, Wall and Floor lay blocks (Geo's comments c12 and c15), with the copy as the unit: the
first right click fixes the first copy where it is and sends nothing; the copies then follow the aim from it, side by
side, each the copy's own size along each direction (as it's turned) from the one before, so they touch without
overlapping; and the second right click pastes them all as one shape edit, which one undo takes back. The copy then
follows the aim again for a new set.

- Off: each right click pastes one copy, as above.
- Line: a row of copies along whichever of east-west, up-down and north-south from the first copy Effortless
  Building's Line picks for the view (`Line.findLine`, public, with the first copy's middle block as its first
  position): the line through the first copy that the view's line passes nearest. The row ends with the copy that
  holds the block `findLine` gives, so one mode lays a row sideways or up and down.
- Wall: a grid of copies in the upright plane through the first copy that Effortless Building's Wall picks
  (`Wall.findWall`), from the first copy to the copy that holds the block it gives.
- Floor: a grid of copies on the first copy's level, from the first copy to the copy that holds the block Effortless
  Building's Floor gives (`Floor.findFloor`).

Each direction has up to 10 copies, as Satisfactory zoops up to 10, so a Wall or a Floor has up to 100 (1238 Q4
A). `findLine`, `findWall` and `findFloor` use Effortless Building's reach (32 in survival, 200
in creative) and aim from the camera (hooks E25 and E26); where they find no block, the copies stay as they were. A
pure function (`CopyGrid`) turns the block they give into the number of copies along each direction, counting from
the first copy's middle by the turned copy's size. Each copy's box joins the recent pastes the copy snaps to, as a
paste of its own, in the order the copies lie from the first, row by row (961 Q1 A), so a Floor of 4 by 4 counts as 16
pastes, the most the copy keeps. Left Ctrl's line stays with the most recent paste, the last copy.

R during a set, after its first click, switches to the next mode with the same first copy, so a Line can become a
Wall or a Floor without starting again; at Off, the set drops and the copy follows the aim (Geo's comment c7, made of
the one ghost block's Zoop: the key "should still be available" while the length is adjusted).

The other copies are placements of the same schematic, which Litematica draws as it draws the copy and Building
Planner adds and removes as the set grows and shrinks (hooks L4, L5 and L6 treat them as the copy). While a set is
fixed, the wheel and the middle click turn and mirror every copy in it (the first in place, around its middle, and the
others laid side by side again at the turned size). H locks the first copy where it is and the nudge keys move it, and
the rest of the set with it, while the far end keeps following the aim (Geo's comment c5: the portions already locked
in "remain locked, and they're all adjusted together"). A copy locked before the first click starts the set where it's
locked. A set whose blocks together pass the block cap draws red (below), and its click pastes nothing.

**Colours.** The copy's box is drawn by Building Planner through MaLiLib's renderer, as the box for Copy is
(`RenderUtils.renderAreaOutline` and `renderAreaSides`, an outline and see-through sides in the box's colour, as
transparent as that box's: 0.15 alpha, and 0.2 where it is red; `CopyOutline`), in Satisfactory's hologram colours (806
Q7 B): blue (#00D4FF) where a paste would go in as it shows; yellow (#FFE100) where some of its blocks would replace ghost
blocks already there, which the paste still does, as Satisfactory's yellow "soft clearance" still builds; and red
(#FF7070) where the server would refuse the paste: part of the copy outside the world's build height or its border, or
a set over the block cap. While it's red, the controls line says why (a Line, Wall or Floor copy's too, before its
first click) and a right click pastes nothing and sends no
edit. Hook L6 leaves the copy and its set out of Litematica's own placement boxes, as it does tiles. The copy's blocks
draw as Litematica draws them.

The colour is a pure function (`CopyColour`), worked out each frame and at each click. With a set fixed, each copy's
box is drawn, and the whole set has one colour, since it pastes as one edit. Red follows the server's checks in its
order: the set's blocks over the cap, then the build height, then the border. The copy's box is the smallest box
around its blocks, so the set's two far corners stand for its blocks in the height and border checks. Yellow reads the
player's copy of the layer whether ghost blocks show or not, since a paste shows them again and replaces them. A ghost
block the same as the copy's block there doesn't count, since the paste leaves it as it is. Whether the copy is over
ghost blocks is kept until the set's boxes, the copy, its turn or the layer change. A set's first click, which pastes
nothing, still fixes the first copy while the copy is red; the second click is the one that pastes nothing.

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
| Place | Left click | Paste, with the right click, which places blocks in Effortless Building's modes (Geo's comment c14). |
| Back to the build menu | Right click | Put the copy away, or drop a set of copies, with the left click or Esc. |
| Rotate Hologram Right / Left: 90° or 45° steps; foundations 45°, and 5° on a foundation with Ctrl | Wheel up / down | A quarter turn clockwise / counterclockwise; no finer step. |
| Hologram Rotation Mode (Static by default, or facing the player) | Setting | Static only: the copy keeps its turn. |
| Snapping to foundations and walls, edge to edge, and a 1 m grid on a foundation | Automatic | The block grid always; flush to the player's recent pastes. |
| Snap To Guide Lines, and the world grid off a foundation | Hold Left Ctrl | In line with the most recent paste, with Minecraft's sprint key (Left Ctrl). |
| Lock Hologram | H | Lock the copy; H again unlocks it. |
| Nudge Forward, Backward, Left, Right: 1 m | Arrow keys | One block. |
| Nudge Up, Down (since 1.1): 1 m | Page Up, Page Down | One block. |
| Half step | Ctrl with a nudge key | None: a block is the smallest step. |
| Change Build Mode; held, a wheel of modes | R | Off, Line, Wall and Floor, cycled; no wheel. |
| Zoop: first click fixes, aim to extend, second click builds, up to 10 in one line | Left click twice | Line, Wall and Floor: the same with the right click, for copies side by side, up to 10 along each direction. |
| One-Click Build: hold to place a multi-step building (off by default) | Setting | None: two clicks. |
| Sample Building or Blueprint, with its settings | Middle click | Minecraft's pick block outside the mode; in the mode the middle click mirrors. |
| Build menu, Quick Switch, hotbars, Save Hologram to Hotbar, Quick Search | Q, E, 0–9, Left Alt with a number, N | None: one copy at a time; the radial menu (Left Alt) starts Copy and Paste. |
| Dismantle, mass dismantle, Dismantle Filter, Customizer | F, Ctrl, G, X | None for the copy: the break click erases with Effortless Building's shapes, and Delete marks with Left Ctrl and erases with a held left button, as mass dismantle does (see "Delete"). |
| Hologram colours: blue, yellow (soft clearance), red (hard clearance) | Automatic | The copy's box in the same colours. |
| Toggle HUD (build mode's text) | T | Minecraft's F1, which hides the controls line. |

The wiki's pages disagree in places. Its Build Gun page and the 1.1.0.0 notes say nudging has no distance limit, and
its Controls page still says the distance is limited; the copy has no limit. The Build Gun page gives fine rotation on
foundations in 10° steps, and its Foundations and Controls pages give 5°; neither applies here. The Controls page says
pressing H again "will reset the origin", which the copy reads as unlocking. Whether a Satisfactory hologram stays
locked after it's placed isn't stated; the copy unlocks once pasted (1239 Q2 A). Since 1.2.2.2 a "Remember
Snapping Toggle" option keeps "the last used snap mode", which suggests snapping can be toggled, but the wiki doesn't
say how on a keyboard, and its "Hold To Snap Building" setting is for controllers only; the copy's guide lines follow
the key held.

#### Saved copies

Proposed for 0.11.0, not built. Geo asked for it in a comment on 1375 Q1: "I'd like to add a feature to save
previously copied regions, like saved materials lists. Instead of a button next to the radial menu, you would access it
as a new keyboard press that's displayed while you're pasting. It would have a menu like the saved materials lists.
Copies would save automatically, with the most recent 10, but you can view any of those copies in that menu and rename
it, which saves it until you delete it." Geo's answers in the request "Saved copies" (1384) settle it.

**Which copies stay.** Each copy that Copy makes is saved at once as the newest saved copy, named by the date and time
it was made ("Nov 19, 6:21 PM", the pattern "Save list" fills in). The 10 most recent copies the player hasn't renamed
stay, and when Copy makes another, the oldest of them is deleted. A copy the player renames stays until they delete
it and doesn't count among the 10; a player may keep as many as they rename, however large (1384 Q3 A). A paste, a
turn or a mirror changes no saved copy, and a box that Copy refuses saves nothing. The rule is a pure function
(`CopyHistory`, common) of the saved copies, newest first, and each change: a new copy, a rename and a delete.

**Where they're kept.** Saved copies are kept on the player's computer, a set for each Minecraft account, the same on
every server, in singleplayer and in a LAN world, so a copy saved on one server pastes on another (1384 Q1 A). Only the
player at that computer sees them, and another computer doesn't have them. The server stores nothing, so saved copies
work the same on servers with and without Building Planner, and 0.11.0 changes nothing the server does and keeps
protocol version 5.

**The key.** "Saved copies" (V), under the heading "Building Planner" in Controls, opens the "Saved copies" screen
whenever Building Planner has a session and no screen is open, in any mouse mode or none (1384 Q2 A). The radial menu
has no button for it (Geo: "Instead of a button next to the radial menu"). While the copy is at the aim, its controls
line shows the key before the left click's control, "[V: saved copies][Esc/Left-click: close]" (or "cancel" during a
set), naming the key as the player has bound it, and leaves the control out while "Saved copies" has no key. Opening the
screen keeps the mouse mode, as any screen does. V is no default key, or part of a default key combination, of Minecraft
26.3 (the Minecraft Wiki's [Controls](https://minecraft.wiki/w/Controls) page lists V only with F3, for "Dump Version
Info", which matters only while F3 is held), Litematica (`config/Hotkeys.java`), MaLiLib (`MaLiLibConfigs`, whose only
default hotkey is "A,C" for its settings), Effortless Building 4.3 (`utilities/KeyBindings.java`: left Alt, the keypad's
+, Z and Y) or Freecam 1.5.1, and none of Building Planner's own. It sits beside C and B, under the left hand while the
right hand pastes.

**The screen.** "Saved copies" is laid out as "Saved lists" (`SavedListsScreen`): the title; under it the line "Your 10
most recent copies stay here. Rename a copy to keep it."; and a row for each saved copy, newest first, with its name,
under the name its size and its number of blocks ("12 × 5 × 8 · 120 blocks"), and the buttons "Paste", "Rename" and
"Delete". With none, it says "No saved copies yet." Each row is as high as its two lines of text and its buttons, with
the three buttons 50 wide at its right, in a width of up to 400. The mouse wheel scrolls the rows, and "Close" at the
bottom right, like Escape, goes back to the game.

- "Paste" closes the screen and brings that copy to the aim as the radial menu's Paste button brings the most recent
  copy: it ends any mouse mode, and the copy comes unlocked, in the copy mode the player last set, turned and mirrored
  as it was copied. It becomes the copy that the Paste button brings back, and its place in the list stays.
- "Rename" opens "Rename copy", laid out as "Save list": a field "Name" filled in with the copy's name, which the player
  may type over, and "Save" and "Cancel". The field accepts at most 64 characters, "Save" is greyed out while the name
  is empty or only spaces, and spaces at either end are dropped. "Save" makes the copy stay until it's deleted, under the new name or
  the same one; two copies may have the same name. Both buttons, and Escape, go back to "Saved copies".
- "Delete" asks again ("Delete the copy "Hut"?", with "Delete" and "Cancel"), deletes the copy's files, and comes back
  to "Saved copies" either way. A copy at the aim stays there until the player puts it away or brings another.

The screen's texts and the control are in the wording Geo approves in 0.11.0's texts step.

**After a restart.** The copy stays while the game runs (1375 Q1 A), and the saved copies outlast it: with no copy
since the game started, the radial menu's Paste button brings the newest saved copy to the aim, as it was copied, and
it gives the message "Nothing to paste. Use Copy first." only while there are no saved copies (1384 Q5 A).

**The files.** Each player's saved copies are in `<game folder>/building-planner/copies/<player UUID>/`: each copy's
blocks as `<number>.litematic`, in Litematica's format, which `LitematicaSchematic.writeToFile` writes and
`createFromFile` reads, with the copy's region and its structure void as `Clipboard` makes them; and `index.dat`,
gzip-compressed NBT:

- `version`: the format's version, 1.
- `next`: the number the next copy's file takes, so that no number is used twice.
- `copies`: each saved copy, newest first, with `file` (its number), `name`, `time` (when it was copied, in
  milliseconds since 1970), `kept` (1 once it's renamed, and 0 before), `size` (x, y and z) and `blocks`.

One background thread reads and writes the files, in order, each write through a temporary file and a rename: a copy's
file as Copy makes it, the index after each change, and the file of a copy that's dropped or deleted removed
once the index without them is written. It reads the index as the game starts, and a copy's file when the player picks
it, which then comes to the aim. A file that can't be read is logged as an error and renamed with `.unreadable` at its
end: for the index, the game starts with no saved copies and leaves the copies' files in place; for a copy, it drops
from the list, and the player gets the message "That copy couldn't be read.". A copy has at most the block cap's blocks
(200,000 for an edit), since Copy refuses more, and its file is about the size of Litematica's own file of the same
box.

`SavedCopies` (client) owns the list and the background thread, outside the session, so the list stays across
servers as the copy does; `SavedCopiesScreen` and `RenameCopyScreen` are the two screens; and `ControlsLine.copy` adds
the key's control.

### Locking and the first click

In each of Effortless Building's modes but Disable, the block the first click starts from shows at the aim before
that click, and H locks where that first click goes (Geo's comment c4: "make the other build modes work like that to
more easily indicate where the start of that build mode will be placed"; c5: "locking and nudging should work for the
other build modes as well for the first click placement"). The lock sets only positions: Effortless Building's clicks
stay its own, so a right click places in Single and sets a shape's next position in the other modes, a left click
erases in Single and starts an erasing shape in the others, and its shapes, mirror, radial mirror, array, block mix
and undo work as they do without the lock (c5: "the lock is just about setting the position of that first item; the
rest of what you can do should continue to work normally"). Single places one ghost block a click, as Effortless
Building's Single does; Line lays a row of blocks along any of the three directions, up and down into open air too,
so Single has no row modes of its own (Geo's comment c6; 1238 Q1 A). Left Ctrl doesn't
line the block up: it turns and flips the block instead (below), in every one of Effortless Building's modes (1009 Q2
C), and in Disable the real block a right click places while Left Ctrl is held (1029 Q1 A, see "Turning real blocks
in Disable").

The lock's controls are active while Building Planner has a session, Effortless Building's mode isn't Disable, no
mouse mode is on, and the main hand holds an item that places a block (`ClickRule`, as for shapes). The turning
controls (Left Ctrl with the wheel and the middle click) are active under the same conditions in every mode, while no
shape drawn with the break click is under way (`getBuildState()` isn't `BREAKING`), since that shape erases
(`SingleRule.turning`); in Disable they act on the real block a right click places. A client class, `ShapeLock`,
stores the lock's state: the step it's for (the first click, or a shape's second or third position), whether it's on,
and the locked position, with, for the first click, the face and hit point aimed at when it was locked. The positions
the shape's clicks have set stay Effortless Building's own (`firstBlockEntry`, and `secondBlockEntry` in the modes of
three clicks). A second client class,
`HeldTurn`, stores the turn and the item it was set for, since the turn reaches every mode. Leaving the server,
changing dimension, choosing a mode in the radial menu (E21), Esc's step that leaves the mode (see "Esc") and any
mouse mode starting end the lock. The first click's block has Effortless Building's reach (32 in
survival, 200 in creative).

**The first click's blocks.** Before the first click, Effortless Building's preview draws the block where a right
click would start, at the position its first click takes (`BuildPipelineClient.resolveFirstClickPos`: beside the
aimed face), turned as it will be placed (hook E28). Effortless Building's preview skips a set of one block while no
shape is under way, so hook E30 lets it draw this one in every mode but Disable, with the mix's first block when the
block mix is on. With the mirror, radial mirror or array on, the set has more than one block and Effortless
Building's preview draws it as it is. While the player aims at nothing within reach, nothing shows.

Beside it, the block a left click would start from shows in Effortless Building's own look for a shape drawn with the
break click, red sides with a checkered texture and a red outline (Geo's comment c7 on 1359: "before the first click,
when the user hasn't decided whether they're going to use delete or place yet, then the block you're looking at will
have the red frame block around it (the same thing that shows once first left clicking to start specifying an erase)
at the same time as the hovering placement block frame"). That block is the aimed one, a ghost block or a real block
by hook E9's aim, since Effortless Building starts a shape drawn with the break click on the aimed block
(`resolveFirstClickPos` returns the hit block for `BREAKING`), with its copies by the mirror, radial mirror and array
(`BuildPipelineClient.CLIENT.processBlocks` on a `BREAKING` set). Hook E38 draws it after the preview with Effortless
Building's own drawing (`renderBoundingBoxFaces`, `renderEdgeQuads`). In Single, a left click on a real block erases
nothing, so there the red frame shows only on a ghost block, and in the other modes on any aimed block, where the
erasing shape starts ("Yes, but on ghost blocks only, since single does erase ghost blocks", Geo, 1369 Q1). While the first click's position is locked, both clicks start at the locked position,
which shows the copy's box (see "Colours"), so no red frame shows.

**Locking and nudging.** H ("Lock or unlock") locks the position the aim is setting at that moment, for the step
under way (Geo's comment c3 on 1359): before the first click, the first click's position (980 Q4 A); during a shape,
the position the aim sets for its next click, such as a wall's far corner, which sets the wall's length and height, or
a cube's height. With nothing aimed at, H does nothing. Locked, the position stays where it is while the player walks
or flies around it, and the arrow keys, Page Up and Page Down nudge it a block at a time, as they nudge the copy: along
the compass direction nearest the view (the Freecam camera's while Freecam flies it), with the copy's repeat for a held
key (`CopyNudge`, `KeyRepeat`) and no distance limit. H again unlocks it, and the position follows the aim again.

Before the first click, the first click, right or left, starts at the locked position whatever the player aims at:
hook E32 gives Effortless Building's click and preview the locked position, and its aim the hit the position was
locked with, moved by the nudges. In Single, a right click places one ghost block there and a left click erases
there, each with Effortless Building's mirror, radial mirror and array. A locked block's state comes from its
placement rules with the face and hit point aimed at when it was locked and the facing of the player (or the camera)
at each click, then the turn (below).

Each click sets its step's position, as without the lock (c3: "Once you click to set the position of it, that
effectively "locks" it, anyway"), and the next step follows the aim until H locks it ("for the next step, pressing lock
should "lock" the placement of the next hover in that step"). While a step of a shape is locked, hook E37 gives
Effortless Building the locked position in place of the one its aim finds, so the preview, the click that sets the
step and the finished shape take it. A nudge along a direction the step sets moves the step's position, which changes
the shape's size ("for the second step of placing a wall, nudge will nudge the length and height of the wall"). A
nudge along a direction the step doesn't set moves the whole shape: every position the clicks have set, the first
click's hit with them (hook E33), and the step's position ("shifting perpendicular to a wall during the second step of
a wall placement, which would shift the entire wall that way"). The directions each step sets come from how
Effortless Building finds its position:

- Line's second position (`Line.findLine`): along the line, or every direction while the line is one block long;
- Wall's second position (`Wall.findWall`): up and down, and along the wall, or both level directions while the wall
  is one block wide;
- the second position of Floor, Cube, Circle, Cylinder, Sphere, Diagonal Line, Diagonal Wall and Slope Floor
  (`Floor.findFloor`): both level directions;
- the third position of Cube, Cylinder, Sphere, Diagonal Line, Diagonal Wall and Slope Floor (`findHeight`): up and
  down.

Moving the whole shape, rather than the step's position alone, keeps the shape as it was. Effortless Building reads a
wall's second position moved across the wall as a turn: `Wall.getWallBlocks` makes a wall along the Z axis where the
two positions share their X, and otherwise a wall along the X axis at the first position's Z. It reads a line's the
same way (`Line.getLineBlocks` takes the X axis where the X differs, then Y, then Z). Moving the far corner alone
would turn the wall or the line rather than shift it.

The lock ends once the click that finishes a shape has placed it (Geo's comment c5 on 815 revision 2: "Once blocks
have been placed after a lock, the block should no longer be locked"): in Single after each right click, and in the
other modes after the shape's last click. A click that erases ends the lock too (1239 Q1 A). Cancelling a shape with
Effortless Building's left click keeps a position locked before the first click, so the player can start again from
it. Hold to repeat in Single places the first block at the locked position and the rest where the player aims, since
the lock ends with the first.

**Colours.** While a position is locked, before the first click or for a shape's step, the copy's box shows around
it (980 Q7 A), drawn as the copy's is (`CopyOutline`, with `CopyColour` over 1×1×1 spots): blue where the block
would go in as it shows, yellow where it would replace a different ghost block, and red where it's outside the world's
build height or its border, where Effortless Building's constraints leave the block out, so the click places nothing
there.

**Controls line.** While the lock's controls are active, with no screen open and the HUD shown, a line of each mode's
controls shows where the mouse modes' lines show, in the same rectangles (`ModeHud`, `ControlsLine.shape`), with the
keys as the player has them bound (980 Q8 A), before the first click too (Geo's comment c17: "you should always see
control hints for left click and right click for the regular build modes, even if you haven't placed anything yet").
It names only the controls that work at that moment. These are the texts, in Geo's form for the controls lines, as
Geo approved them (1313):

- Single: "[Right-click: place][Left-click: erase][Hold Left Control: turn or flip][H: Lock (Off)][Esc: exit mode]";
- the other modes before the first click: "[Right-click: start placing][Left-click: start erasing][Hold Left Control:
  turn or flip][H: Lock (Off)][Esc: exit mode]";
- locked before the first click: "[Right-click: place][Arrow keys + Page Up/Down: nudge][Left-click: erase][Hold Left
  Control: turn or flip][H: Lock (On)][Esc: exit mode]", with "start placing" and "start erasing" in the other modes;
- while Left Control is held, "[Scroll: turn][Middle-click: flip]" in place of "[Hold Left Control: turn or flip]";
  for a block that only turns, "[Hold Left Control: turn]" and "[Scroll: turn]"; for one that only flips, "[Hold Left
  Control: flip]" and "[Middle-click: flip]"; for one that does neither, no turning control;
- a turned or flipped block changes no line: its preview shows the turn;
- while the locked position is red, the reason in red, "[Can't place: outside the world]", in place of "[Right-click:
  place]" or "[Right-click: start placing]".

During a shape, Effortless Building draws its own line, in Building Planner's rectangles (hook E31): "[Esc/Left-click:
cancel][Right-click: place]" during a shape drawn with the place click, 58 above the screen's bottom
(`RenderHandler.renderSubtitle`), and the shape's block count in the action bar just above it
(`RenderHandler.updateFeedback`, which calls `sendOverlayMessage`). Building Planner's line then shows the lock and
the turning controls only: "[Hold Left Control: turn or flip][H: Lock (Off)]", and while the step under way is
locked, "[Arrow keys + Page Up/Down: nudge][Hold Left Control: turn or flip][H: Lock (On)]"; after each click, the
next step's line starts at "[H: Lock (Off)]"; during a shape drawn with the break click, whose
turning controls aren't active, "[H: Lock (Off)]" or the locked line without the turning control. The game draws the
action bar's text from 72 to 63 above the screen's bottom (`Hud.extractOverlayMessage`), so while Effortless
Building's line shows, `ModeHud` draws Building Planner's line above the action bar, its last row ending 2 pixels above
it, and both of Effortless Building's texts stay readable.

**Turning and flipping.** The wheel and the middle click work as in Minecraft, changing the hotbar slot and picking a
block (980 Q5). While the player holds Minecraft's sprint key (Left Ctrl unless they change it) and the turning
controls are active, they turn and flip the block in hand instead, so the player can place it facing any way wherever
they stand (980 Q5 and Q6, Geo's answers), in each of Effortless Building's modes (1009 Q2 C), Disable's as
"Turning real blocks in Disable" describes:

- Each notch of the wheel (M5) gives the block a quarter turn: clockwise seen from above for a notch up, and
  counterclockwise for a notch down, as the copy turns. The turn is Minecraft's own for a turned structure
  (`BlockState.rotate`): a stair's or a chest's facing, a log's axis between east-west and north-south, a sign's
  rotation.
- The middle click (M4) flips the block to the next of the other ways it can sit, in a fixed order, back to the first
  after the last (`BlockTurn.flips`): a log's or a chain's axis up and back; a stair's or a trapdoor's half and a
  slab's top or bottom (never double); a piston, an observer or a dispenser facing up, down and back to its turn; a
  button or a lever on the floor, a wall or the ceiling; a lantern hanging or standing.

The first turn or flip starts from the block as Minecraft would place it at that moment: by its placement rules with
the hit the first position was locked with, the shape's first click, or the aim, as the preview shows it where it
draws the block (see "The first click's blocks"). From
then on, the block keeps that orientation whichever way the player faces and whichever face they aim at, until they
switch to another block (980 revision 2, Q1 A): the main hand's item becoming another item, by the hotbar or by picking
a block, drops the turn, and so does switching to another block and back; the same block in another hotbar slot
keeps it. The turn is kept as the values of the block's orientation properties (facing, axis, rotation, half, slab
type, attached face, hanging), and `BlockTurn.apply` sets each on every block placed that has that property, after
the placement rules, which still set the rest (waterlogging, a fence's connections). With the block mix on, each of
the mix's blocks takes the properties it has.

Every mode's shapes and their preview take the turn (1009 Q2 C): Single's one block, and the shapes of
Line, Wall, Floor, Cube, Diagonal Line, Diagonal Wall, Slope Floor, Circle, Cylinder and Sphere, the modes Effortless
Building 4.3 offers besides Disable and Single (`buildmode/BuildModeEnum.java`, where Pyramid, Cone and Dome are
commented out). Each of these modes gives every block of its shape the held block's placement rules with the first
click's face (`BuildPipelineClient.getFirstClickHit`), then each entry's mirror and turn from the modifiers
(`BlockEntry.applyTransforms`), in the preview (`BlockPreviewRenderer.render`) as in E1's states
(`EffortlessShapes.place`). The turn goes between the two: E1 applies `BlockTurn.apply` to each state after its
placement rules and before the entry's transforms, and E28 to the state the preview starts from, so a mirrored copy
of a turned stair faces the mirrored way, as the preview shows it. The turn stays as the player changes mode, Disable
among them, until they switch to another block (980 revision 2, Q1 A). The player may turn the block before a shape's
first click or between its clicks; the preview shows each turn at once (before the first click, on the first click's
block), and the shape places with the turn it has at its last click. A turn changes a shape's states and never its
positions: no mode reads the held block's state (the only
block state read under `buildmode/` is the world's, in `BuildSettings.java`).

In Disable the turning controls act on the real block a right click places, as "Turning real blocks in Disable"
(below) describes.

Left Ctrl takes the wheel only while the block in hand (or a block of the mix) is one a turn changes, and the middle
click only while it's one that flips. With a stone block in hand, the wheel changes the hotbar slot and the middle
click picks a block, Left Ctrl or not; with a chest, which turns but doesn't flip, the middle click with Left Ctrl is
Minecraft's, which in creative mode picks a block with its contents. Effortless Building 4.3 handles the wheel and
Ctrl only in its screens (its jar's classes searched for those calls), so neither clashes with its controls. Litematica's pick block stays out while the middle click flips (hook
L11). The sprint key still sprints, so a player who walks forward while turning the block also sprints.

#### Turning real blocks in Disable

In Disable a right click places a real block as Minecraft does, and the server decides its state from the face aimed at
and the player's facing. Left Ctrl turns and flips the real blocks placed there too, only while Left Ctrl is held,
and the player sees the turned block before it goes in (1029 Q1 A).

Effortless Building draws no preview of the clicked block in Disable: its preview gives Disable one block at the aim
(`BuildPipelineClient.getPreviewBlocks`) and draws no set of one block while no shape is under way
(`BlockPreviewRenderer.render`), and its longer reach skips Disable (`MixinGameRenderer`), so only Minecraft's outline
of the aimed block shows, at Minecraft's reach. With Effortless Building's mirror or array on, its preview in Disable
draws the copies they would make, though Disable places only the clicked block (`shouldInterceptPlacing` is false in
Disable, so the click is Minecraft's). Neither Effortless Building's line nor a line of Building Planner's shows.
Minecraft 26.3 reads the Control key for the drop key (Ctrl+Q drops the stack) and the pick block key (in creative, a
block with its contents), and not for a right click (`Minecraft.startUseItem`, `MultiPlayerGameMode.useItemOn`).

Disable's turning controls are active while the turning controls would be in another mode (see the start of "Locking
and the first click"), with a block in hand that turns or flips, or one whose right click places a block that does (a torch
aimed at a wall, which places a wall torch):

- Holding Left Ctrl shows the block where a right click would place it, in the state it would place, drawn as
  Effortless Building's preview draws a block in the other modes (below). The wheel turns it and the middle click
  flips it, as in the other modes. The turn is the one `HeldTurn` stores, so a turn made in Line shows here, and one
  made here carries to Line.
- A right click while Left Ctrl is held places the real block in that state. Holding the right button repeats as in
  Minecraft, every 4 ticks, and each block takes the turn.
- Without Left Ctrl, Disable is Minecraft's: the wheel changes the hotbar slot, the middle click picks a block, and a
  right click places the block as Minecraft would. The turn waits for the next press of Left Ctrl or the next mode.
- A right click on a block that has a use of its own (a chest, a door, a lever) uses it, as in Minecraft, unless the
  player sneaks. The preview shows the block beside it all the same.

**How the turned block is placed.** The client sends the server the turn before the click that uses it, and both
sides apply it at the same step of Minecraft's own placing, so the client's guess is the server's block:

- `place_turn(item, properties)`, client to server: the item the turn is for and the turn's property values (each
  name and value, as `HeldTurn` stores them), or no item to clear the turn. Hook M6, at the head of
  `MultiPlayerGameMode.useItemOn`, which a right click on a block calls before it predicts the block and sends
  `ServerboundUseItemOnPacket`, sends `place_turn` when the turn this click takes differs from the last one sent: the
  turn of the block in hand while Left Ctrl is held in Disable, the ghost block's turn where the setting "Match ghost
  blocks" gives one (see "Settings"), and none otherwise (`DisableTurn`). Messages on one
  connection keep their order, so the server has the turn before it handles the click. It stores the last one for
  each player (`PlaceTurns`) until the player leaves, and accepts only the orientation properties `BlockTurn` sets.
- Hook S2, `@WrapOperation` on `BlockItem.place`'s call to `getPlacementState(BlockPlaceContext)`, which
  every block item's right click goes through, including the items that override `getPlacementState` (a torch's, a
  sign's, a bed's): when the context's player has a turn for the item in the main hand, S2 sets the turn's properties
  on the state (`BlockTurn.apply`), lets the neighbouring blocks set its shape again
  (`Block.updateFromNeighbourShapes`, so a turned stair joins the stairs beside it as Minecraft's would), and checks
  that the turned block can stay (below). A block of two parts (a door, a tall plant, a bed) keeps its shape, since
  its other part isn't placed yet and the shape update would take it for gone; a block the shape update changes into
  another, such as air, can't stay. On the server it reads `PlaceTurns`, and on the player's computer the turn
  M6 sent last. The rule is one pure function (`PlaceTurn`), beside `BlockTurn`.
- Minecraft's client runs the same `BlockItem.place` on its own copy of the world to predict the block
  (`MultiPlayerGameMode.performUseItemOn`, inside `startPrediction`), so S2 turns its guess the same way. The server's
  answer, a `ClientboundBlockUpdatePacket` for the clicked position and the one beside it and the acknowledgement of
  the click's sequence (`ServerGamePacketListenerImpl.handleUseItemOn`), then matches the guess, and the block doesn't
  flicker.
- The server sets the block with Minecraft's own `setBlock`, so every player near it sees it turned, with or without
  the add-on. A turned real block fills a ghost block turned the same way, as any real block does (see "Ghost blocks
  the real world fills").

| # | File, method | Hook | Purpose |
|---|---|---|---|
| M6 | `net/minecraft/client/multiplayer/MultiPlayerGameMode.java`, `useItemOn(LocalPlayer, InteractionHand, BlockHitResult)` | `@Inject` at head | Send `place_turn` when the turn the click takes has changed, before the click's prediction and packet. |
| S2 | `net/minecraft/world/item/BlockItem.java`, `place(BlockPlaceContext)` | `@WrapOperation` on the call to `getPlacementState`, on both sides | Turn the state the click places, set its shape from its neighbours, and return null when the turned block can't stay. |
| E29 | `buildpipeline/BuildPipelineClient.java`, `getPreviewBlocks` | `@ModifyReturnValue` | In Disable, while Disable's preview shows, a set of one block where a right click would place, marked missing, so it draws red, when the block wouldn't go in; or null when it wouldn't place at all. |
| E30 | `render/BlockPreviewRenderer.java`, `render` | `@ModifyExpressionValue` on the first `BlockSet.size()` | Draw a set of one block, which the method skips while no shape is under way: Disable's, and the first click's block in every other mode while the lock's controls are active, at the aim or at the locked position (`ShapeLock.drawnBeforeFirstClick`, see "Locking and the first click"). A set of more than one block, such as the copies of the mirror, radial mirror and array, is drawn by the method as it is, so nothing is drawn twice. |

S2 is the server's second hook, after S1 (see "Ghost blocks the real world fills"), in the mixin config's common
section, since both sides place blocks through `BlockItem.place`. M6 is in the client section, beside M2.

**When the turned block can't stay.** Minecraft's `getPlacementState` checks the state it would place: that it can
stay where it goes and that no entity is in its way (`BlockItem.canPlace`). S2 checks the turned state again the same
way, so a wall torch turned away from its wall or a lantern turned to hang under nothing fails. A bed places its head
toward its facing (`AbstractBedBlock.setPlacedBy`, which checks nothing), and Minecraft checks that space only for the
player's facing (`getStateForPlacement`), so S2 also checks that the space toward the turned facing can take a block
and is inside the world border. A turned block that fails places nothing, and the player keeps the item (1033 Q1 A).
The preview draws it red, as Effortless Building draws a block
the player lacks: tinted red with a red outline (hook E29, which marks that one position missing after hook E8
clears the tracker).

Blocks of two parts turn whole. A door's upper half takes the placed half's state (`DoorBlock.setPlacedBy`), and so
does a small dripleaf's top; a bed's head takes the foot's facing. A tall plant neither turns nor flips, so it places
as in Minecraft.

**The preview.** Effortless Building's preview draws Disable's block, with its see-through block, checkerboard sides
and white outline, as in the other modes. While Left Ctrl is held in Disable with a block that turns or flips, hook
E29 gives the preview one block where a right click would place it, from Minecraft's own aim (`Minecraft.hitResult`,
at Minecraft's reach): the aimed block when it can be replaced, and the space beside the aimed face otherwise
(`BlockPlaceContext.getClickedPos`, after the item's `updatePlacementContext`). Hook E28 gives that block the state S2
would give it (`EffortlessShapes.disableShown`, through S2's own step, `PlaceTurns.place`), the turned state from
Minecraft's own `BlockItem.getPlacementState`, so a torch aimed at a wall shows as a wall torch. The block draws red
where the turned block can't stay, where Minecraft's own right click would place nothing, and where an entity is in
the turned block's way. E30 lets the preview draw a set of one block; with no shape under way, it
shows no block count and plays no sound (`RenderHandler.updateFeedback`). Nothing shows while the aim isn't a block,
or where the block can't go at all (`BlockPlaceContext.canPlace`). Effortless Building's mirror and array draw no
copies while Disable's preview shows.

**Controls line.** In Disable, while the turning controls are active, a line of them shows in the place and
rectangles of the turning line (`ControlsLine.disable`), with the sprint key as the player has it bound, in the
turning line's words and Single's line's "[Right-click: place]". These texts follow Geo's form for the controls lines
(inbox request 1155), with no label (1313 [c1]):

- without Left Control, the turning line: "[Hold Left Control: turn or flip]", or "[Hold Left Control: turn]" or
  "[Hold Left Control: flip]", since a right click then places as in Minecraft (1033 Q2 A); no
  line while the block a right click would place hangs on the block behind it, as Minecraft's rule for that block
  requires (a ladder; a torch, sign, banner, button, lever or coral fan on a wall; a tripwire hook; cocoa;
  `DisablePreview.hangs`), though Left Control still turns it (1037 Q1 B);
- while Left Control is held, "[Right-click: place][Scroll: turn][Middle-click: flip]", or with the one of "[Scroll:
  turn]" and "[Middle-click: flip]" the block has;
- while the turned block can't stay, the reason in red, "[Can't place here]", in place of "[Right-click: place]".

**Freecam.** Minecraft aims from Freecam's camera while it's on (`Minecraft.pick` traces from `getCameraEntity`), so
the preview goes where the camera aims. With Freecam's "Allow interaction" and "Player control" off, the click
doesn't reach the world, so no preview shows (`Aim` reads both by reflection; `DisablePreview.interactionBlocked`).
With it on, the right click goes to the server as the player's, and the server places only where the aimed block's
box is within the player's block interaction range and one block more of the player's eyes
(`Player.isWithinBlockInteractionRange`), so the preview shows only where the player, not the camera, is within that
reach (`DisablePreview.withinReach`). The turn sets property values, so the block faces the same way from the camera as from the player.

**Without the session.** On a server without Building Planner, or with another protocol version, Building Planner has
no session, so in Disable Left Ctrl, the wheel and the middle click work as in Minecraft, M6 sends nothing, and S2
changes nothing. The player's computer turns its guess only during a session, so its guess never differs from the
server's block. A player without the add-on places blocks as in Minecraft, and sees other players' turned blocks as
any blocks. `place_turn` is one more payload for the client's check at joining (see "When the server lacks the
mod"). It is part of protocol version 5, which no release has used.

### Settings

Effortless Building's settings screen (`ClientConfigScreen`), which it names "Visual Settings", is named "Settings",
and so is the radial menu's button that opens it (Geo's comment c4 on 1359: "This should now just be renamed to
"settings""). Hook E39 gives the screen's title and the button's name Building Planner's text
(`buildingplanner.ui.settings`) in place of Effortless Building's two keys (`effortlessbuilding.screen.client_config`
and `effortlessbuilding.action.open_client_config`, both "Visual Settings"), on any server, as the screen's rows show
on any server. The button's description, which Effortless Building leaves empty, stays empty.

"Match ghost blocks" is a row Building Planner adds to that screen, which the radial menu's "Settings" button opens
(1244 Q1 B). Geo wants the setting with Effortless Building's settings (1241 Q1: "I meant the game options for
effortless building, which you have to get to through the pause or main menu's settings button"), and this is
Effortless Building's only settings screen; Mod Menu, which the pack doesn't have, would open it too. The row comes after Effortless Building's own rows
and is drawn as they are, a setting that is on or off, labelled "Match ghost blocks", with the tooltip "Automatically
flips and turns a block you place to match a ghost block you're filling in" (Geo's wording, 1313). It is off at first. The
screen's "Save" button saves it with Effortless Building's own settings, in `config/buildingplanner-client.properties`
(`match-ghost-turn`, `false` at first, which a missing or other value reads as), and "Cancel" leaves it as it was. The
row shows on any server; the setting acts only during a session.

The screen lays out its rows, draws their labels and shows their tooltips from one list of label keys
(`ClientConfigScreen.LABEL_KEYS`), each tooltip from its label's key with `.tooltip` added, so hook E36 adds the row's
key to that list where `init`, `extractRenderState` and `renderRowTooltips` read it (`VisualSettingsRow`). The screen
then places one row more, centred as before; E36 adds the row's switch where the screen puts its own, with the
screen's own "ON" and "OFF" (`onOff`), moves "Save" and "Cancel" a row down below it, and saves the switch in
`save`.

With "Match ghost blocks" on, a real block placed into a ghost block of the same block takes the ghost block's
orientation, regardless of how the player is standing (Geo's comment c9: "stairs being placed correctly, according to
the ghost block"), while ghost blocks show (1313 Q1 A). With it off, or with ghost blocks hidden, a block faces as the
player does, as in Minecraft. Real blocks go in with a right click in Disable, so the setting acts
there: while ghost blocks show, when a right click would place a block (at `BlockPlaceContext.getClickedPos`, after the item's
`updatePlacementContext`, as Disable's preview finds it) where the player's copy of the layer has a ghost block, and
the block Minecraft would place there is the ghost block's block (a torch aimed at a wall places a wall torch, so it
matches a ghost wall torch and not a ghost torch), hook M6 sends the ghost block's values of the orientation
properties `BlockTurn` sets (facing, axis, rotation, half, slab type, attached face, hanging) as the click's turn,
through `place_turn`, and hook S2 places the block with them, as it places a turn made with Left Ctrl. The ghost
block's turn takes the place of Left Ctrl's for that click, and Disable's preview, while Left Ctrl is held, shows the
ghost block's turn there. M6 sends the ghost block's turn only when the turned block can stay (`PlaceTurn`'s check, on
the player's computer); otherwise the click places as it would with the setting off. A block placed so fills the ghost
block (see "Ghost blocks the real world fills"). The rule is a pure function (`GhostMatch`), beside `DisableTurn`.
The setting changes nothing on the server and no payload, so it works with protocol version 5.

The row "Place real blocks", proposed for 0.9.0, follows "Match ghost blocks" in the same screen (see "Place real
blocks").

### Place real blocks

Proposed for 0.9.0, not built. Geo asked for a setting "Place real blocks" (comment c4 on 1359: "I also want a setting
for "place real blocks""), weighed against Building Planner working from the player's computer alone on other servers,
and picked "Yes, on servers that run Building Planner: the server places the whole shape" (1368 Q2 A). It is an
exception to Geo's rule that Effortless Building's shapes, mirror, repeat and Randomizer edit only ghost blocks, never
real blocks (402 r2, c1), and it holds only while the setting is on. Geo settled how it works in the request "Place
real blocks: open choices" (1372).

**The setting.** "Place real blocks" is a row of "Settings" after "Match ghost blocks", drawn, explained and saved as
that row is (hook E36's list of label keys gains its key; `VisualSettingsRow`): off at first, saved in
`config/buildingplanner-client.properties` as `place-real-blocks` (`false` at first, which a missing or other value
reads as). Its tooltip is in the wording Geo approves in build step 5. The row shows on any server. The setting acts
while Building Planner has a session and the server takes the payload `place` (`ClientPlayNetworking.canSend`): with
it on, a server with 0.8.0 gets ghost blocks and a server without Building Planner gets nothing, as with it off. On a
server without Building Planner, as proposed for 0.10.0, shapes draw ghost blocks with it on (see "Servers without
Building Planner").

**What a shape does.** While the setting acts, in every build mode but Disable, with an item that places a block in
hand, the place (right) click's shape places real blocks instead of ghost blocks: the shape with Effortless
Building's mirror, radial mirror and array, and with the block mix's blocks while the mix applies, each block in the
state the preview shows (the placing rules with the first click's face, the turn of the block in hand, the mirror's and
the array's turns, and, with "Match ghost blocks" on while ghost blocks show, the turn of a ghost block of the same
block that it fills, by `GhostMatch`'s rule). Single places one real block a click, and holding the button repeats at
the build speed. Disable stays as it is: its right click is Minecraft's already. A paste places real blocks too, by
the same rules (1372 Q6 B; see "A paste" below). Everything else keeps editing ghost blocks: the break (left) click's
shape erases ghost blocks, as with the setting off (1372 Q4 A), and Delete, the boxes and the material list mark and
count ghost blocks. A shape or a paste of real blocks leaves hidden ghost blocks hidden. A real block that fills a
ghost block takes it out of the layer, as any real block does (see "Ghost blocks the real world fills").

**How the server places each block.** The game sends a shape as one `place(dimension, face, hit, blocks)`: the first
click's face and hit, and each block's position and state (the layer's codec for a change), without the other half of
a two-part block, which Minecraft places itself. The server places the blocks from the lowest level up, and gives
each block that couldn't stay one more try after the rest, so a torch or a hanging lantern whose support the same
`place` puts in goes in too (`RealPlace.order`). It places each block as the player's own right click would place it
there, through Minecraft's `BlockItem.place` with a `BlockPlaceContext` at that position (read in the 26.3 jar):
Minecraft checks that the block is enabled and that the space takes a block (`BlockPlaceContext.canPlace`: air,
water, lava, grass, a single snow layer), asks the item for the state (`getPlacementState`, which hook S2 turns),
checks that the state can stay and that no entity is in its way (`canPlace`), sets it, fills its block entity from the
item, completes a door or a bed (`Block.setPlacedBy`), triggers the advancements for placing
(`ItemUsedOnLocationTrigger`), plays the place sound to the players nearby, and uses up one item of the stack
(`ItemStack.consume`; in creative the server places from a stack of the block's item that it makes, and uses none).
For a block made of several items, such as a double slab, `RealPlacer` uses up the rest too (see "The inventory"). An
operator's block, such as a command block, places only for an operator (`GameMasterBlockItem.getPlacementState`).
Each block's context has the shape's
face and the hit at the block's own height, as Building Planner's ghost blocks and Effortless Building's server take
it (`EffortlessShapes.states`, `PacketHandler.handlePlaceBuildMode`). During that call, hook S2 gives the state the
orientation of the shape's state (its values of the properties `BlockTurn` sets, through `PlaceTurn.accepted`) in
place of the player's turn for Disable, and the count of a block made of several items. Where Minecraft gives the
other form of the same item, such as a standing torch for a wall torch or a standing sign for a wall sign
(`StandingAndWallBlockItem`, whose form depends on where the player looks), S2 takes the shape's form, turned the same
way; it returns null when Minecraft's state is any other block. A block that doesn't go in uses no item.

Before it places, the server checks the shape as `ServerGamePacketListenerImpl.handleUseItemOn` checks a right click
(read in the 26.3 jar), and skips each block that fails: the dimension is the player's own; the player's game mode
lets them build (`Player.mayBuild`, false in adventure and spectator); the block's chunk is loaded
(`ServerChunkCache.getChunkNow`, so placing never loads a chunk); the block is within the build height, inside the
world border and outside spawn protection (`MinecraftServer.isUnderSpawnProtection`, `ServerLevel.mayInteract`);
it is within 64 blocks of the player's eyes, the player's and not Freecam's camera's (1372 Q2 A); and an item places
it (`Block.asItem` gives a `BlockItem`: water, lava, fire, a portal and a piston's head have none). Effortless
Building's own server checks no distance: its `handlePlaceBuildMode` places wherever the game's packet says. Its game
aims each click of a shape within its reach, 32 in survival, so a Line, Wall, Floor or Cube between such clicks stays
within 56 blocks of the eyes (32 times the square root of 3); a circle drawn from its centre, and the copies of the
mirror (up to 256 blocks across) and the array (up to 64 copies 64 apart), reach farther, and their blocks past 64
stay out. A protection mod that hooks the right click (Fabric API's `UseBlockCallback`) doesn't see these blocks; the
server runs none.

- **The inventory** (1372 Q1 C). In survival, each block uses an item of its block (`Block.asItem`: a torch for a
  wall torch) from the player's inventory, the main hand's stack first and then the inventory's slots in order, as
  Effortless Building's server takes them (`InventoryHelper.consumeItems`). A block made of several items uses that
  many, as by hand: a double slab two slabs, and three candles three candles, as for sea pickles, snow layers, turtle
  eggs and pink petals. Nothing goes in unless the inventory contains every item the blocks that pass the checks above
  need: the blocks those checks leave out need none, every block of the mirror's and the array's copies counts, and
  with the block mix each block counts as its own item (a Line of 16 cobblestone and 4 mossy cobblestone needs 16 and
  4). When the inventory lacks any of them, the server places nothing and its `notice` names each item short and how
  many, in the wording Geo approves in build step 5. A stack with components (a named block, a filled shulker box)
  places as by hand, its components going to the block entity.
- **Real blocks in the way** (1372 Q3 A). A block goes only into a space a right click could place it into
  (`BlockPlaceContext.canPlace`), so a shape or a paste never replaces a real block. Effortless Building's "Replace
  mode" button in
  the radial menu changes nothing, as with the setting off: its modes act only in the packet Building Planner never
  sends.
- **The cap.** The server refuses a `place` of more than 2,000 blocks (`RealPlace.MAX_BLOCKS`: Effortless Building's
  own cap on a survival player's shape, `ServerConfig.survivalMaxBlocksPlaced`, which such a shape never passes), and
  one that would take a player past that many real blocks in a second, with a `notice`, and places none of it. The cap
  is fixed rather than a server setting, so the player's game knows it too, and the copy shows red past it (see "A
  paste").
- **One sound.** Hook S3 lets only the first block placed play its place sound to the players nearby. The player hears
  a shape's from their own game, which plays it at the shape's first block as the click sends
  (`BuildPipelineClient.handleClick`), unless the inventory doesn't cover the shape (hook E41); a paste's first block
  plays its sound to the player too, since the game plays none for a paste.
- **The notice.** When the server leaves out blocks the player's preview showed going in (someone built there since,
  spawn protection, a chunk the server hasn't loaded), a `notice` says how many, in the wording Geo approves in build
  step 5. A shape whose blocks all go in sends nothing back; the player sees the blocks arrive as any block changes.

**The preview.** While the setting acts, Effortless Building's preview counts the inventory as it does on its own
servers, over the blocks that would go in: hook E8 lets `ItemUsageTracker.compute` run on the blocks `RealPlace`
lets in, in the form that counts each block's own item (`compute(Player, BlockSet, boolean)`), so the icons beside
the crosshair show how many of each block the click uses, with the missing count in red
(`RenderHandler.drawPlacingStacks`). Hook E40 then sets the blocks the preview draws red
(`ItemUsageTracker.missingPositions`, which `BlockPreviewRenderer.render` reads): the whole shape when the inventory
lacks any item (`ItemUsageTracker.hasMissing`), since then nothing goes in, and otherwise each block the server would
leave out by the rule the player's computer can check: a space that doesn't take a block, a block past the reach, and
a turned block that can't stay or has an entity in its way (`RealPlace`, the pure rule the server's placing uses too;
spawn protection and later changes are the server's alone). The controls line shows the state on the right click's
own control, by Geo's rule for the lines (1313 [c1]):
"[Right-click: place real blocks]" in Single and "[Right-click: start placing real blocks]" before a shape's first
click, in the wording Geo approves in build step 5.

**A paste** (1372 Q6 B). While the setting acts, the copy's right click (hook M2; see "Copy and paste") places the
copy's blocks as real blocks instead of writing ghost blocks, by the rules for a shape above: within 64 blocks of the
player's eyes, only into spaces a right click could place into, nothing unless the inventory contains every item the
paste needs, and at most 2,000 blocks. A paste sends one `place` of each block of the copy other than air and
structure void, at the position and in the state where Litematica draws it, and a Line, Wall or Floor of copies sends
one `place` for the whole set at its second click. A paste has no first click, so its `place` carries the top face
and a hit at the bottom middle of its first block, as if each block were placed onto the block under it; hook S2 then
gives each block the copy's orientation. The copy leaves out block entities' contents, so a pasted chest is empty and a
pasted sign blank. The copy stays at the aim for more pastes, and each paste's box joins the recent pastes the copy
snaps to (`PasteBoxes`), as a paste of ghost blocks does.

While the setting acts, the copy's colour (`CopyColour`) follows the rules for real blocks:

- blue where every block of the copy would go in, as far as the player's game can tell;
- yellow where some blocks wouldn't and the rest would, as Satisfactory's yellow still builds: a space that doesn't
  take a block (a real block there), a block past the reach, outside the build height or the border, or a block no
  item places, such as water. Ghost blocks under the copy don't make it yellow, since real blocks don't replace them.
  Whether each block can stay is the server's alone, since the copy's own blocks may hold it up;
- red where nothing would go in: more than 2,000 blocks in the paste or the set, checked first, as on the server, or,
  in survival, an inventory without every item the paste needs, counted as for a shape.

While the copy is red, the reason shows in red in place of the right click's control, "[Can't paste: too many
blocks]" or "[Can't paste: not enough blocks]", and a right click pastes nothing and sends nothing, as for a red copy
of ghost blocks. Otherwise the right click's control says the paste places real blocks, "[Right-click: paste real
blocks]" and "[Right-click: paste 4 copies as real blocks]", in the wording Geo approves in build step 5.

**Undo** (1372 Q5 B). Ctrl+Z and Ctrl+Y undo and redo only changes to ghost blocks: a shape or a paste of real blocks
makes no step on the player's undo list and leaves the redo list as it is (see "Undo"). So Ctrl+Z right after a shape
of real blocks undoes the player's newest change to ghost blocks, made before that shape, and the real blocks come out
only by hand.

**Compatibility.** 0.9.0 keeps protocol version 5: the game's check at joining asks for the same ten payloads (see
"When the server lacks the mod"), and the `place` payload decides only whether the setting acts, so a 0.8.0 game and
a 0.9.0 server work together, and so do a 0.9.0 game and a 0.8.0 server, with ghost blocks.

| # | File, method | Hook | Purpose |
|---|---|---|---|
| E1 | `buildpipeline/BuildPipelineClient.java`, `handleClick` | the same hook | While the setting acts, the shape goes to the server as `place` instead of into the layer (`EffortlessShapes.place`). |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | the same hook | While the setting acts, the place click's tracker computes as Effortless Building's own, over the blocks `RealPlace` lets in and counting each block's own item (`compute(Player, BlockSet, boolean)`), so the icons count what the click uses; the break click's list stays cleared. |
| E40 | `buildpipeline/BuildPipelineClient.java`, private static `updateDisplayTrackers(Player, BlockSet)` | `@Inject` at tail | While the setting acts, set `ITEM_USAGE.missingPositions` to every block of the shape when `ITEM_USAGE.hasMissing()`, and otherwise to the blocks `RealPlace` refuses, as hook E29 marks Disable's one block. |
| E41 | `buildpipeline/BuildPipelineClient.java`, private static `handleClick(Minecraft, BuildPipeline.BuildState)` | `@WrapWithCondition` on `ClientLevel.playLocalSound(BlockPos, SoundEvent, SoundSource, float, float, boolean)` | While the setting acts, play the place sound only when `RealPlace` finds that the inventory covers the click's blocks (the local `BlockSet blocks`), since otherwise nothing goes in. |
| M2 | `Minecraft.java`, `startUseItem()` | the same hook | While the setting acts, a paste goes to the server as `place` instead of as an edit, and a red copy's click sends nothing. |
| S2 | `BlockItem.place`, `getPlacementState` | the same hook | While the server places a shape or a paste, the shape's orientation and count for that block in place of the player's turn, the shape's form where Minecraft gives the other form of the same item (`StandingAndWallBlockItem`), and null when Minecraft's state is any other block. |
| S3 | `net/minecraft/world/item/BlockItem.java`, `place(BlockPlaceContext)` | `@WrapWithCondition` on `Level.playSound(Entity, BlockPos, SoundEvent, SoundSource, float, float)` | While the server places a shape or a paste, only its first block plays the place sound; a paste's first block plays it to the player too (`Level.playSound(null, …)`). |

`RealPlace` (common) is the rule for each block, the order, the inventory and the cap, which the game's preview, the
copy's colour and the server's placing share; `RealPlacer` (server) applies a `place`.

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
"Hide available", "Info HUD", "Clear ignored", "Clear cache" and "Multiplier". A "Close" button at the bottom right,
where "Litematica menu" was, closes it, as Escape does (Geo's comment c8: "The "materials lists" view should have a
clickable button to close the list"; c19).

The (i) icon at the top right, beside "Multiplier", shows Building Planner's text in place of Litematica's, which is
about Litematica's hotkey, its placements and its schematic browser (hook L12; Geo's comment c19: "only include info
relevant to this mod"). The text is one line a point, as Geo approved it (1313):

- "Counts the ghost blocks and real blocks in the box you drew."
- "Total: how many of each block are in the box."
- "Missing: how many are still to place, where there's air or a different block."
- "Available: how many you carry."
- "Info HUD: pins this list to the screen while you build."
- "Save list: keeps this list on the server."
- "Saved lists: opens the lists you kept."
- "The radial menu's "Material lists" button opens the current list again."

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
yet." The mouse wheel scrolls the rows. A "Close" button at the bottom right of "Saved lists" goes back to the screen
it opened from, as Escape does on either screen (Geo's comment c19: "a "close" button in the bottom right, like I want
for the saved lists screen"). The screen
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
buttons do nothing. A singleplayer world runs no server side, so it has no lists either. As proposed for 0.10.0, on a
server that takes none of Building Planner's payloads, the game starts a local session and saves the player's lists
on their computer (see "Servers without Building Planner"), and in the player's own world, the game saves them in the
world's folder, as this server does (see "The player's own worlds").

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
Building Planner's boxes, the copy at the aim, the build modes' first click and lock, and Delete's marking all aim
from the camera, and each block of a shape faces as it would for a player at the camera facing the same way (a stair
drawn while the camera faces north faces north). The nudge keys move the copy and the locked positions by the way the
camera faces, and the copy's middle click mirrors it by the way the camera faces. The
radial menu, the block mix, undo and redo, show or hide, and the material list's screens and panel don't aim, and work
as without Freecam. Real blocks stay as Freecam's own options leave them: with its "Allow interaction" off, as by
default, a click with an item that doesn't place a block, or in Disable, doesn't reach the world. That option guards
real blocks. Building Planner changes a real block only by turning the one a right click places in Disable, which
reaches the world only when the option lets the click through (see "Turning real blocks in Disable"). So ghost block
editing doesn't wait on the option, and players needn't find and change it.

Freecam 1.5.1 moves the camera as an entity of its own (`FreeCamera`, a client-side player entity), which it makes
the game's camera entity (`Minecraft.setCameraEntity`), and leaves the player standing where they were. Building
Planner's aim (`Aim`) is the eye position and look direction of the camera entity (`Minecraft.getCameraEntity()`),
which is the player whenever Freecam's camera is off, so nothing changes then. While Freecam's "Player control" is on,
which moves the player and keeps the camera still, the aim is the player's; `Aim` reads that option from Freecam's
`Freecam.isPlayerControlEnabled()` by reflection, since Building Planner never compiles against Freecam, and the rule
itself is `AimSource`. `GhostAim`, `BoxDraw`, the copy's aim and mirror, E1's block states (each placed with a
`BlockPlaceContext` for the camera entity), the lock's controls, and Delete's marking (`DeleteAim`)
read it. Reaches count from the camera: 256 for a box's corner and Delete's marks, 64 for the copy, and Effortless
Building's own for shapes and their first click. Freecam loads no
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

### Litematica's status panel

Geo saw two lines in a corner of the screen in the trial of 0.7.0, "Layer Mode: All" and "Renderers: A: ON S: ON B:
ON O: ON SEL: OFF" (1359, comment c1 and image 1). They are Litematica's status panel
(`render/infohud/StatusInfoRenderer.java`, its texts `litematica.hud.misc.render_layer_mode_all` and
`litematica.hud.misc.renderer_status`): the layer of the schematic Litematica draws (all of it), and whether each of
its drawing switches is on (A all rendering, S schematics, B schematic blocks, O the overlay's colours, SEL area
selections). Litematica's tool panel (`ToolHud`, at `toolHudAlignment`, the bottom left by default) draws it, with no
screen open.

Litematica shows the panel for 10 seconds (`startOverrideDelay`) whenever it adds a placement
(`SchematicPlacementManager.onPlacementAdded`) or a selection changes (`AreaSelection.markDirty`), while its option
`statusInfoHudAuto` (on by default) is on and any of those switches is off or the layer isn't All
(`shouldOverrideShowStatusHud`). "SEL: OFF" is Building Planner's own change (see "Litematica's area selection"), and
Building Planner adds a placement for each column of ghost blocks as its chunk loads, whenever an edit puts a column's
first ghost block in or reaches a 16-block section above or below its others (`Tiles.columnChanged`), for every column each time
ghost blocks show again, and for the copy, so the panel shows often. Players can't open Litematica's settings to
turn it off, since M opens Freecam's (see "Keys").

Building Planner turns off `statusInfoHudAuto` once on each player's computer, under its own record (see "Settings
Building Planner changes once"), so the panel shows only for a player who turns on Litematica's `statusInfoHud`
(off by default). Area selections stay undrawn.

### When the server lacks the mod

On joining, the client checks with `ClientPlayNetworking.canSend` that the server takes all ten of its payloads. When
it doesn't, Building Planner stays off for that server, silently: the player sees no message, and its keys, its radial
menu buttons and its sliders do nothing, and neither the mix panel nor the material list panel shows. While it's off, Effortless Building's shapes
place nothing: its place and break packets (hooks E1 and E2) and its undo and redo packets (E4) aren't sent, so a
server that runs Effortless Building without Building Planner never changes real blocks for them. In Disable, Left
Ctrl, the wheel and the middle click work as in Minecraft, and every real block places as Minecraft places it (see
"Turning real blocks in Disable"). The same holds from
joining until the server's `hello` arrives. A `hello` with another protocol version leaves it off the same way, so a
player whose game still has Building Planner 0.5.0 (protocol version 4) sees no ghost blocks and no lists on a server
with 0.6.0 until their next launch updates it. A
singleplayer world, or a LAN host's own world, runs no server side, and Effortless Building works there as it does
without Building Planner. The server sends nothing to a player who can't receive it (`ServerPlayNetworking.canSend`),
so players without the add-on, or without any mods, join and play as before. It sends `hello` only to a game that also
takes `columns`, since Building Planner 0.2 takes a `hello` of another layout under the same name.

As proposed for 0.10.0, on a server that takes none of the ten payloads, the game starts a local session instead of
leaving Building Planner off (see "Servers without Building Planner"), and in the player's own world, Building
Planner's server part runs inside the game and sends the `hello` (see "The player's own worlds"). Building Planner then
stays off only on a server that takes some or all of them and sends no `hello` of the game's protocol version.

### Servers without Building Planner

Proposed for 0.10.0, not built. Geo wants Building Planner to work as a client-side mod on other servers, where other
people don't need to see the player's ghost blocks (comment c4 on 1359), and picked "Yes, in a later version: there,
your ghost blocks are saved on your computer and only you see them, and this server still shares them." (1368 Q1 A).
Shapes place real blocks only on servers that run Building Planner (1368 Q2 A). For the player's own worlds, Geo picked
"Yes, with ghost blocks shared with the world's LAN guests" (1374 Q4 C). Geo approved the designs that follow in 1381.

**Which servers.** At joining, the game checks the ten payloads as "When the server lacks the mod" describes. If the
server takes none of them, the game starts a local session at once: Building Planner plays the server's part itself,
on the player's computer, for that player alone. If the server takes some or all of them, the game waits for the
server's `hello`, so a server with another version of Building Planner leaves it off, silently
(483). If a `hello` arrives during a local session, the game saves and ends the local session and starts the server's.
On a server that runs Effortless Building's own server part but not Building Planner, the game starts one too: the
player's shapes draw ghost blocks there, and hooks E1, E2 and E4 keep Effortless Building's packets from going out, as
on this server. The player's own world, singleplayer or opened to LAN, runs Building Planner's server part inside the
game, so the game takes the payloads and sends the `hello` there as this server does (see "The player's own worlds").
A LAN guest's game shares the ghost blocks of a world whose host's game runs Building Planner, and starts a local
session in a world whose host's game doesn't.

**The local server.** `LocalServer` (client) runs the classes the dedicated server runs from the common source set:
`LayerSync` for the layer, its checks and undo, `LayerStore` for the layer's files, and `MaterialLists` with
`ListFiles` for the material lists. Its `LayerSync.World` answers from the game itself: the player's dimension and
chunk column, the radius of the chunks the game keeps loaded as the view distance, the level's build height and world
border, and a real block only in a chunk the game has loaded (`ClientLevel`). Its `Outbox` queues each payload for the
player, and the game handles the queue in order at the start of its next tick, as it handles a server's packets.
`ClientSession` and `ListSync` send through a link, the network on a server that runs Building Planner and
`LocalServer` elsewhere, so the session, its replica (`LayerReplica`) and the tiles work as they do on this server. The
local `hello` has protocol version 5 and the block cap of 200,000 blocks an edit (`SyncLimits.DEFAULT`).

**The files.** The game saves the player's ghost blocks for each server in
`<game folder>/building-planner/servers/<server>/<player UUID>/layer/<dimension namespace>/<dimension path>/<x>.<z>.dat`,
in the format of "The layer's files on the server", and their material lists in
`<game folder>/building-planner/servers/<server>/<player UUID>/material-lists/<player UUID>.dat`, in the format of "The
lists on the server". `<server>` is the name Litematica gives its own folders for that server (MaLiLib's
`StringUtils.getWorldOrServerName`): the address the player joined, with each `:` turned into `_` (`play.example.org`,
`localhost_25565`), and `realms` for a Realm. The game reads every file in the server's folder as the session starts,
writes a changed area at most every 30 seconds while the layer has changed, and writes every change as the player
leaves the server and as the game closes (`ClientLifecycleEvents.CLIENT_STOPPING`), each through a temporary file and a
rename, by `LayerStore`'s rules. A file it can't read is logged, skipped and never written over, as on the server. If
it can't list the server's folder, Building Planner stays off on that server and logs an error, rather than run
without the player's ghost blocks. A server joined by two addresses (a name and its IP address) has two folders, as
Litematica's own do. A LAN world has a new port each time it's opened to LAN, so in a world whose host's game doesn't
run Building Planner, a guest's ghost blocks come back only while the host keeps the world open.

**What works there.** Every feature in "What players get" works there as on this server, from the player's computer
alone, except:

- Only the player sees their ghost blocks, and no one else's edits reach them, so none glow.
- Undo and redo take back the player's own changes, with 50 steps and 2,000,000 blocks as on this server. The steps
  survive leaving and rejoining the server and changing dimension, and last until the game closes.
- A real block fills a ghost block by the same rule, but only where the game sees it. Hook C1 notes each change to a
  block in a chunk the game has loaded, and at the end of the client tick `LayerSync.removeFilled` removes the ghost
  blocks still filled, as one edit with no undo step. A position where the game placed a block that the server hasn't
  confirmed yet (`BlockStatePredictionHandler`) waits for the server's answer, so a placement the server refuses leaves
  its ghost block in place. The check at chunk load (`ClientChunkEvents.CHUNK_LOAD`, `LayerSync.chunkLoaded`) finds blocks
  placed while the chunk wasn't loaded. A block placed and broken again while the chunk wasn't loaded leaves its ghost
  block, where this server's hook S1 would have removed it.
- "Place real blocks" doesn't act: with it on, shapes draw ghost blocks, as with it off (1368 Q2 A).
- In Disable, Left Ctrl, the wheel and the middle click work as in Minecraft, and "Match ghost blocks" doesn't act,
  since both turn a real block through the server's hook S2: real blocks place as Minecraft places them (1374 Q3).
  `DisableTurn`'s rule and hook M6's `place_turn` act only during a session with a server that runs Building Planner.
- The material lists are kept on the computer for that server: "Last counted", the saved lists, the current list and
  the panel come back when the player returns (1374 Q2 A).
- While the radial menu is open, a line above "Building Planner" in its corner says that only the player sees their
  ghost blocks there (1374 Q1 C), reading "(client only)" (1381 c1), in the wording Geo approves in
  build step 8. Hook E42 draws it as Effortless Building draws the name below it: right-aligned 4 from the screen's
  right edge, with a shadow, one row (10) above the name, in the grey of the menu's mode descriptions
  (`descriptionTextColor`, #888888 at 87% opacity) (1381 Q1 A). The menu draws a hovered mode's description 100 below its middle, so on a screen up to 256 high in the
  GUI's units (854 by 480 at GUI scale 2, 1280 by 720 or 1366 by 768 at GUI scale 3), where that description would
  cross the line's row, the line goes a row above the description's instead. The line doesn't show in the player's
  own world or on a server that runs Building Planner.
- `OldPlans` removes the old plans' placements only during a session with a server that runs Building Planner, other
  than the player's own world, so a player's Litematica and Syncmatica placements on other servers stay.

**Between servers.** The copy stays while the game runs: leaving a server, joining another and changing dimension don't
drop it, on this server too, so a player can copy ghost blocks and real blocks on one server and paste them as
ghost blocks on another (1375 Q1 A). The ghost blocks saved for a server stay in its
folder, unused, while that server runs Building Planner, and show again if it stops running it (1375 Q2
A).

**This server.** On a server that runs Building Planner, 0.10.0 works as 0.9.0 does: the layer is shared, and the
server keeps the lists. 0.10.0 changes nothing the server does and keeps protocol version 5, so a 0.9.0 game and a
0.10.0 server work together, and so do a 0.10.0 game and a 0.9.0 server. If this server ran without Building Planner,
as after a rollback, the games of players with 0.10.0 would start local sessions there, drawing ghost blocks only they see.

**The player's own worlds.** In a world of the player's own, singleplayer or opened to LAN, Building Planner works as
on this server, its server part running inside the game (1374 Q4 C, 1381 Q2 A):

- `BuildingPlanner.onInitialize` registers `ServerSync` in the game too, so the game's own server runs the layer, undo,
  the lists and `PlaceTurns` from the world's start to its stop, through the same Fabric API events and payloads as a
  dedicated server. The player's game receives the `hello` at joining and starts a session, and so does each LAN guest's
  game with Building Planner: they share the world's ghost blocks, each with their own undo, and see each other's
  edits glow. Guests without the add-on join and play as on this server.
- The layer and the lists are files in the world's folder, `<game folder>/saves/<world>/building-planner/layer/` and
  `material-lists/`, in the formats of this server's and written as often, so a copy of the world contains its ghost
  blocks, and deleting the world deletes them. The world's stop, as the player leaves it or the game closes, writes
  every change.
- In the game, the cap is 200,000 blocks an edit (`SyncLimits.DEFAULT`), and the server part reads and writes no
  `config/buildingplanner.properties`.
- Hook S1 is in the mixin config's common section, beside S2 and S3, so real blocks fill ghost blocks there as on this
  server; it acts only for a `ServerLevel`, so the game's own levels pass through it.
- The import of the plans Syncmatica shared runs only on a dedicated server, and `OldPlans` doesn't run in the player's
  own world (`Minecraft.isLocalServer`), so the player's Litematica placements there stay, old plans' files included.
  A LAN guest's game runs it as on a server.
- If the server part can't list a world's layer files, it logs an error and stays off in that world, sending no
  `hello`, so the world opens with Building Planner off, as on a server with another version. A dedicated server
  still refuses to start.
- The server part doesn't check any permission, so a world with cheats off works the same.
- `BuildingPlannerClient`'s `off`, `ownsShapes` and `undo` make no exception for the game's own world: they ask only
  whether a session runs, so Building Planner is off there from joining until the `hello` arrives, as on this server.
- Effortless Building's shapes draw ghost blocks there: hooks E1, E2 and E4 keep its packets from going out, so its own
  server part, which the game also runs, places nothing for them. With "Place real blocks" on, shapes and pastes place
  real blocks through Building Planner's server part by the rules of "Place real blocks": at most 2,000 blocks a shape
  and a second, within 64 blocks of the player's eyes, from the inventory in survival, and Ctrl+Z undoes only ghost
  blocks (1372 Q5 B) (1381 Q3 A). Disable's turn and
  "Match ghost blocks" work there as on this server.

**Litematica's extra blocks.** Hook L7 shows Litematica's colour for an extra block inside the box of a placement of
the player's own that holds air there, on any server, so the player's own placements show extra blocks as Litematica
does without Building Planner. The layer's tiles mark none.

| # | File, method | Hook | Purpose |
|---|---|---|---|
| C1 | `LevelChunk.setBlockState(BlockPos, BlockState, int)` | `@Inject` at return, in the mixin config's client section | During a local session, for the game's own level: note a position whose new state fills a ghost block of the local layer, for the check at the end of the client tick. |
| L7 | `render/schematic/ChunkRendererSchematicVbo.java`, `getOverlayType(BlockState, BlockState)` | the same hook, or one on its caller where the block's position is known | Keep `EXTRA` inside the box of a placement that isn't a tile or the copy and holds air there. |
| E42 | `screen/RadialMenu.java`, `drawTexts` | `@Inject` after the call that draws the corner's name (E18's string) | During a local session, draw the line above "Building Planner" (`buildingplanner.ui.radial.local`) at the row `RadialLine` gives for the screen's height, and record its text and row for the driver's checks. |

`LocalServer` (client) wires the common classes to the game; `SessionLink` is what `ClientSession` and `ListSync` send
through; `RadialLine` (client) is the pure rule for the radial menu line's row: 20 above the screen's bottom, or 90
below the menu's middle on a screen up to 256 high.

**Real blocks from the computer alone.** Shapes don't place real blocks on other servers (1368 Q2 A). What the sources
show:

- From the player's computer alone, a real block goes in only as a player places it: one right click sent for each
  block (`ServerboundUseItemOnPacket`), which Minecraft 26.3's server takes only within the player's block reach plus
  1 of their eyes (`ServerGamePacketListenerImpl.handleUseItemOn`, `Player.isWithinBlockInteractionRange(pos, 1.0)`:
  5.5 blocks in survival), and only with the block in the player's hand, from their inventory in survival.
  Litematica's Easy Place places a plan's blocks this way (`util/WorldUtils.doEasyPlaceAction`: one `useItemOn` a
  block, the block picked into the hand first, with a limit on how fast). On a server without Servux or Carpet, Easy
  Place sends only a slab's top or bottom half with the click (`PlacementHandler.getEffectiveProtocolVersion`), so
  other blocks face the way the player looks. Many servers' rules forbid mods that place blocks for the player
  ("printers").
- A whole shape's real blocks at once, at a distance, need a server part. Effortless Building's own server places
  them (`network/PacketHandler.handlePlaceBuildMode`: within its reach of 32 in survival, taking the blocks from the
  player's inventory, with undo of 50 steps for each player in `utilities/UndoManager`), and its game alone places
  nothing (issue #16, in [plan.md](plan.md) "Source evidence"). Building Planner's server part could place them the
  same way.

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
  Ctrl+Y and the break click. As proposed for 0.10.0, both clients start a local session there instead, the block
  mix key opens the mix screen, and the shape adds ghost blocks on Planner1's client only (see `local.sh`).
- A `hello` with protocol version 4, as Building Planner 0.5.0 sends, which the driver hands to the client, starts no
  session and shows no message. Then Left Ctrl doesn't turn the block in hand (step 18): in Disable, with oak stairs in hand, Left
  Ctrl with the wheel changes the hotbar slot, and a right click with Left Ctrl held places the stair as Minecraft
  places it.
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
  the screen's edges (the driver's `radialclear`), in their slots (the driver's `slots`: "Show or hide ghost blocks"
  under the wrench, Copy, Paste and Delete in the row above it, "Block mix", "Material list" and "Material lists" in
  the row above that), each with its hover text; each is shot, with the buttons' boxes in the log, and so is Delete's
  hover text. With the pointer on "Show or hide ghost blocks", its hover text is the frame's only tooltip, with no
  "Functional Settings" (hook E34), for Planner1 an operator and, once Planner1 makes Planner2 one and itself none,
  not; Planner2 then makes Planner1 an operator again (the driver's `tooltips` and `operator`). Shots taken 1 and 2
  frames after the menu opens show the buttons nearer the middle and smaller than when it's open, as the menu's own
  are. With
  Effortless Building's mode Line, no mode segment shows selected while the copy is at the aim or a box is drawn, and
  Paste, Copy, "Material list" or Delete does; with the box ended, Line's segment does, and no button.
- The menu's corner text reads "Building Planner", and Controls shows Building Planner's keys, the copy's eight among
  them, and Effortless Building's under the "Building Planner" heading, sorted by name as Controls sorts them, with no
  raw key name;
  each is shot, Controls at 854 by 480 and 1920 by 1080, GUI scale 2, and at 1920 by 1080, GUI scale 4. The driver's
  own Page Up key, under a heading of its own, is unbound first, so Controls shows no clash.
- The Copy button starts a box: the controls line shows "[Left-click: set the first corner][Esc/Right-click:
  cancel]" and "Copy" shows left of the hotbar in Single's place, a right click and a use of the place click places no block, and Effortless Building's preview and mode are
  as they were; Esc ends a box as the right click does, opening no screen. The box draws white (the driver's
  `drawbox`). Aiming at a block's top face, the box's first corner is
  the space above it, and that face shows shaded (the driver's `boxshade`); aiming at its side, the block itself, with
  no face shaded. The first click, with the aim held, keeps the same box and drops the shade: the shot before it has
  more than 500 pixels darker than the shot after it, which has fewer than 200 darker than the one before (the
  driver's `shotdarker`); the shaded face is shot at 854 by 480 and 1920 by 1080, GUI scale 2. The box follows the aim
  between clicks and the list under the crosshair counts its blocks, its row on a dark background 2 wider than its
  cells at each side (the driver's `boxlistbackgrounds`); the box is shot at both sizes after its first click. The third
  click makes the copy, with its message, places no real block, doesn't reach the other player, and leaves
  Effortless Building's mode as it was. The copy rests on the aimed top face, the middle of its bottom on the block
  above, and follows the aim, and Litematica neither selects nor lists it. Against a real wall's side face, it stands
  beside the wall, the middle of the side that touches it on the block beside the face; under a floating block's bottom
  face, it hangs below it. On the floor and against the wall it's shot at 854 by 480 and 1920 by 1080, GUI scale 2.
- At 854 by 480, GUI scale 2, the list counts a small box's blocks, most numerous first, and the material list box's
  controls line wraps onto two rows of whole rectangles, the box's size last. A box of 144 by 3 by 144 (as wide as the chunks the dev
  server's view distance of 6 loads around the player allows) over a floor of 40 kinds of block shows the list in rows
  of 10 that end inside the screen and above the controls line, with a "+11" cell, each row on its own dark
  background, the rows' backgrounds meeting; it's shot at 854 by 480 and 1920 by 1080. A break shape drawn in Line mode
  over real grass has an empty break list (E8). A box past the side limit, or over a chunk not loaded, draws in the
  limit's red, says which limit, and its click sets no corner.
- With the copy at the aim, the controls line shows its controls. A notch up turns it a quarter turn clockwise in place,
  the middle of its bottom staying on the same block, a notch down back, and the hotbar slot stays; a middle click,
  facing south with the crosshair on the copy's blocks, flips it east to west within the same box, with its stairs
  turning with it, and picks no block, neither the game's nor Litematica's.
- Over other ghost blocks, the copy's blocks show where they overlap, and the ghost blocks show in its empty space.
- A right click pastes it: the other player sees its blocks at once, turned and mirrored as the copy showed them,
  glowing with the paster's name; the copy's empty space leaves the ghost blocks under it, and the copy stays. One
  Ctrl+Z undoes the whole paste, and Ctrl+Y redoes it.
- Aimed at a side face inside that paste's box, the copy sits flush against the box's face, its edges in line with the
  box's at the corner nearest the aim, not where the aim alone would put it; it's shot at 854 by 480 and 1920 by 1080.
- With Left Ctrl held, the copy stays on the paste's level and its row, moving along the row as the aim moves, and on
  its column when the aim is nearer that; it's shot on the row at both sizes. Let go, the copy rests on the aimed face
  again.
- H locks the copy, and with Planner1 moved away and aiming elsewhere it stays where it was; it's shot at 854 by 480 and
  1920 by 1080. Facing north, each arrow key, Page Up and Page Down moves it one block: Up north, Down south, Left west,
  Right east, Page Up up and Page Down down. Up held down moves it 1 + (ticks down − 1) / 4 blocks, by the driver's
  count of the ticks the key was down, about 5 a second. H unlocks it, and it rests on the aimed face again. Locked
  again, a right click pastes it, for the other player too, and the copy unlocks, its line naming Left Control's line
  up again, and follows the aim; it's shot following the aim at 854 by 480 and 1920 by 1080. With the paste erased and
  the copy locked again where it was, with Freecam's camera on and facing east while Planner1 faces north, Up moves it
  east, away from the camera.
- The copy's mode starts as Off, and R switches it to Line, Wall, Floor and Off, H locking and unlocking the copy in
  each, and to Line again. In Line the first right click fixes the copy as the set's first copy and sends no edit.
  Looking up past the copy, the set has 4 copies, each the copy's height above the one before, each one of
  Litematica's placements, which Litematica draws; the first stays where the click fixed it. It's shot at 854 by 480
  and 1920 by 1080. Looking farther up, the set stops at 10 copies; looking back, it shrinks to 4. A notch of the
  wheel up turns every copy a quarter turn, the set laid again at the turned size, and a notch down turns them back.
  H locks the first copy, and a nudge moves it and the set with it, which the nudge back returns. A left click drops
  the set, sending no edit, and leaves the copy following the aim, still in Line. The Line of 4 up pastes as one edit,
  which the other player sees, every copy's blocks, and one Ctrl+Z takes it back; so does a Line of 4 east, shot at
  both sizes. R during that Line switches to Wall with the same first copy and, from the same aim, the same 4 copies;
  aimed up and east, the Wall is 3 copies by 2, shot at both sizes, and pastes as one edit, which one Ctrl+Z takes
  back. R during a Wall switches to Floor with the same first copy; aimed north east on the grass, the Floor is 4
  copies by 4, shot at both sizes, and pastes as one edit; aimed at the east face of one of its copies, the copy sits
  flush beside that copy, not beside the Floor's edge; and one Ctrl+Z takes back the whole Floor. R during a Floor
  switches to Off, which drops the set and leaves the copy following the aim.
- Esc during a copy's Line of 4, with Single chosen: with the inventory open, Esc closes it and the set stays; then
  Esc drops the set, sending no edit, and the copy follows the aim, still in Line, with Line's line before the first
  click; Esc again puts the copy away, and Single's line shows; Esc again returns the build mode to Disable; and Esc
  again, with no step left, pauses the game. No step opens a screen.
- With Line set, a new copy made with the Copy button keeps the mode, and its first click fixes a set's first copy.
  After Planner1's game restarts, the mode is Off again.
- The copy's colours (the driver's `copycolour`): on the grass it's blue, and Litematica draws no box for it or for a
  set's copies (hook L6, the driver's `boxes`), which are blue too; over a ghost gold block its stone bricks would
  replace, it's yellow, and its click still pastes. Locked and nudged down to the world's bottom it's blue, and one
  block lower, partly below the build height, it's red; then, back on the grass, with a world border whose edge
  crosses it, it's red again. Each red click pastes nothing, sends no edit and adds no chat line, and the copy stays
  locked where it was; with the border back, it's blue. A copy of 102,400 ghost stone blocks is blue, and a Line of two
  of them going down, 204,800 blocks, is red; its click sends no edit and leaves the set, and aimed back to one copy
  the set is blue. The copy is shot blue on the grass, yellow over the ghost blocks, red at the border, and the red set
  over the cap, each at 854 by 480 and 1920 by 1080, GUI scale 2.
- The controls lines, each checked word for word (the driver's `modehud`): Off before the first paste, and with
  "[Hold Left Control: line up]" after it; locked after a paste, with the nudges, "[H: Lock (On)]" and no "line up";
  locked and red below the build height and at the border, with the reason in place of the paste, and the paste back
  once it's blue; R on that locked red copy, which makes it a red Line, Wall and Floor in turn before its first
  click, each with the reason before "[Right-click: set the start]", and R once more back to Off; Line, Wall and Floor
  before the first click, each locked too; a set of 1, 4 and 10 copies in Line, and of 4 locked, with the nudges; a
  Wall of 4 and of 6 copies and a Floor of 16; the red set over the cap, with "[Can't paste: too many blocks]"; and
  each box's line, Delete's among them. With the lock key bound to K, the mode key to G, the nudge forward to I and the
  sprint key to Right Control (the driver's `bindkey`), the line names K, G and Right Control, K locks the copy, and
  the locked line names the four nudges one by one beside "Page Up/Down", K and G; with the keys back, it names H and R
  again. Each line is shot at noon at 1920 by 1080, GUI scale 4, the longest (the locked copy, and locked and red) at
  854 by 480, GUI scale 2, locked and red at 1920 by 1080, GUI scale 2, too, and Off's over a floor of ghost white
  wool, birch planks and glowstone at GUI scales 2 and 4, with the lines as they wrap, every rectangle's window box,
  and the union of their backgrounds in the log (the driver's `modewrap`, `moderects` and
  `modebox`). In each shot every rectangle is one line of text high with 2 GUI pixels of background at each side, the
  rectangles in a row are 4 GUI pixels apart, the rows 11 apart, and each row is centred and inside the screen less 4
  GUI pixels at each side; at 854 by 480, GUI scale 2, the locked line wraps onto a second row. The reason's
  rectangle on the locked red copy and on Disable's red line, and the warning's on a box too large, are red, and the
  others white (the driver's `modecolours`).
- A left click puts the copy away, and the radial menu's Paste button brings it back to the aim, turned and mirrored
  as it was. Choosing a mode in the radial menu, or the Block mix button, puts it away too.
- A box around a real house with no ghost blocks copies its real blocks. With a ghost gold block added inside the house
  and ghost stone over one of its real oak planks, the same box copies the ghost blocks and the real blocks together,
  taking the ghost stone over the oak plank; with ghost blocks hidden, it copies the house's real blocks only, and
  ghost blocks stay hidden.
- A box with no blocks in it (its first corner, a real block, taken away before the third click) gives its message; a
  copy over the block cap gives the message with the server's limit and keeps the earlier copy; Paste with no copy
  gives its message; a box of one block has its own message.
- The Copy button during its own box ends it; the "Material list" button during a Copy box switches it, keeping its
  corners, with its line.
- The Delete button during a box ends it and starts Delete (the driver's `mousemode`), and a right click with no
  marks ends Delete, "Single" showing left of the hotbar again.
- A box for the material list draws blue, and is shot after its first click at 854 by 480 and 1920 by 1080. A box for
  the material list around 10 ghost oak planks, 6 of them built as real oak planks, and 20 real stone
  blocks, with ghost blocks shown and a copy at the aim, opens Litematica's material list as "The material list"
  describes, places and erases nothing, and puts the copy away. With ghost blocks hidden, the same box lists only the
  planks and stone that are built, and ghost blocks stay hidden. Litematica's list and the button's hover text are shot
  at 1920 by 1080, GUI scales 2 and 4. A box with no blocks opens an empty list.
- The material list's screen shows "Refresh", "Hide available", "Info HUD", "Clear ignored", "Clear cache", "Save
  list", "Saved lists", "Close" and "Multiplier", and no "Export", "Write to file" or "Litematica menu" (the driver's
  `ListScreenProbe` reads the buttons' labels), with "Close" ending 10 from the screen's right edge and near its
  bottom, at 854 by 480 and 1920 by 1080, GUI scale 2, each shot. Pointing at the (i) icon (the driver's `infoicon`)
  draws each line of Building Planner's text and none of Litematica's, shot at both sizes. "Close" closes the screen,
  back to the game, as Escape does.
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
  their sizes, and "Close" at the bottom right, below the rows, which goes back to the list's screen, as Escape does.
  A new box replaces "Last counted" and leaves "Planks"; opening "Planks" doesn't change "Last counted". "Delete" asks
  again, then removes it. A name of 65 characters can't be typed, and "Save" is greyed out with an empty name.
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
- Delete, marking by sweeping ("Delete"): the Delete button starts Delete (the driver's `mousemode`), R does nothing
  in it, and after Planner1's game restarts the Delete button starts Delete again. Facing a ghost wall of 10 by 5 stone
  bricks 8 blocks away, with a real stone block in front of the middle row's fourth, holding Left Ctrl while the driver
  turns the view across the middle row from end to end in 3 frames (`turn`) marks the row's other 9 blocks and no real
  block (`marks`), with no gap; the list under the crosshair shows stone bricks and 9. A second hold that starts on the
  third mark and sweeps west unmarks the first three. A ghost door marked by its lower half has one mark and two
  positions to erase. Planner2 erasing a marked block drops its mark. A right click clears the marks, and a second ends
  Delete; Esc does the same, clearing the marks, and again ending Delete, opening no screen. One hold over each row marks the wall's 49 blocks the aim meets, and the marked wall is shot at 854 by 480 and
  1920 by 1080, GUI scale 2, and with GUI scale 4, beside the same view unmarked. The Delete button with ghost blocks
  hidden shows them and starts Delete, and the Copy button in Delete starts a Copy box.
- Delete, marking with a box: over ghost stone bricks of 10 by 4 by 10, Left Ctrl held and the left button pressed on
  the front face of the bottom corner block start the box at that block (the driver's `markbox`); aimed at the top of
  the far top corner block, the box is 10 by 4 by 10, its line names the release and its size, and the list under the
  crosshair counts 400 stone bricks. It's shot at 854 by 480 and 1920 by 1080. While it's drawn, the crosshair passing
  over the ghost wall outside the box marks none of it, and letting go marks the box's 400 ghost blocks and nothing
  else, shot at both sizes. A box started on a marked block unmarks: a box of 5 by 4 by 5 from it leaves 300 marked. A
  box may start on a real block, and a right click during the box ends it, marking nothing; so does Esc, opening no
  screen, and letting go of the button after it marks nothing. The 300 marks erase as one
  edit, leaving the 100 unmarked, and one Ctrl+Z brings them back.
- Delete, erasing and its lines: with 9 marks and the ghost door marked, a left click held 10 ticks shows the progress
  bar (the driver's `erasebar`) a quarter full at the 5th tick and half full at the 10th, 182 by 5 in the screen's
  units, centred, its bottom 4 above the controls line's top (`modebox`), shot half full at 1920 by 1080, GUI scale
  4; let go then, it erases nothing and the bar goes. Held 20 ticks, the left click sends one edit, which takes the
  door's upper half too and which Planner2 sees glow with Planner1's name, and says "Erased 11 ghost blocks."; holding
  on erases nothing more. Delete stays on with no marks, and one Ctrl+Z brings the blocks back unmarked. The lines,
  each checked word for word (the driver's `modehud`): with no marks, with marks, during a hold that unmarks, while the
  box is drawn marking and unmarking, with the cap's "Too many blocks" at 200,000 marks, and with the box over two
  more ghost blocks at the cap, which draws red with "Too many blocks" and whose release marks nothing; with the sprint
  key bound to Right Control, the line names Right Control. Each line is shot at 1920 by 1080, GUI scale 4, the line
  with marks also at 854 by 480, GUI scale 2, and the erase's message, each with its rectangles checked as the copy's
  are. What marks cost goes to the log: frames timed (`frametimes`) over 200,000 ghost bricks with none marked and
  with all marked (`markall`), building the shape of those marks and of 200,000 that touch no other, whole and after
  one mark changes (`timeoutline`), and a frame's sweep of 65 aims (`timeaims`).
- Locking the first click (step 14): in Single mode with stone bricks, H locks the position beside the aimed face
  (the driver's `lock`), and with Planner1 flown 5 blocks away and aiming elsewhere, Effortless Building's preview
  draws it there. With Effortless Building's array of 2 copies one block apart upward, the preview draws all three.
  Facing north, Up moves it one block north and Page Up one up. A right click places one ghost block there, on both
  clients, and the lock ends. Locked and nudged, the right button held for a second while Planner1 looks at the sky
  sends one edit, at the locked position, and the lock ends. Locked on a ghost block, with Planner1 aiming at another,
  a left click erases the ghost block at the locked position, leaves the aimed one, and ends the lock. With the array
  of 2 copies upward, a locked right click places three ghost blocks and a left click locked at the first erases all
  three. Locked on a ghost block, the left button held for a second while Planner1 looks at the sky sends one edit. A
  locked oak stair, nudged up into open air with Planner1 facing east, places there facing east, and the lock ends.
  With a stick in hand, H and the nudge keys do nothing and Single's clicks act as without the add-on; with stone
  bricks back in hand, the position is still locked; choosing Line in the radial menu ends the lock; with the copy at
  the aim, H locks the copy and not the first click. With Freecam's camera on and facing east while Planner1 faces
  north, Up moves the locked position east. The locked position is shot in open air at 854 by 480 and 1920 by 1080, GUI
  scale 2, and Controls' "Building Planner" heading with the keys' names at GUI scale 4. In Wall, H before the
  first click locks the first corner and Up nudges it; aimed elsewhere, the preview draws the corner alone there; the
  first click starts the wall there, the far corner follows the aim (a wall of 6, then of 15, by the preview), and the
  second click places the wall from the locked corner, on both clients, and ends the lock. In Cube, H after the second
  click locks both positions the clicks set, Up moves both north with the preview, and the third click places the cube
  from the moved positions and ends the lock. The locked Wall corner and the nudged Cube are shot at 854 by 480 and
  1920 by 1080, GUI scale 2.
- Single without row modes, and a Line from a locked position: in Single mode with stone bricks, R leaves Single's
  line, the lock and the copy's mode as they are (the driver's `modehud`, `lock` and `copymode`), and a right click
  places one ghost block as one edit. In Line mode with oak stairs, H locks the first click's position; the first
  click, aimed elsewhere, starts the Line there and sends no edit; looking up into open air on the line above the
  start, the preview draws a Line of 12 (the driver's `preview`), and the second click places the 12 stairs as one
  edit, on both clients, and ends the lock. The Line is shot before the second click at 854 by 480 and 1920 by 1080,
  GUI scale 2.
- One ghost block, turning and flipping (step 16): with oak stairs in Single mode, facing north, a notch up with Left
  Ctrl held turns the stair a quarter turn clockwise, to face east (the driver's `heldturn`), and the hotbar slot
  stays; locked, the preview draws the stair facing east (the driver's `previewstate`); the stair placed then faces
  east, and so does one placed after Planner1 turns to face south. The middle click with Left Ctrl held, aimed at a
  ghost block, makes the stair's top half, with no block picked, from the game or Litematica; locked, the preview
  draws it so, and placed, it is so on both clients; unlocked, no preview draws it. The same stairs in a second slot keep the turn; with stone in hand, Left Ctrl and the wheel change the hotbar
  slot, and back on the stairs, the turn is gone. An oak log aimed at a side flips to standing and back. Without Left
  Ctrl, the wheel changes the hotbar slot and the middle click picks the aimed block. The checks log what an unlocked
  left click erases with Effortless Building's array and mirror on. Turned and flipped stairs are shot before placing,
  locked, and after, at 854 by 480 and 1920 by 1080, GUI scale 2.
- Turning in every mode (step 17): with oak stairs turned a quarter turn in Single, to face east, the turn stays after
  choosing Line in the radial menu, and a Line of 5 places 5 stairs facing east, on both clients, as its preview drew
  each of them (the driver's `previewstate`). In Line, a notch up with Left Ctrl held before the first click turns the
  stairs to face south. Before the first click, in Wall, Cube, Sphere and Line, the preview draws one stair at the aim,
  beside the aimed face, facing south, and nothing else (the driver's `preview`); aiming at the sky, it draws nothing;
  with Effortless Building's array of 2 copies one block apart upward, it draws the stair and its 2 copies once each.
  The first click's stair is shot in Wall and in Cube at 854 by 480 and 1920 by 1080, GUI scale 2, with its box in the
  window logged (the driver's `screenbox`). Another notch between the two clicks turns the preview at once, to face
  west, and the Line places facing west. One shape of the turned stairs in each of Wall, Floor, Cube, Diagonal Line, Diagonal Wall, Slope Floor,
  Circle, Cylinder and Sphere has every ghost block in its box facing west (the driver's `boxstates`, with each
  shape's count in the log); a Floor of oak slabs flipped to the top half has every slab's top half. With Effortless
  Building's mirror across x −52, a Line of 4 turned stairs has its mirror image's stairs facing east, in the preview
  and once placed; with the mix of oak and spruce stairs, a Wall's stairs of both kinds all face west. In Disable, Left
  Ctrl with a notch down turns the stairs to face south and the hotbar slot stays, and back in Line the stairs keep
  the turn; switching to stone and back to the stairs drops it; and at the end, in Disable, the middle click with Left
  Ctrl held flips the stairs to their top half and picks no block, and with stone in hand it picks the aimed block. In Single, unlocked, with the turned stairs in hand, the preview draws one stair at the aim,
  facing east, and nothing else (the driver's `preview` and `previewstate`); with Effortless Building's array of 2
  copies one block apart upward, it draws the stair and its 2 copies once each; a turned stair at the aim is shot at 854
  by 480 and 1920 by 1080, GUI scale 2, with the stair outlined in red. During a Line drawn with the break click, the line shows only the lock and Left Ctrl with the
  wheel changes the hotbar slot; a right click cancels it, erasing nothing. Line's line, checked word for word (the
  driver's `modehud`): before the first click with stairs, with Left Ctrl held, turned, with and without Left Ctrl, for
  a chest (turn only) and a slab (flip only), each with Left Ctrl held too, and for stone with no turning control;
  locked before the first click; during a Line drawn with the place click, locked and not, turned, with and without
  Left Ctrl; during one drawn with the break click, locked and not, with no turning control; a cancelled shape keeps
  the lock. No line shows with a stick in hand; in Disable, Disable's line in its place (step 19); with the copy at the
  aim, the copy's line shows; with the sprint key on Right Control, it names Right Control; F1 hides it. During a Line
  after its first click, the line ends 2 GUI pixels above the action bar's text, which shows the shape's block count
  (the driver's `modebox` and `actionbar`), at 854 by 480, GUI scale 2, and at 1920 by 1080, GUI scale 2 and 4, each
  shot. Each of Line's lines is shot at 1920 by 1080, GUI scale 4, turned before a shape at 854 by 480, GUI scale 2,
  too, and a placed Line of turned stairs at 854 by 480 and 1920 by 1080, GUI scale 2.
- The build mode's name and the held item's name, at 854 by 480, GUI scale 2, with oak stairs in hand: choosing Wall
  in the radial menu (a real pointer on its segment of the ring, the driver's `ringat`) puts nothing in the action bar
  (hook E35). The mode's name, as the last frame drew it (the driver's `drawnbox`), shows once, its rectangle ending 4
  GUI pixels left of the hotbar and centred on the hotbar's height, in every mode but Disable, each by Effortless
  Building's own name ("Diagonal Wall", say); in Disable no name shows. "Copy", "Material list", "Paste" and "Delete"
  show there in Single's place during Copy's box, the material list's box, the copy at the aim (in Line and in Disable
  too: exactly "Paste", and the mode's name back once the copy is put away) and Delete. With a shield in the off hand, the rectangle
  ends 4 left of the off hand's slot, shot at 854 by 480 and 1920 by 1080, GUI scale 2; without it, 4 left of the
  hotbar again. The held item's name (hook M8), shown again by switching to stone and back, ends 2 GUI pixels above the
  top of the controls' highest row: in survival mode, over Line's line, shot at 854 by 480 and 1920 by 1080, GUI scale
  2 (Geo's image 1); in creative mode, over Single's line.
- Effortless Building's own line during a shape (hook E31), word for word and colour by colour as the last frame drew
  it 58 above the screen's bottom (the driver's `ebline`, `eblinecolours` and `eblinerects`, from every component the
  frame drew there): during a Line drawn with the place click, "[Esc/Left-click: cancel][Right-click: place]", each
  rectangle white with its action in dark aqua; with an iron pickaxe, a tool, in hand during that shape,
  "[Esc/Left-click: cancel][Right-click: interact]", in the same colours; during a Line drawn with the break click, "[Left-click:
  break][Esc/Right-click: cancel]", each rectangle white with its action in red; and once a click cancels the shape, Effortless
  Building's line is gone and Line's line shows again. Esc cancels a Line drawn with either click too, sending no
  edit, opening no screen, and leaving Line chosen with its line. Each is shot at noon at 1920 by 1080, GUI scale 4, and at 854 by 480, GUI scale 2,
  with every rectangle's window box in the log for the shots' outlines, and in each the rectangles are laid out as the
  controls' are, in a centred row starting 58 GUI pixels above the bottom.
- Turning real blocks in Disable (step 18), with Planner1 3 blocks south of each block, facing north, first in
  creative and then in survival: oak stairs turned a quarter turn with Left Ctrl and a notch up, and a right click with
  Left Ctrl held, place a real stair facing east, which the server's world (read with `/execute if block … run say`,
  run by Planner1, whose words the server's log shows) and Planner2 show facing east. The driver reads Planner1's own
  block at that position every tick from just before the click (`watchblock`), and it is air and then the stair facing
  east, with no flicker. An oak log flipped onto its side and turned a quarter, to lie east to west, and placed on the
  grass's top lies east to west, where Minecraft stands it up. Without Left Ctrl, the stair faces north, as Minecraft
  places it, and the stairs keep the turn. The right button held with Left Ctrl for a second places 4 turned stairs or
  more, all facing east. A torch aimed at a wall turns as the wall torch it places; turned away from the wall, it
  places nothing, on either side, and the player keeps the torch (the driver's `handcount`). A red bed turned toward a
  stone wall places nothing and leaves the wall, and the player keeps the bed. Then, in creative: a turned stair placed
  in front of a stair facing north takes the same shapes, its own and its neighbour's, as a stair Minecraft places
  facing east, with Planner1 facing east, in front of another stair facing north. An oak door turned to face east has
  both halves facing east. A ghost stair facing east is filled by the turned real stair, on both clients. With a turn
  made in Line, Disable without Left Ctrl places as Minecraft does, and with Left Ctrl places turned. Switching to
  stone and back drops the turn, and Left Ctrl then places as Minecraft does. `step8.sh` checks a game handed a
  `hello` of another protocol version: Left Ctrl with the wheel changes the hotbar slot, and the stair places with Left
  Ctrl as Minecraft places it (a `hello` in `step9.sh` would start a session without the lists, which the server sends
  only at joining). With Freecam's
  camera on, "Allow interaction" on and the camera 3 blocks east of Planner1, the turned stair goes where the camera
  aims and faces east. A turned stair and log are shot placed, at 854 by 480 and 1920 by 1080, GUI scale 2, with each
  one's box in the window logged (the driver's `screenbox`) for outlining them in red.
- Disable's preview and its line (step 19): with Left Ctrl held and oak stairs, the preview draws one stair where a
  right click would place it, facing north, and turned a notch, facing east (the driver's `preview` and
  `previewstate`), not red (`previewred`), and the stair a Left Ctrl right click places matches it (`realstate`);
  aimed at short grass, which a block replaces, the preview takes the grass's place. Without Left Ctrl, no preview
  shows. A torch aimed at a wall shows as a wall torch; turned away from the wall, it shows red. Beyond Minecraft's reach, and aimed at
  the sky, no preview shows. With Freecam's camera on and "Allow interaction" off, no preview shows; with it on, it
  shows only while Planner1 is within reach of the aimed block. With Effortless Building's mirror on, the preview
  shows one block. The lines, each checked word for word (the driver's `modehud`): without Left Control, with it held,
  turned, for a chest (turn only) and a slab (flip only), red, and none for stone or with a stick in hand; with the
  sprint key on Right Control, they name Right Control; F1 hides them. The preview of a turned stair and the red
  torch are shot at 854 by 480 and 1920 by 1080, GUI scale 2, and each line at 1920 by 1080, GUI scale 4, with the
  changes outlined in red.
- The locked block's colours and Single's line (step 20): locked over grass the block is blue (the driver's
  `copycolour`); nudged into a ghost gold block it's yellow and its click still places and ends the lock; locked again
  over the stair placed there it's blue; nudged below the world's bottom it's red, and its click sends no edit and
  ends the lock. With Planner1 following the aim, no box shows. The lines, each checked word for word: Single's line
  following the aim, with Left Ctrl held, for a chest (turn only), a slab (flip only) and stone (neither), turned,
  locked and red, with "Lock (Off)" or "Lock (On)" and Esc; with the lock key on K, the nudge forward on I and the
  sprint key on Right Control, the lines name them. No line shows with a stick in hand or with the copy at the aim (the
  copy's line shows), and in Line mode Line's line shows; F1 hides it. Each line is shot at 1920 by 1080, GUI scale 4,
  and the locked line and the locked block at 854 by 480 and 1920 by 1080, GUI scale 2.

`step10.sh`, hold to repeat, editing from Freecam and the settings Building Planner changes once, with Freecam 1.5.1 (Modrinth version
`lwrkvqym`, the pack's) on both clients. The driver counts the ticks each mouse button was down and
notes the client tick of each edit the client sends, and the blocks it changes (a mixin in the `clienttest` source
set), and the script holds the real buttons with xdotool while Planner1 is teleported east along a row, looking
straight down. The game's commands pace the teleports unevenly, so now and then two repeats edit the same block of
the row; the checks on the row count the blocks the edits changed, not the edits:

- Holding use in Single mode with stone bricks sends an edit when the button goes down and then one every 4 ticks, and
  each edit put a ghost block in a new block of the row, on both clients, and no real block.
- Holding attack over a row filled with ghost blocks sends an edit when the button goes down and then one every 6
  ticks, and each edit erased the ghost block of a new block of the row, on both clients, leaving the real blocks.
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
  (`FRONT_BACK`, where Planner1's east would give `LEFT_RIGHT`), and a left click puts it away. The radial menu opens and its show or hide button
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
warning a minute, with each of 300 columns keeping its own minute. They also cover when Litematica's rebuilds let the
self-check read a column: only after a rebuild that began at or past the change's mark has finished, never while a
later rebuild of the column runs, not after a rebuild that began during the read, and after the 30 seconds without a
finished rebuild.

`filled.sh`, ghost blocks the real world fills ("Ghost blocks the real world fills"), more than 16 blocks from the
world's spawn, where Planner2, who isn't an operator, may build:

- Real blocks placed by hand, by Planner1 and by Planner2 in Effortless Building's Disable mode, by `/fill`, by a piston
  and by water flowing into ghost water remove their ghost blocks: a tick after the real block shows, first on the
  client that learns of it from the server, then on the other, the ghost block is gone, and neither client shows a
  glow. The ghost blocks beside them stay.
- A stair placed facing the other way leaves its ghost block; one facing the same way, waterlogged, fills it.
- Breaking filled blocks brings no ghost block back.
- A ghost block of 4 candles stays with 1, 2 and 3 real candles and goes at the 4th (the driver's `layerstate` shows
  the ghost block's properties).
- Ghost oak planks drawn over real oak planks that have a ghost stone block on them remove the ghost stone on both
  clients; one Ctrl+Z brings the stone back, and Ctrl+Y removes it again.
- A paste onto built blocks (the driver's `pastehere`, which copies a box and pastes it where it was copied from, and
  gives the copy as it was pasted) changes nothing, with no glow.
- Ghost blocks saved over matching real blocks while the server ran without the add-on are gone once their chunk
  loads, and a ghost stair under a real stair facing the other way stays.
- Neither client's log has a self-check warning, checked before the restarts and at the end.
- Planner2 is shot at 854 by 480 and 1920 by 1080, GUI scale 2, looking at a ghost wall before and after real blocks
  fill half of it.
- Wrong blocks through walls ("Ghost blocks through walls"): a real cobblestone in a ghost stone brick and a real oak
  stair facing north in a ghost oak stair facing east, 4 blocks behind a real stone wall that fills Planner2's view,
  leave their ghost blocks. From 10 blocks, on the wall's other side, the stone brick draws red and the stair orange
  (the driver's `wrongblocks`, what the last frame drew), and the view differs from the same view with ghost blocks
  hidden by more than 500 pixels; with ghost blocks hidden nothing draws, and shown again both draw. From 33 blocks
  nothing draws, and from 32 both do. Both are shot at 854 by 480 and 1920 by 1080, GUI scale 2, through the wall from
  10 blocks and from in front of it. The driver's `wrongscan` times 20 scans with a ghost block at every position
  within 32 blocks of Planner2's camera, and writes the figures to the checks' log.
- "Match ghost blocks" ("Settings"): Planner1's "Visual Settings" (the driver's `visualsettings`, which opens it as the
  radial menu's button does) has the row, off at first, and its label shows the tooltip; a real click on its switch
  turns it on and saves nothing until a click on "Save", which saves `match-ghost-turn=true`. With it on, Planner1, in
  Disable 3 blocks north of a ghost oak stair facing east and facing south, places oak stairs into it: Planner1's own
  block, the server's world and Planner2 have the stair facing east, and the ghost block is gone on both clients. With
  Planner1's ghost blocks hidden, a stair placed the same way into a third ghost stair facing east faces south and
  leaves its ghost block. Turned
  off and saved, a stair placed the same way into a second ghost stair facing east faces south and leaves its ghost
  block. Turned on and saved again, the row shows it on once client 1 has restarted twice. "Visual Settings" is shot
  without and with the row's tooltip (which covers the row), and Planner2's view of both stairs, at 854 by 480 and 1920 by 1080, GUI scale 2.

`real.sh`, "Place real blocks" (proposed for 0.9.0), more than 16
blocks from the world's spawn unless a check says otherwise, with Planner1 in survival (`/gamemode`) and its inventory
set with `/clear` and `/give`. "On the server" means the dev server's own world, as `/execute if block` finds it (the
scripts' `server_has`), and every real block checked there is checked on both clients too:

- Planner1's "Settings" has the row "Place real blocks" after "Match ghost blocks", off at first; a click on its switch
  and "Save" saves `place-real-blocks=true`, and the row shows it on after a restart.
- With 10 oak planks in hand and 10 elsewhere in the inventory, a 20-block Line places all 20 as real oak planks on
  the server, no ghost block appears, and the inventory holds no oak planks. The controls line reads "[Right-click:
  start placing real blocks]" before the first click, and Single's reads "[Right-click: place real blocks]".
- With 19 oak planks, the preview of the same Line draws all 20 red and the icons beside the crosshair show 20 and a
  red 1; the click places nothing, Planner1's game plays no place sound, the notice names 1 oak plank short, and the
  inventory still has its 19.
- Oak stairs in a Wall, turned with the wheel and with Effortless Building's mirror on, face on the server as the
  preview showed them, and the stairs beside each other join as by hand. A door has both halves and a bed both parts.
  A Line of torches along a wall, aimed at the wall's side, places wall torches.
- With the mix of 80 cobblestone and 20 mossy cobblestone, each block of a 20-block Line on the server is the block the
  preview showed there when the inventory contains as many of each block as the preview uses, and nothing goes in
  when it has one mossy cobblestone fewer. With the mirror on, a Line and its mirrored copy count together: an
  inventory that covers only one of them places nothing.
- A Line through a real stone block and a cow, with oak planks for only the other blocks, leaves the stone and the
  cow's space as they were and places the rest; the preview showed both red.
- With the array set to copies 70 blocks apart, the copy past 64 blocks from Planner1's eyes isn't placed and draws
  red, and the one at 63 is placed.
- A Floor of oak planks over a ghost floor of oak planks takes those ghost blocks out of the layer; over ghost stone
  bricks, it leaves them, red.
- Planner2, who isn't an operator, places nothing within the spawn protection's 16 blocks and gets the notice; in
  adventure mode, Planner2's shape places nothing.
- In creative, a shape of 2,001 blocks places nothing and gets the cap's notice; one of 2,000 places all of them and
  uses no item. The time the server took to place it, which `RealPlacer` logs at debug level (the dev server's
  `logs/debug.log`), goes to the checks' log and "Unverified".
- Planner2's game plays one place sound for Planner1's 20-block Line (a count of the sounds the game plays, which the
  driver gains for this check).
- A copy of a hut (a cobblestone floor and walls, an oak door, a wall torch inside, a lantern hanging from its roof, a
  double stone slab, 3 candles and a water block) shows yellow (for the water) with "[Right-click: paste real
  blocks]", and, with exactly the items it needs, pastes as one `place`: on the server each block is as the copy
  shows it, the door with both halves, the torch on its wall and the lantern under the roof, the water isn't placed,
  and the inventory has none of those items left (2 stone slabs for the double slab, 3 candles). Planner1 and Planner2
  each hear one place sound. With one cobblestone fewer, the box is red, the line reads "[Can't paste: not enough
  blocks]", and a right click places nothing and sends nothing. Pasted over a real stone block, the paste leaves the
  stone and places the rest. A Line of 3 copies of a 4-block copy places all 12 blocks at its second click, as one
  `place`, and a Floor of copies over 2,000 blocks draws red with "[Can't paste: too many blocks]".
- After a ghost shape, then a Line of real blocks, Ctrl+Z undoes the ghost shape and leaves the Line's real blocks, and
  Ctrl+Y redoes the ghost shape; with no change to ghost blocks left to undo, Ctrl+Z says there's nothing to undo and
  the real blocks stay.
- With the setting on, the break click's shape erases ghost blocks and breaks no real block. Turned off, shapes and
  pastes draw ghost blocks again.
- Shots, at 854 by 480 and 1920 by 1080, GUI scale 2: "Settings" with the row, the preview with its red blocks and the
  crosshair's icons, the whole Line red for 1 oak plank short, the controls line in Single and in Line, the copy's box
  yellow and red with its lines, and Planner2's view of the placed Wall and the pasted hut.

`RealPlaceTest` covers the rule for each block: the order (the lowest level first, and a block that can't stay tried
again after the rest), the inventory (the main hand's stack first, each mix block as its own item, every block of the
mirror's and the array's copies counted, none for a block the checks leave out, 2 slabs for a double slab and as many
items as a block is made of, nothing going in when one item is short, and nothing used in creative), the reach (63,
64 and 65 blocks from the eyes along each axis and a diagonal), a space that doesn't take a block, a block no item
places, the cap of 2,000, and the setting acting only during a session whose server takes `place`, in every mode but
Disable, with an item that places a block. `CopyColourTest` covers the copy's colours with the setting on: blue on
clear ground and over ghost blocks; yellow for a real block in a space, a block past the reach, one outside the build
height or the border, and water; red past 2,000 blocks, checked first, and in survival for an inventory one item
short. `PayloadsTest` covers `place` and its limits, and `ControlsLineTest` the lines with the setting on, the copy's
included.

`local.sh`, servers without Building Planner (proposed for 0.10.0; the checks follow the options the requests
recommend), on the dev server started with the add-on left off its classpath (`start_server "$ADDON"
server-without-addon`, as `step8.sh` starts it), with both clients having the add-on. Real blocks are checked on the
server with the scripts' `server_has`:

- Both clients start a local session at joining, and the driver's counter of the custom payloads leaving the client
  counts none of Building Planner's.
- A Line of 4 oak planks draws 4 ghost blocks on Planner1's client, places no real block on the server, and draws none
  on Planner2's; Ctrl+Z takes them back, and Ctrl+Y draws them again.
- With the radial menu open, the line above "Building Planner" shows at the row E42 records: one row above the name at
  1920 by 1080, GUI scales 2 and 4, and a row above a hovered mode's description at 854 by 480, GUI scale 2. With the
  server restarted with Building Planner, it doesn't show.
- After Planner1 leaves, the area's file is in
  `building-planner/servers/localhost_25565/<Planner1's UUID>/layer/minecraft/overworld/`, and the ghost blocks show
  again after a rejoin and after the game restarts; a file made unreadable stays as it was, and its ghost blocks don't
  show.
- Ghost blocks a real block fills: a real oak plank Planner1 places into a ghost oak plank removes it; a `/setblock` of
  oak planks from the server's console in a chunk Planner1 has loaded removes another; one set while Planner1 is
  farther away than the view distance goes once Planner1 returns and the chunk loads; and with Planner1 no longer an
  operator, an oak plank placed into a ghost oak plank within 16 blocks of the world's spawn, which the server refuses,
  leaves the ghost block.
- In Disable, with oak stairs in hand, Left Ctrl with the wheel changes the hotbar slot, and with "Match ghost blocks"
  on, an oak stair placed into a ghost oak stair facing east faces the way Minecraft places it, on the server.
- With "Place real blocks" on, a Line draws ghost blocks and places no real block.
- A box counted, then saved as a list, and the material list panel come back after a rejoin.
- A copy of ghost blocks and real blocks taken on the server with Building Planner pastes as ghost blocks on Planner1's
  client after the server restarts without it, and the copy stays through a change of dimension.
- With the server restarted with Building Planner, Planner1 sees the shared ghost blocks without its own local ones;
  restarted without it again, its local ghost blocks show.
- A Litematica placement of Planner1's own, with air where a real block stands, marks that block as extra.
- Shots at 854 by 480 and 1920 by 1080: Planner1's ghost blocks on the server without the add-on, and the radial menu
  with its line, at GUI scale 4 too.

`own.sh`, the player's own worlds (proposed for 0.10.0; the checks follow the options the request recommends), with
the dev server stopped: Planner1's game opens a copy of the dev server's world from its `saves` folder
(`--quickPlaySingleplayer`), then opens it to LAN with the driver's `lan` command (`IntegratedServer.publishServer`),
and Planner2's game joins it at that port.

- Planner1's game starts a session from the world's `hello`, and its radial menu doesn't show the line above
  "Building Planner".
- A Line of 4 oak planks draws 4 ghost blocks and places no real block; Ctrl+Z takes them back, and Ctrl+Y draws them
  again.
- With "Place real blocks" on, a Line of 4 oak planks places 4 real ones.
- A real oak plank Planner1 places into a ghost oak plank removes it.
- After Planner1 leaves the world, the area's file is in
  `saves/<world>/building-planner/layer/minecraft/overworld/`, the ghost blocks show again when the world opens again,
  and Planner1's game folder has no `config/buildingplanner.properties`.
- A Litematica placement of a file in `schematics/Building Planner/` stays in Planner1's world once the session starts.
- With the world open to LAN, Planner2's game starts a session and shows Planner1's ghost blocks, a shape Planner2
  draws reaches Planner1 glowing with Planner2's name, and Planner2's radial menu doesn't show the line.
- Shots at 1920 by 1080: Planner1's ghost blocks in its own world, and Planner2 beside them in the LAN world.

`JoinRuleTest` covers the rule at joining: a server that takes none of the ten payloads (a local session), the game's
own world and a server that takes some or all of them (waiting for `hello`), a `hello` of another version (off), and a
`hello` during a local session (the server's session). `LocalServerTest`, with `LayerNetwork`'s in-order queues,
covers an edit, an erase, undo and redo, the steps kept across a rejoin, a ghost block a real block fills at a change
and at chunk load, a position awaiting the server's answer waiting and a refused one keeping its ghost block, the files
written and read back at the next session, an unreadable file never written over, and the lists kept.
`LocalFoldersTest` covers the folders: each `:` turned into `_`, a Realm, and two players' UUIDs in two folders.
`RadialLineTest` covers the radial menu line's row on screens 240, 256, 257 and 270 high.

The copy's placement has unit tests as pure functions. `CopyPlacementTest` covers the copy against each of the six
faces, with sides of odd and even lengths, the origin for each turn and mirror filling the turned box, and a turn or
mirror keeping the middle block. `PasteBoxesTest` covers snapping: flush against each face of a paste's box at the
corner nearest the aim, the most recent paste winning, only 16 kept, each copy of a row a paste of its own, and the guide line along a row and a column on the
latest paste's level.

The copy's keys have unit tests too. `CopyNudgeTest` covers each nudge key's direction from the view's yaw: forward the
nearest compass direction, back its opposite, left and right its sides, up and down whatever the facing. `KeyRepeatTest`
covers a key's repeat: a press acts at once, a held key again after 4 ticks and then every 4 (5 a second), the
computer's own key repeat not counting while the key stays down, and each new press acting at once.

The copy's set has unit tests as a pure function too. `CopyGridTest` covers R's order of the modes; Off as the first
copy alone; a Line up and down its height apart, and east, west, north and south its length apart; a block within the
first copy as the first copy alone; a Wall as a grid in its upright plane and a Floor as a grid on the first copy's
level, each row by row from the first, in each direction; at most 10 copies along each direction, so 100 in a Wall or
a Floor; a turned copy its turned size apart, the copies touching without overlapping; and the box around the set.

The copy's colour has unit tests as a pure function too. `CopyColourTest` covers a copy on clear ground and one beside
ghost blocks, blue; a copy whose block would replace a ghost block, yellow, but not over a ghost block the same as its
own; a set yellow when any of its copies is; a copy partly above or below the build height and one partly outside the
border, red, and one just inside each, blue; red over yellow; a set over the block cap, red, and one at the cap, blue;
a set reaching past the build height while its first copy doesn't; the cap checked first, as on the server; and ghost
blocks read only while no red check fails. `LayerTest` covers the layer's version, which changes with each change and
only then.

`saved.sh`, saved copies (proposed for 0.11.0),
on the dev server, with the server restarted without the add-on for the check between servers. The driver reads
`index.dat` and the copies' folder for its checks, and its `bindkey` rebinds the key:

- A copy of a hut of oak planks, with a door and a wall torch, puts `1.litematic` and `index.dat` in
  `building-planner/copies/<Planner1's UUID>/`, the index naming it by its time, not kept, with the hut's size and block
  count. Planner2's game has no saved copy.
- With the copy at the aim, its controls line ends with "[V: saved copies][Esc/Left-click: close]", and during a Line
  set with "[V: saved copies][Esc/Left-click: cancel]"; with the key unbound, the control is gone and V opens nothing;
  rebound to K, the line names K.
- V opens "Saved copies" with the hut's row: its time as its name, and its size and block count. V does nothing with
  the inventory open. V during a box for Copy opens the screen, "Close" goes back to the game with the box still drawn,
  and "Paste" on a row ends the box and brings that copy to the aim.
- "Rename" on the hut's row shows its name in the field; an empty name or only spaces greys "Save" out, the field
  accepts no 65th character, and "Hut " saves as "Hut", kept in the index. Eleven more copies, of one block each and
  each of another block, leave 11 rows, "Hut" and the 10 newest: the first of the 11 is gone, and its file is
  deleted.
- "Paste" on "Hut" closes the screen and brings the hut to the aim, unlocked and unturned, in the copy mode last set;
  a right click pastes it, and Planner2 sees its blocks; the radial menu's Paste button then brings the hut back.
- "Delete" on "Hut" asks "Delete the copy "Hut"?"; "Cancel" leaves it, and "Delete" removes the row and its files. A
  copy's `.litematic` cut short drops from the list when it's picked, with the message "That copy couldn't be read.",
  and is renamed with `.unreadable`; an unreadable `index.dat` is renamed the same way, and the screen says "No saved
  copies yet.".
- After Planner1's game restarts, the rows come back, and the radial menu's Paste button brings the newest copy to the
  aim; with the folder emptied, it says "Nothing to paste. Use Copy first.".
- With the server restarted without the add-on, Planner1's local session shows the same rows, and a saved copy pastes
  there as ghost blocks.
- Shots at 854 by 480 and 1920 by 1080, GUI scales 2 and 4: "Saved copies" with 11 rows scrolled to its top and its
  end, "Rename copy", the delete question, the empty screen, and the copy's controls line with the key.

`CopyHistoryTest` covers the rule for which copies stay: a new copy at the top, named by its time; the 11th copy not
renamed dropping the oldest one not renamed, the renamed ones not counting; a renamed copy kept through any number of
new copies; a delete of a renamed and of a recent copy; and the files' numbers never used twice. `CopyIndexTest`
covers `index.dat` written and read back, and an unreadable one starting an empty list. `ControlsLineTest` covers the
key's control in each of the copy's lines, and its absence while the key is unbound.

The controls lines have unit tests as a pure function too. `ControlsLineTest` reads each line in English from
`en_us.json`: Off before and after the first paste, locked with and without a paste to line up with (no "line up",
and "H: Lock (On)"), Line, Wall and Floor before the first click, locked or not, a set of 4, of 1 and of 16, locked or
not, after it (no "line up"), each red reason in place of the paste or the set's (and a red Line or Floor, locked or
not, giving its reason before "set the start"), each control as a rectangle of its own with no label, the reason as a warning and the actions as actions, a
warning above the controls as a line of its own in red, keys a player rebound, the nudge keys off the arrow
keys named one by one, the up and down keys named with their shared first word dropped ("Page Up/Down", "Keypad
8/2") or in full ("U/J"), the nudge phrase with only one of its two parts, unbound keys left out and the phrase gone
with all six unbound, each box's lines, the tool's name beside the hotbar ("Copy", "Material list", "Delete", and
none for the copy or no mouse mode), and every controls line Geo approved for 0.7.0, exactly (1313). `PayloadsTest` covers the
erase's message writing 16,001 with digit grouping. For the new features (steps 13, 17, 19 and 20),
`ControlsLineTest` also covers Delete's lines (no marks, marks, unmarking, the box marking and unmarking with its
size, the cap's warning, a box's limit as a warning, the actions as actions, and the sprint key as a player rebound
it or left out unbound), the turning line (each turn phrase, with Left Control held, the sprint key as a player rebound it, the actions as actions, and no line for a block that neither
turns nor flips or with the sprint key unbound), Disable's lines (without Left Control, with
it held, each turn phrase, the red "Can't place here" as a warning, the actions as actions, the sprint key as a player rebound it, no line
without Left Control for a block that hangs on the block behind it, and none for a block that neither turns nor
flips or with the sprint key unbound), and the build modes' lines (Single's and the other modes' before the first
click, locked, each turn phrase with and without Left Control, the red reason as a
warning in place of the right click, during a shape drawn with the place click and with the break click, locked or
not, keys a player rebound, and unbound keys left out), with Esc before
the click that cancels a shape, the left click for one drawn with the place click ("[Esc/Left-click: cancel]") and the
right click for one drawn with the break click ("[Esc/Right-click: cancel]"), and before each box's right click.

Delete's marks have unit tests as pure functions. `DeleteMarksTest` covers a sweep between two aims
10 blocks apart marking all 10 ghost blocks between them, and none behind a real block; at most 65 aims for a frame's
half turn; an aim at nothing or at a real block marking nothing; a sweep that starts on a mark unmarking until it
ends; a box marking every ghost block between its corners at every height, a box started on a mark unmarking the
marks in it and leaving the rest, a hold's sweep deciding before its box does, a box that would pass the cap marking
nothing, and a box counting the other half of a door it cuts; a door marked by its lower half as one mark and two
positions to erase; the cap, with the other halves counted; marks dropping when their ghost blocks leave the layer,
and staying as they were when none did; and the changes the outline rebuilds from, each mark, unmark and dropped
mark, and all of them after a clear. `MouseModeTest` covers which actions end Delete. `EraseHoldTest` covers the hold:
19 ticks erase nothing, the 20th erases once, holding on erases nothing more, a release starts the count again, a hold
cancelled when its marks go erases nothing, and the ticks held, which the progress bar fills by.

The build modes' turning and lock rules have unit tests as pure functions (steps 14 to 17). `SingleRuleTest` covers
when the turning controls are active (`SingleRule.turning`: every mode but Disable, where step 18 adds `DisableTurn`'s rule; none without a session, with a
mouse mode on, with an item that places no block, or during a shape drawn with the break click, but during one drawn
with the place click). `ShapeLockTest` covers when the lock's controls are active (a session, any mode but Disable, no
mouse mode, an item that places a block), when the first click's block alone is drawn, the lock off until H, the
locked position and hit taking the first click, no click taken during a shape or while the controls aren't active, a
nudge moving the position and the hit point one block, unlocking, and a shape's first click moving with a nudge.
`EscStepsTest` covers the order of Esc's steps (see "Esc"): during a copy's set, dropping the set, putting the copy
away, Disable and the pause; in Delete, ending its box, clearing the marks and ending Delete; a box ending before
anything else; a shape cancelled before Disable; and the pause in Disable with no mode on.
`BlockTurnTest` (step 16) covers a
quarter turn each way for stairs, logs, chests and signs, and two turns adding up; the flips' order for a log, a
stair, a slab (a double slab flipping to bottom), a piston (one placed facing up flipping to the way it would face on
its side, and a turn while flipped up turning the way it goes back to), a button and a lantern; the turn kept as
property values and set on another block of the mix that has them, leaving a door's upper or lower half and
waterlogging; which blocks turn and which flip; a block that neither turns nor flips; and the turn dropping when the
item in hand changes. `CopyColourTest` covers a locked
position blue, yellow and red.

Disable's turn has unit tests as pure functions (steps 18 and 19). `DisableTurnTest` covers when Disable's turning
controls are active (a session, Disable, no mouse mode, a block that turns or flips) and when not; the turn M6 sends:
the turn while Left Ctrl is held and the controls are active, and none without Left Ctrl, with the controls inactive
or with no turn; and a payload only when the item or the turn's values differ from the last one sent.
`PlaceTurnTest` covers the turn S2 applies: only for the turn's item, in the main hand; no turn for a player without
one, and no item clearing it; property names outside `BlockTurn`'s and values the block doesn't have refused; the
shape set again from the neighbours for a stair but not for a door, a bed or a tall plant; a bed's head toward the
turned facing, and the bed staying only where that space takes a block; and another block staying where Minecraft's
own check lets it. `DisablePreviewTest` covers when the preview shows: only while the turning controls are active,
the sprint key is held and Freecam doesn't keep the click from the world, by Freecam's rule; the reach, the range and
one block more of the player's eyes, not the camera's; and which blocks hang on the block behind them and which don't
(a grindstone, a wall skull or a hanging sign on a wall). Where the preview goes is Minecraft's own
`BlockPlaceContext.getClickedPos`, checked in the game. `GhostMatchTest` covers "Match ghost blocks": an oak stair
placed facing south into a ghost oak stair facing east takes the ghost block's facing and half; with the setting off,
with ghost blocks hidden, into a ghost spruce stair, with no ghost block or nothing placed, and for a ghost block with no orientation, no turn; a
torch placed as a wall torch matches a ghost wall torch and not a ghost torch; a ghost block's turn that can't stay
leaves the click with the turn it has with the setting off, Left Ctrl's or none; and the setting on only when saved as
`true`.

The fill rule's unit tests (`FillRuleTest`) cover each property it compares or ignores: a stair facing another way or
upside down, a log on another axis, a door with the other hinge or its other half, a bed's other part, a button on
another face, a sign turned another way and a hopper, piston, dripstone tip and crafter facing another way stay; a
stair of another shape or waterlogged, a fence or wall with other connections, a waterlogged slab, a crop of another
age, an open or powered door, an occupied bed, a lit furnace and an extended piston fill it; a top or double slab
doesn't fill a bottom one; air fills nothing; and for each of candles, sea pickles, snow layers, turtle eggs, pink
petals and leaf litter, a block of another count stays and one of the same count fills. `WrongBlocksTest` covers which
ghost blocks draw through walls: over another block red, over the same block facing another way orange, and over the
same block, air, water or lava, or with no ghost block, nothing; and the range, a block 10 away and one 32 away on
every axis drawing, and one 33 away on any axis not. `LayerFillTest` covers the end-of-tick edit (by the nil author with no
client sequence, to every watcher, only for blocks still filled when the tick ends, split at the block cap), an edit's
filter (a position a real block fills takes air instead of the new ghost block, unless the server hasn't loaded its
chunk, so a ghost block drawn over a real block that fills it removes the older ghost block there, one undo brings it
back, a redo removes it again, and a redo over blocks built since does the same; a paste onto built blocks with no older
ghost blocks changes nothing and leaves no undo step), the candle count rule at the end of the tick, undo and redo skipping positions built since, the undo notice counting only
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
- How long the copy's yellow check takes for a large copy moving over ghost blocks: each time the copy moves over a
  column of the layer, it reads the blocks of every copy in the set until one would replace a ghost block, up to the
  block cap of 200,000; expected a few milliseconds, not measured.
- For saved copies (proposed for 0.11.0): how long writing and reading a large copy takes on the background thread, and
  how large its file is: Litematica's file stores every position of the box, at 2 bits or more each, so a box of 256 a
  side is at least 4 MB before compression, not measured.
- That Ctrl with the middle click in creative mode picks a block with its contents in Minecraft 26.3 (expected, from
  earlier versions; the client's `pickBlockOrEntity` not read for it), which hook M4 stops while it flips the block
  in hand.
- That moving a shape's set positions mid-shape (hook E33) moves its preview and its placed blocks in every mode of
  three clicks: Effortless Building's `findCoordinates` reads `firstBlockEntry` and `secondBlockEntry` each frame (its
  source read, not run), and no mode was seen keeping a copy of them.
- That hook E37 reaches every mode's second and third positions: `TwoClicksBuildMode.findCoordinates` and
  `ThreeClicksBuildMode.onClick` and `findCoordinates` are the only callers of `findSecondPos` and `findThirdPos`, and
  no mode was seen overriding them (source read, not run).
- That a locked step's position, moved by nudges where the aim could never put it, gives each mode the shape the
  directions in "Locking and nudging" describe: read in Line, Wall, Floor and Cube, not in Circle, Cylinder, Sphere,
  Diagonal Line, Diagonal Wall and Slope Floor.
- That turning off `statusInfoHudAuto` keeps Litematica's status panel off in every case Geo saw it: read in
  `StatusInfoRenderer` (Litematica 0.29.1), not run.
- That hook E32's wrap of `resolveFirstClickPos` reaches every mode's first click: `handleClick` and
  `getPreviewBlocks` both call it (source read), and no mode was seen finding its first position another way.
- That Esc reaches hook M7 with no screen open while another mod's key handler also reads Esc; no mod in the pack was
  seen doing so.
- That Effortless Building's "Functional Settings" button is the only one the radial menu gives operators alone
  (source read); Planner1 and Planner2 are made operators for the check of hook E34.
- What drawing ghost blocks through walls costs on players' computers (`ThroughWalls`, rescanned every 10 ticks
  within 32 blocks, up to 65 by 65 by 65 positions a scan in the sections that hold ghost blocks): measured only in the
  test harness's container, where a scan with a ghost block at all 274,625 positions takes 4.5 ms at the median of 20
  scans and 26.9 ms at the slowest. Building and drawing the shapes, with missing blocks among them and only changed
  sections rebuilt, aren't timed yet.
- That a two-part block placed with "Match ghost blocks" on (a door, a bed) takes the ghost block's facing for both
  parts, as one turned with Left Ctrl does; read, not run.
- That no Litematica or MaLiLib hotkey acts on Left Ctrl with the wheel or the middle button while Litematica's tool
  item is off, as Building Planner sets it.
- Satisfactory's mass dismantle as Delete follows it: marking by sweeping the aim with Ctrl held rests on one
  player's post from 2020, not the official wiki; whether holding the left button for a second also confirms a mass
  dismantle isn't stated; and the colour of Satisfactory's marks isn't given.
- Turning real blocks in Disable (see "Turning real blocks in Disable"), read in the 26.3 jars' bytecode and Effortless
  Building's source, not run: that the client's prediction and the server's block match with no flicker; that
  `place_turn` reaches the server before the `ServerboundUseItemOnPacket` sent right after it (both go out on the one
  connection, in order, as the layer's snapshots and edits rely on); that `Block.updateFromNeighbourShapes` gives a
  turned stair the shape Minecraft gives one placed facing that way; and that nothing on the server places blocks
  through `BlockItem.place` with a player who has a turn other than that player's own right click (a dispenser's
  context has no player).
- "Place real blocks" (proposed for 0.9.0), read in the 26.3 jar and Effortless Building's source, not run: that
  `BlockItem.place` with a context at each of a shape's positions places as the player's right click would there; how
  long the server takes to place 2,000 blocks in one tick; that `Block.asItem` gives the item that places every block a
  shape can hold; and how often a shape or a paste of signs opens the sign editor, which Minecraft opens for the player
  as each sign is placed. Not read: that `StandingAndWallBlockItem` picks its form from where the player looks and that
  S2 can give the shape's form in its place; which blocks are made of several items (a double slab, candles, sea
  pickles, snow layers, turtle eggs, pink petals), each to be listed from the 26.3 jar's block state properties; and
  that a paste's context, the top face with a hit at each block's bottom middle, gives each block of a copy its own
  state once S2 turns it.
- Servers without Building Planner (proposed for 0.10.0), not run:
  - That `ClientPlayNetworking.canSend` at joining (`ClientPlayConnectionEvents.JOIN`) already knows every channel a
    server running Building Planner takes, so no such server starts a local session; a `hello` that arrives during one
    replaces it, should the check be early.
  - That the 26.3 client's `BlockStatePredictionHandler` tracks each position the game placed until the server answers,
    so hook C1's check can wait for it: its methods (`startPredicting`, `retainKnownServerState`,
    `endPredictionsUpTo`) were seen in the jar, their bytecode not read.
  - How long the game takes to read a server's files at joining, on its own thread: expected a few milliseconds for a
    player's own ghost blocks, not measured.
  - MaLiLib's `StringUtils.getWorldOrServerName` read in its source (`sakura-ryoko/malilib`, branch `26.3`), not run
    for a Realm or a LAN world. Behind a proxy that joins several servers under one address (Velocity, BungeeCord), all
    of them share one folder, and whether the game sees a new join at each switch isn't known.
  - Where hook L7 can read the block's position: `getOverlayType` has none, so the hook may go on its caller; Litematica's
    source not read for it.
  - That Fabric API's server events and networking (`ServerLifecycleEvents`, `ServerPlayConnectionEvents`,
    `ServerPlayNetworking`) work on the game's own server, for the player's own game and for LAN guests, as on a
    dedicated server: expected from Fabric API, not run with Building Planner.
  - The radial menu line's row: Effortless Building's `RadialMenu.drawTexts` read in its source (branch `26.3`), not
    drawn in the game.
- How Freecam 1.5.1 stops a right click while "Allow interaction" is off, and that `Aim` can read the option by
  reflection, as it reads "Player control"; its source wasn't read for either.
- That every orientation property `BlockTurn` stores is a separate property from the one a two-part block uses for its
  part: a door's and a tall plant's `half` (`DoubleBlockHalf`) shares its name with a stair's `half` (`Half`), so
  `BlockTurn` and `PlaceTurn` go by the property, not its name.
- What Delete's marks cost on a player's computer, with a graphics card. In the in-game checks' container (6
  CPUs, Mesa's software OpenGL; `step9.sh`'s log), a frame's sweep of 65 aims took 0.65 ms on average, and 1.2 ms when
  every aim reached the horizon. Building the whole shape of 200,000 marks took 59 ms for a solid block of them and 546 ms for 200,000 that touch no other; building it again after one mark
  changed, as in a frame of a sweep, took 0.6 ms on average, and at most 14 ms for marks that touch no other. Frames
  over 200,000 ghost blocks took 86 ms on average with none marked and 90 ms with all of them marked, times that
  software drawing sets. With a graphics card, expected well under a frame; not measured.
