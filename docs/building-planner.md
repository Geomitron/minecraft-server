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
- the radial menu's Delete button and its box (see "The Delete box"), and a second Delete mode, Aim, that marks the
  ghost blocks the player looks at and erases them on a confirm (see "Delete by aim");
- the copy's controls for one ghost block in Effortless Building's Single mode (locking and nudging, and Zoop and
  Vertical rows), and turning and flipping the block in hand with Left Ctrl held, in every mode, and in Disable the
  real block a right click places while Left Ctrl is held (see "One ghost block");
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
- **One ghost block.** In Single mode, with a block in hand, the copy's controls work for the one ghost block at the
  aim, as if it were a copy of one block, while Single's buttons stay: H locks it in place, and the arrow keys, Page
  Up and Page Down nudge it; R switches it between Single, Zoop and Vertical, whose rows of up to 32 blocks go down
  with two right clicks. A line of these controls shows at the bottom of the screen (see "One ghost block").
- **Turning and flipping.** In every mode, with a block in hand, while Left Ctrl is held, the wheel turns the block
  and the middle click flips it to the other ways it can sit (a log's axis, a stair's top or bottom half). Every shape
  and its preview take that turn, in each mode but Disable, until the player switches to another block. In Disable,
  holding Left Ctrl shows the block where a right click would place it, turned, and a right click with Left Ctrl held
  places the real block that way (see "Turning real blocks in Disable"). These controls show at the bottom of the
  screen, in Single mode's line or in a line of their own in the other modes (see "One ghost block").
- **Hold to repeat.** In Single mode, holding the place button keeps placing ghost blocks and holding the break button
  keeps erasing them. Two sliders in Effortless Building's radial menu, "Build speed" and "Break speed", set how fast,
  from 1 to 20 blocks a second; at first they're the game's own pace for a held button, five a second to place and
  just over three to erase. The first repeat waits at least 0.2 seconds, the game's own wait before a held click
  repeats, so a quick click places or erases one block at every speed. The other modes act once for each click, and so
  does Single mode while its ghost block is locked or in Zoop or Vertical (see "One ghost block").
- **Disable.** With "Disable" chosen in Effortless Building's radial menu, clicks act as they do without mods, placing
  and breaking real blocks, even with the block mix on, except that a right click with Left Ctrl held places the block
  turned (see "Turning and flipping").
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
  ghost block in it as one change, which one undo takes back (see "The Delete box"). R switches Delete to Aim, as in
  Satisfactory's dismantle: holding Left Ctrl and sweeping the aim marks each ghost block the crosshair passes over,
  and holding the left button for a second erases the marked ones as one change (see "Delete by aim").
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
  button and slider labels, the panel) is in the wording Geo approved, which this spec quotes, except the texts "One
  ghost block", "Delete by aim" and "Keys" mark as proposed, which go to Geo with their build's screenshots.

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
  limits, the hold-to-repeat timer, the look defaults' rules, the turns of the block in hand (`BlockTurn`), the turn
  of a real block placed in Disable (`PlaceTurn`, and hook S2, which both sides run) and the marks of Delete by aim
  (`DeleteMarks`).
- **Server:** the layer store and its files, column watching, edits and their order, per-player undo, each player's
  material lists and their files, each player's turn for Disable (`PlaceTurns`), and the one-time import of the plans
  Syncmatica shared. They run only on a dedicated server, which is how the group plays.
- **Client:** the player's copy of the layer and its column watching, the tiles that hand it to Litematica and the
  hooks that hide them, the show or hide switch, the Effortless Building capture, hold to repeat, the radial menu's
  buttons and sliders, the block mix and its panel, copy and paste, the controls for one ghost block, Disable's turn
  and its preview (`DisableTurn`, `DisablePreview`), the Delete box
  and Delete by aim, the material list with its panel and saved lists,
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
| E1 | `buildpipeline/BuildPipelineClient.java`, `handleClick` | `@WrapWithCondition` on `PacketHandler.sendToServer(PlaceBuildModePacket)`, reading the local `BlockSet blocks` | Take the finished shape (every entry, after mode, modifiers, mix and constraints) into the layer; don't send the packet. `getFirstClickHit()` is still set here, so block states use the first click's face. In every mode each state takes the turn of the block in hand (`BlockTurn.apply`, see "One ghost block") after its placement rules and before the entry's mirror and turn from the modifiers (`BlockEntry.applyTransforms`). |
| E2 | same method | `@WrapWithCondition` on `PacketHandler.sendToServer(BreakBuildModePacket)` | Erase the ghost blocks among the shape's positions, in the game's copy of the layer, with the other half of each two-block ghost block (`GhostErase.of`); don't send. A shape over no ghost blocks sends no edit. |
| E3 | same method | `@WrapWithCondition` on `PlacedBlockTracker.clientTrackAll` | Don't record ghost positions as real blocks the player placed. |
| E4 | `network/PacketHandler.java`, `sendToServer(UndoPacket)` and `sendToServer(RedoPacket)` | `@Inject` at head, cancellable | Route undo and redo to the layer. Covers both the Ctrl+Z/Ctrl+Y keys (`fabric/.../EffortlessBuildingClient.java`, client tick) and the radial menu (`buildmode/ModeOptions.java`, `performAction`). |
| E5 | `buildpipeline/RandomizerSystem.java`, `processBlocks` | `@Inject` at head, cancellable | While the mix applies, give each entry its block from the mix (`entry.item`, `entry.blockState`), chosen by a fixed hash of the position so the preview and the result match. |
| E6 | `buildpipeline/BuildPipelineClient.java` `shouldInterceptPlacing`, `updateDisplayTrackers`; `render/BlockPreviewRenderer.java` `render` | `@ModifyExpressionValue` on `instanceof RandomizerToolItem` | While the mix applies, treat the held block as Effortless Building's Randomizer, so the preview draws each entry's own block. In `shouldInterceptPlacing` this holds only while the mode isn't Disable (`BuildModes.CLIENT.getBuildMode() != BuildModeEnum.DISABLED`), so clicks in Disable stay vanilla's. |
| E7 | `buildpipeline/ConstraintSystem.java`, `processBlocks` | `@ModifyExpressionValue` on the `instabuild` read, and on `getProtectTileEntities()` | Skip the survival checks on real blocks (placed-by-player, hardness, tools) and the protected-block-entity check, which concern real blocks, not ghost blocks. World border, build height and the block cap still apply. |
| E8 | `buildpipeline/BuildPipelineClient.java`, `updateDisplayTrackers` | `@WrapOperation` on both `ItemUsageTracker.compute` calls and on `BreakDisplayTracker.compute`, clearing the tracker instead | No red "missing items" tint: ghost blocks don't use up items. No list of the real blocks under a shape drawn with the break click either (`render/RenderHandler.java`, `drawBreakingStacks`, which draws them in one row from the screen's middle that never wraps): that shape erases ghost blocks and leaves those real blocks. In Disable's preview, it marks the one block missing when the turned block can't stay, so it draws red (see "Turning real blocks in Disable"). |
| E9 | `buildpipeline/BuildPipelineClient.java`, `handleClick` and `getPreviewBlocks`; `resolveFirstClickPos` | `@WrapOperation` on `ClientLevel.clip`; on `Level.getBlockState` | Let shapes start on ghost blocks: aim at the nearer of the real block and a ghost block (`GhostAim`, which traces the ray through the player's copy of the layer while ghost blocks show, not through Litematica's schematic world, which also holds the copy and shows an edit only after its rebuild; so a held click's repeat aims at the ghost blocks the click before it placed, as on real blocks), and read the ghost block's state, where the real block can be replaced, when picking the start position, so the shape goes beside it. |
| E10 | `buildpipeline/BuildPipelineClient.java`, public static `handleRightClick` and `handleLeftClick`, with `shouldInterceptPlacing`, `shouldInterceptBreaking` and `getBuildState`; `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem`; `buildmode/BuildModes.java`, `getBuildMode`; the click checks copy `fabric/.../EffortlessBuildingClient.java`'s client tick | No mixin: Building Planner's own `END_CLIENT_TICK` calls the two public methods again while a button stays down | Hold to repeat in Single mode (see "The client"). Effortless Building's own tick acts only when a button goes down (`rightJustPressed`, `leftJustPressed`). |
| E11 | `screen/RadialMenu.java`, `init()` | `@Inject` at tail, calling `Screen.addRenderableWidget` | Add Building Planner's buttons and sliders to the radial menu each time it opens (Minecraft clears a screen's widgets and calls `init` on each open). They're children of the screen, so `RadialMenu.mouseClicked`, which calls `super.mouseClicked` after its own `performAction`, routes clicks and drags to them. |
| E12 | `screen/RadialMenu.java`, `extractRenderState(GuiGraphicsExtractor, int, int, float)` | `@Inject` before its call to `drawTexts` | Place those widgets for the opening animation (E20) and draw them: the method draws the menu itself and never calls `Screen`'s own drawing, which would draw them. Drawn before the menu's texts, they leave a hovered mode's name, which can fall on the sliders, on top. |
| E13 | `buildpipeline/BuildPipeline.java`, `isBuildTriggerItem` | `@ModifyReturnValue` | On a server (with a session, or with Building Planner off), an item starts shapes only if it places a block (`ClickRule.placeable`: Minecraft's `BlockItem`, the same test as the mix's; `ClickRule` uses none of Litematica's classes, so these hooks can ask it while Building Planner is off). Effortless Building's own rule also takes buckets and tools with a `TOOL` component, and leaves out doors, beds and tall plants. |
| E14 | `buildpipeline/BuildPipelineClient.java`, `shouldInterceptPlacing` and `shouldInterceptBreaking` | `@ModifyReturnValue` | While the player draws a box or the copy is at the aim (`MouseMode`), neither click is Effortless Building's in any mode, so hooks M1 to M5 take them. With the same rule as E13, while the main hand holds no item that places a block and no shape is under way (`getBuildState()` null), neither click is Effortless Building's either: its client tick, its `AttackBlockCallback`, and its cancelling of vanilla's `startUseItem`, `startAttack` and `continueAttack` all ask these first, and so does hold to repeat, so both clicks and holding them are vanilla's. A shape under way still takes the click that finishes or cancels it. In Single mode, the place click isn't Effortless Building's while the one ghost block is locked or in Zoop or Vertical, and the break click isn't while the block is locked or a row is fixed (`SingleBlock`), so hooks M2 and M1 take them (see "One ghost block"). |
| E15 | `Minecraft.pick(float)`, into which Effortless Building's `MixinGameRenderer` adds, at its tail, a reach of 32 for an empty hand, aiming through entities; `buildmode/BuildModes.java`, `getBuildMode` | `@WrapMethod` on `pick`; `@ModifyReturnValue` on `getBuildMode` | While the main hand holds no item that places a block and no shape is under way, or while a box is drawn or the copy is at the aim, `getBuildMode` reads Disable during the pick, which that code skips, so the hand aims as without mods, at an entity before the block behind it. |
| E16 | `screen/RadialMenu.java`, `onClose()` | `@Inject` at head, finding the button under the pointer, and at tail, acting | Let Building Planner's buttons act when the menu closes, as the menu's own do: its `tick` calls `onClose` once its key is let go (Escape calls it too), and `onClose` acts on the menu's button under the pointer unless a click acted since the menu opened (`performedActionUsingMouse`). Building Planner's button is found as `onClose` begins, before closing grabs the mouse and moves the pointer to the window's middle, and acts after the menu has closed, like the menu's own, so "Block mix" can open its screen. |
| E18 | `screen/RadialMenu.java`, `drawTexts` | `@ModifyConstant` on the string `"Effortless Building"` | The menu's corner text, which the method draws right-aligned from the string's width, reads "Building Planner" (`buildingplanner.ui.radial.credits`). It is the only place the game shows Effortless Building's name to a player on this server: its welcome message and its build mode hint come from its server side (`config/WelcomeMessageStorage.java`, `config/BuildModeHintStorage.java`), which the server doesn't run, and its screens' titles ("Build Modes", "Modifier Settings") don't name it. |
| E19 | `screen/RadialMenu.java`, `drawRadialButtonBackgrounds` | `@ModifyExpressionValue` on `BuildModeEnum.ordinal()` | While a box is drawn or the copy is at the aim, no mode segment shows selected (the method compares each segment's index with the mode's `ordinal()`), since the player is in Building Planner's mode, whose button shows selected instead. |
| E20 | `screen/RadialMenu.java`, private field `visibility` | `@Shadow`, read by E12 | The menu's opening animation: `extractRenderState` adds 0.4 × the frame's partial tick to `visibility` each frame, up to 1, and places its ring and its buttons at 0.8 + 0.2 × `visibility` of their distance and size from the middle, while the screen darkens. E12 places and sizes Building Planner's widgets by the same factor before drawing them. |
| E21 | `buildmode/BuildModes.java`, `setBuildMode` | `@Inject` at head | Choosing a mode (the radial menu's `performAction` is its only caller outside `BuildModes`) ends a box and puts the copy away, even when the player chooses the mode they had. |
| E22 | `render/BlockPreviewRenderer.java`, `render` | `@Inject` at head, cancellable | No shape preview while a box is drawn or the copy is at the aim: the method draws the current mode's first block at the aim each frame (`BuildPipelineClient.getPreviewBlocks`). |
| E23 | `utilities/KeyBindings.java`, `<clinit>` | `@ModifyArg` on the category of each of the four `KeyMapping` constructors | Effortless Building's keys go under Building Planner's heading in Controls (see "Keys"). |
| E27 | `buildpipeline/BuildPipelineClient.java`, public static `getPreviewBlocks` | `@ModifyReturnValue` | While the one ghost block is locked, or a Zoop or Vertical row of it is fixed (after its first click), the preview draws the locked block or the row (`EffortlessShapes.previewBlocks`, run through Effortless Building's modifiers, mix and constraints), not the block at the aim (see "One ghost block"). E30 lets it draw a locked block that's a set of one. |
| E28 | `render/BlockPreviewRenderer.java`, `getPlacementState(BlockItem, Minecraft, ItemStack)` | `@ModifyReturnValue` | In every mode, the preview's block takes the turn of the block in hand (`BlockTurn.apply`), as E1's states do, so the player sees the block as it will be placed. `BlockPreviewRenderer.render` makes one state through this method for the held block, or one for each of the mix's blocks, and applies each entry's mirror and turn to it after (`BlockEntry.applyTransforms`), so every block of a shape's preview takes the turn. In Disable, while Disable's preview shows, it returns the state a right click would place (`DisablePreview.state`, see "Turning real blocks in Disable"). While the one ghost block is locked or its row is fixed, it returns the state a right click places there: the placement rules with the hit it was locked or its row fixed with and the player's facing now (`EffortlessShapes.previewState`, see "One ghost block"). |

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
| M1 | `Minecraft.java`, `startAttack()` | `@Inject` at head, cancellable, returning false | While a box is drawn, the left click sets its next corner; while the copy is at the aim, it pastes the copy; in Delete by aim, it starts the one-second hold that erases the marks. With the one ghost block's row fixed, it drops the row, and while the block is locked, it erases every ghost block a right click would place: the locked position and the copies Effortless Building's mirror, radial mirror and array make of it (1074 Q1 A). None attacks or breaks. `handleKeybinds` calls it once for each press of the attack key. |
| M2 | `Minecraft.java`, `startUseItem()` | `@Inject` at head, cancellable | While a box is drawn or the copy is at the aim, the right click leaves that mode and uses nothing; during a zoop, after its first click, it drops the row and leaves the copy at the aim. In Delete by aim it clears the marks, or leaves the mode when there are none. While the one ghost block is locked or in Zoop or Vertical, it places the block where it's locked, fixes the row's start, or places the row. `handleKeybinds` also calls `startUseItem` every 4 ticks while the use key stays down (`rightClickDelay`), so the use key does nothing more until it's let go. |
| M3 | `Minecraft.java`, `continueAttack(boolean)` | `@Inject` at head, cancellable | Holding the left click breaks nothing while a box is drawn, the copy is at the aim, Delete by aim is on, or the one ghost block is locked or its row is fixed. |
| M4 | `Minecraft.java`, `pickBlockOrEntity()` | `@Inject` at head, cancellable | While the copy is at the aim, the middle click (the pick block key) mirrors the copy; while a box is drawn or Delete by aim is on, it does nothing. While the turning controls are active (every mode, Disable's as "Turning real blocks in Disable" describes), with a block in hand that flips and Minecraft's sprint key held, it flips the block (see "One ghost block"). |
| M5 | `MouseHandler.java`, `onScroll(long, double, double)` | `@WrapWithCondition` on `Inventory.setSelectedSlot`, reading the local `wheel` | While the copy is at the aim, each notch of the wheel turns the copy a quarter turn, instead of changing the hotbar slot: clockwise seen from above for a notch up, counterclockwise for a notch down, as Satisfactory's wheel turns a hologram. While the turning controls are active (every mode, Disable's as "Turning real blocks in Disable" describes), with a block in hand that turns and Minecraft's sprint key held, each notch turns the block the same way. The method reaches that call only with no screen open and the player not a spectator. |

The hooks go by Minecraft's attack, use and pick block keys, so a player who moves one of them to another button finds
the copy's controls there too. Effortless Building's own hooks into `startUseItem`, `startAttack` and
`continueAttack` (`mixin/MixinMinecraft.java`) and its client tick ask `shouldInterceptPlacing` and
`shouldInterceptBreaking` first, which hook E14 answers false in these modes. The copy's own keys (see "Keys") need no
hook: Building Planner's client tick reads them (`CopyKeys`), with no screen open, while the copy is at the aim, the
one ghost block is active, or Delete is on, and empties their clicks (`consumeClick`) otherwise, so a press made
before then doesn't act once they act.

Litematica, under `src/main/java/fi/dy/masa/litematica/`:

| # | File, method | Hook | Purpose |
|---|---|---|---|
| L1 | `util/SchematicUtils.java`: `setTargetedSchematicBlockState(BlockPos, BlockState)`, `setSchematicBlockStates`, `setAllStatesToAirExcept(SchematicPlacementManager, PlacementPart, BlockState, Level)`, `replaceAllIdenticalBlocks` | `@WrapWithCondition` on each `LitematicaBlockStateContainer.set(int, int, int, BlockState)` (the only four writes the ten Edit Schematic actions make, all called from `event/InputHandler.java`) | Skip a write into a tile's container, so Litematica's Edit Schematic actions never change the layer, even on a computer whose Litematica tool mode is still "Edit Schematic" (`InputHandler.handleUseKey` and `handleAttackKey` edit in that mode whatever the player holds). Writes to other placements, such as the copy, stay Litematica's own. |
| L4 | `schematic/placement/SchematicPlacementManager.java`, `setSelectedSchematicPlacement(SchematicPlacement)` | `@Inject` at head, cancellable | A tile or the copy is never selected: selecting one leaves the selection as it was, since selecting none would also clear Litematica's material list. Covers `addSchematicPlacement`, which selects a new placement when none is selected, `removeSchematicPlacement`, which then selects the first placement, selection by aim, and the placement list. Tiles and the copy are known as theirs before they're added. |
| L5 | `gui/widgets/WidgetListSchematicPlacements.java`, `getAllEntries()` | `@ModifyReturnValue` | Leave tiles and the copy out of Litematica's placement list. |
| L6 | `render/OverlayRenderer.java`, `updatePlacementCache()` | `@WrapOperation` on `SchematicPlacementManager.getAllSchematicsPlacements()` | Leave tiles, the copy and its zoop row out of the boxes and origin markers `renderBoxes` draws; Building Planner draws the copy's box itself, in its colours (see "Copy and paste"). This leaves Litematica's own switch for those boxes (`enablePlacementBoxesRendering`) as the player set it. |
| L7 | `render/schematic/ChunkRendererSchematicVbo.java`, `getOverlayType(BlockState, BlockState)` | `@ModifyReturnValue` | While Building Planner has a session, turn `EXTRA` into `NONE`. Litematica marks a real block as extra wherever a placement's box holds air there (`schematicOverlayTypeExtra`, on by default), and a tile's box is a whole chunk column's sections, so every real block in it would be marked. The layer holds only blocks to place, so no real block is extra. Missing and wrong blocks keep their colours. |
| L8 | `schematic/placement/PlacementManagerTaskRebuild.java`, `run()`; `PlacementManagerTask.java`'s constructors | `@WrapMethod`; a number for each task, in the order they're made (`RebuildOrder`) | Run the rebuilds of one chunk column one at a time. Litematica's background threads (at least two) rebuild a column from its placements' blocks as they are when the rebuild runs, so two rebuilds of one column at once could end with the older one's result, read before an edit's blocks were written. It also notes when each rebuild begins and ends, for the self-check ("Overlapping shapes"). |
| L9 | `schematic/placement/PlacementManagerDaemonHandler.java`, `removeRebuildTasksFor` and `removeDeferredTasksFor` | `@WrapMethod` | A running rebuild drops from the queue only the rebuilds of its column made before it began, whose edits it reads. Litematica's rebuild drops every queued rebuild of its column as it ends, and as it unloads a column with no placements, including one queued for an edit written after it read the column, which would leave that edit undrawn. Elsewhere (a new rebuild replacing the queued ones, an unload) every queued rebuild of the column still goes. |
| L10 | `gui/GuiMaterialList.java`, private `createButtons()` and `createButton(int, int, ButtonListener.Type)` | `@WrapOperation` on `addButton` in `createButton`, which adds "Save list" or "Saved lists" in place of the `EXPORT` or `EXPORT_TYPE` button, telling them by the listener's `type` (read by reflection, since `ButtonListener.Type` is private); `@WrapOperation` on `addButton` in `createButtons`, the call that adds the `ButtonListenerChangeMenu` | For Building Planner's list only (`GhostMaterialList`): no "Export" button, no export type button ("Write to file", which cycles), and no "Litematica menu" button, which `createButtons` adds at the bottom right; "Save list" and "Saved lists" go where "Export" and the export type were, in the same row, which the method moves to the bottom left when the screen is narrow (`isNarrow`). Other material lists keep their buttons. |
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
  "The lists on the server"); and `place_turn(item, properties)`, the turn of the real block a right click places in
  Disable (see "Turning real blocks in Disable").
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

Building Planner has three modes of its own that take over the mouse (`MouseMode`): drawing a box, for Copy, Delete or
the material list; the copy at the aim; and Delete by aim (`DELETE_AIM`, see "Delete by aim"). At most one is on at a
time. None changes Effortless Building's mode, so nothing needs putting back when one ends: while one is on, hook E14
keeps Effortless Building's clicks out and E15 its longer aim, E22 hides its preview, E19 shows no mode selected in the
radial menu, and hooks M1 to M5 take the clicks and the wheel. The one ghost block's controls (see "One ghost block")
aren't a mouse mode: they act only while no mouse mode is on. A mode ends when the player:

- right-clicks (M2), in Delete by aim only once no ghost block is marked;
- chooses a mode in the radial menu (E21), even the one they had;
- shows or hides ghost blocks;
- presses the radial menu's Copy, Delete, "Material list", Paste or Block mix button, or the block mix key, each of
  which then does its own action (Copy, Delete and "Material list" start or switch a box, Paste brings the copy back;
  in Delete by aim, Delete ends it, and Copy and "Material list" start a box for themselves);
- leaves the server or changes dimension.

Undo, redo and the speed sliders keep the mode, so a player can undo a paste and paste again, and so do the copy's own
keys (see "Keys"). R switches a Delete box and Delete by aim into each other (see "Delete by aim").

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

A box for Delete has the same line as Copy's, starting "Delete:", with "R for Aim" before the right click (see
"Delete by aim"). The copy at the aim's line names its mode and its
keys, as the player has them bound. Geo approved these lines as written (inbox request 972, revisions 1 and 2).

The game's texts use the fewest words that stay clear, and one name for each action, as in Geo's edits: "Too many blocks", the panel's column headings "All", "Hold" and "Need", and "Arrow keys + Page Up/Down to nudge", where "nudge" names what all six keys do.

- Single, following the aim: "Left-click to paste, Scroll to turn, Middle-click to mirror, H to lock, R for Zoop,
  Right-click to close";
- locked: "Locked: Left-click to paste, Arrow keys + Page Up/Down to nudge, Scroll to turn, Middle-click to mirror, H to
  unlock, R for Zoop, Right-click to close", since R still changes a locked copy's mode;
- Zoop before its first click: "Zoop: Left-click to set the start, Scroll to turn, Middle-click to mirror, H to lock,
  R for Vertical, Right-click to close", and Vertical the same, starting "Vertical:" and with "R for Single"; locked,
  it starts "Zoop, locked:" and goes on as the locked line, with "Left-click to set the start" and "R for Vertical";
- Zoop or Vertical after its first click: "Zoop: Left-click to paste 4 copies, Right-click to cancel", with the row's
  count ("1 copy" for one), locked or not; R does nothing with the row fixed, so it has no "R for";
- after the player's first paste in the dimension, "Hold Left Control to line up" goes after "Middle-click to mirror"
  in the lines of a copy that follows the aim. A locked copy doesn't line up, so no locked line has it;
- while the copy is red, "Left-click to paste" (or the row's paste) gives way to the reason, in red: "Can't paste: part
  of the copy is outside the world" (the build height or the border) or "Can't paste: too many blocks". In Zoop and
  Vertical before the first click the reason goes in the same place, before "Left-click to set the start", which stays
  since that click still fixes the row: "Zoop: Can't paste: too many blocks, Left-click to set the start, Scroll to
  turn, ...".

Each key goes by its name as Controls shows it ("H", "Page Up", "Left Control"). The six nudge keys make one phrase,
"<keys> to nudge": the four along the ground show as "Arrow keys" while they're on Up, Down, Left and Right in that
order, and otherwise by their names one by one, forward, back, left and right ("I/Down Arrow/Left Arrow/Right Arrow");
the two up and down show as one name and the second name, which drops the first word the two names share ("Page
Up/Down", "Keypad 8/2"), or both names in full when they share none ("U/J"). The two parts are joined with " + "
("Arrow keys + Page Up/Down to nudge", "I/J/U/O + Y/N to nudge"). A key with no key bound leaves its part out, and the
phrase goes when all six are unbound. The clicks and the wheel keep Effortless Building's words. The line is a pure function of the copy's state and the keys'
names (`ControlsLine`), as a box's lines are.

A line too wide for the screen wraps onto a second line above it, breaking only at spaces outside the box's size, which
goes after the last word that leaves it room, or alone on the line below. Effortless Building's own line during its
shapes stays as it is.

Each wrapped line sits on a dark see-through background, black at 0.5 opacity, the game's chat background at its default
opacity, as wide as that line's text plus 2 pixels at each side and one line high, so white words stay readable over
light ghost blocks (white wool, glowstone, snow). `ModeHud` fills it with `GuiGraphicsExtractor.fill` before drawing the
text. The game's "Text Background" accessibility setting ("Chat" or "Everywhere") doesn't change it: the setting works
only through `Options.getBackgroundColor`, which the game's own text (the action bar, subtitles, the player list) calls
and the controls line doesn't, so the background is always there at that opacity.

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
counting the edit's positions, written with digit grouping from 1,000 ("Erased 16,001 ghost blocks."), or, for a box without ghost blocks, "There are no ghost blocks in that box to erase."
and nothing changes. Geo approved these texts and the button's name (inbox request 972).

R ("Change Zoop or Delete mode", see "Keys") switches a Delete box to Delete by aim at any of its clicks, and drops
the corners set so far (see "Delete by aim").

Effortless Building's shapes drawn with the break click keep Effortless Building's own limits (in survival, reach 32,
64 a side and 2,000 positions a shape, counted whether or not they hold ghost blocks), and erase only the ghost blocks
in the shape: hook E2 sends air only for the positions that hold a ghost block in the game's copy of the layer, and the
other halves of two-block ghost blocks, rather than every position of the shape. Raising Effortless Building's own
limits to the box's instead would freeze the game for a large shape: it lists and sorts every position of the shape
each frame while it follows the aim (`BuildPipelineClient.getPreviewBlocks`, `BlockSet.sortByDistance`), 16.7 million
for a cube 256 across (inferred from the code, not measured).

### Delete by aim

Delete has a second mode, Aim, like Satisfactory's dismantle: the ghost blocks the player looks at are marked, and a
confirm erases them (Geo's note on 972, and inbox request 981). Delete's two modes are Box, the Delete box above, and
Aim (`DeleteMode`). R switches between them, as it switches the copy's modes (981 Q1 A). Delete starts in the mode the
player last used while the game runs, and in Box at each launch, as the copy's mode starts in Single. Aim is the
mouse mode `DELETE_AIM`, so the radial menu's Delete button ends it, and Copy and "Material list" start their own box,
with no corners set; while it's on, the Delete button draws in the menu's blue for a selected option, as for a Delete
box. R switches Aim back to a Delete box with no corners, and keeps the marks, which show again when R comes back to
Aim. Starting Aim, or switching to it, while ghost blocks are hidden shows them first, as a Delete box does.

**Marking.** While the player holds Minecraft's sprint key (Left Ctrl unless they change it), each ghost block the
crosshair passes over is marked (981 Q2 A, Q3 A). Each frame, Building Planner aims as hook E9 aims (`GhostAim`, ghost
blocks only, up to 256 blocks from the eyes, as a box's corner): a real block in front of a ghost block stops the aim,
and only ghost blocks are marked. So that a quick sweep leaves no gap, it also aims along the look directions between
the last frame's and this frame's, from eye positions between the two, in as many steps as keep the aimed points at
most half a block apart at the farther of the two frames' aims, up to 64 steps a frame (`DeleteMarks.swept`, run by
`DeleteAim` once a frame, after the world is drawn). The sweep decides at its first ghost block whether it
marks or unmarks: a sweep whose first ghost block is marked unmarks each ghost block it passes over, until the player
lets go of the key (981 Q4 A), as Satisfactory's mass dismantle deselects. The sprint key still sprints, so a player
who walks forward while marking also sprints, and in Freecam the camera flies faster, as when lining up the copy.

Only the ghost block is marked, not the other half of a two-block ghost block (a door, a bed), which the erase takes
with it (`GhostErase.of`). Up to 200,000 ghost blocks may be marked, the server's limit for one edit, as for the Delete
box (981 Q5 A), counting the other halves the erase would take; once the marks reach it, a sweep marks nothing more,
and the controls line shows "Too many blocks" above its controls, as a box past the cap does. A mark drops when its
ghost block leaves the player's copy of the layer, for any reason: another player's edit, the player's own undo, a
real block filling it, or its column unloading (`DeleteMarks.retain`, run when the layer's version changes). Leaving
the server, changing dimension, and anything else that ends Delete clear the marks.

**Look.** Each marked ghost block has a red outline and see-through red sides (981 Q6 A), in the red of the copy's box
(#FF7070, sides at 0.2 alpha), drawn through MaLiLib's renderer as the copy's box is (`MarksOutline`). Only the faces
that don't touch another marked block are drawn, and only the edges where the shape turns, so a marked wall shows as
one red shape (1060 Q1 A). The shape is built in sections of 16 blocks a side, as the game builds its terrain, and a section is
built again only when a mark in or beside it changes (`DeleteMarks.takeChanged`), so a frame of a sweep builds only the
few sections it touches; each frame draws each section where it is from the camera. The marked ghost blocks show
under the crosshair as the Delete box's do (`BoxList`): each kind of block with its count, most numerous first.

**Erasing.** Holding the left button for one second (20 ticks) erases the marked ghost blocks (981 Q2 A), as holding
the left button for a second dismantles in Satisfactory: M1 starts the count when the button goes down, the client
tick counts each tick it stays down, and letting go before the 20th tick erases nothing and starts the count again at
the next press (`EraseHold`). Holding on past the erase erases nothing more until the button is let go and pressed
again. With no marks, the left button does nothing, and a hold whose marks all go before its 20th tick (a right click,
another player's edit) erases nothing. The erase sends one erase edit (`GhostErase.of` over the marks), so one undo
brings them all back, other players see them glow as they go, and the player sees the Delete box's message ("Erased
120 ghost blocks.", "Erased 1 ghost block."). The marks go as the edit is sent, and Aim stays on to mark more (981
Q7 A); an undo brings the ghost blocks back unmarked. A right click clears the marks; with no marks, it ends Aim.

**Controls line.** Aim's line shows as the other mouse modes' lines do (see "Building Planner's mouse modes"), with
the keys as the player has them bound, and names only the controls that work at that moment (`ControlsLine.deleteAim`);
a key with no key bound is left out, as in the copy's lines. These texts are proposed, for Geo's review with the
build's screenshots:

- a Delete box's lines name R before the right click: "Delete: Left-click to set the first corner, R for Aim,
  Right-click to cancel", then "the opposite corner" and "the height", as Copy's box goes on;
- Aim with no marks: "Delete, Aim: Hold Left Control to mark, R for Box, Right-click to close";
- Aim with marks: "Delete, Aim: Hold Left-click to erase, Hold Left Control to mark, R for Box, Right-click to clear";
- during a sweep that unmarks, "Hold Left Control to unmark" in place of "Hold Left Control to mark";
- while the left button is held to erase, its progress in place of "Hold Left-click to erase", as an action: "Erasing
  60%", in steps of 5%, from "Erasing 0%" at the press to "Erasing 95%" at the 19th tick;
- once the marks reach the cap, the warning line "Too many blocks" above the controls, as for a box.

Satisfactory's dismantle, from the official wiki's [Build Gun](https://satisfactory.wiki.gg/wiki/Build_Gun) and
[Controls](https://satisfactory.wiki.gg/wiki/Controls) pages: holding the left button for one second dismantles the
building aimed at; holding Left Ctrl marks up to 50 buildings at once, and letting go of Ctrl and pressing it again
while aiming at a marked building deselects instead. A player's post in the Steam discussion
["HOW DO I REMOVE BUILDINGS"](https://steamcommunity.com/app/526870/discussions/0/2577697615749987269/) (reply #5,
July 2nd, 2020) describes moving the mouse with Ctrl held marking everything the cursor touches. Satisfactory marks
whole buildings, such as an 8-metre foundation, where Aim marks single blocks, so Aim's limit is the server's, not 50.
Satisfactory's Dismantle Filter (G) and its blueprint mode have no equivalent here.

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
only while the copy is at the aim (see "Copy and paste"), the one ghost block is active (see "One ghost block"), or,
for R alone, Delete is on (see "Delete by aim"): "Lock or unlock" (H), "Nudge forward" (Up), "Nudge back" (Down),
"Nudge left" (Left), "Nudge right" (Right), "Nudge up" (Page Up), "Nudge down" (Page Down) and "Change Zoop or Delete
mode" (R), Satisfactory's keys for the same controls. These names are proposed, for Geo's review with the build's
screenshots, since the keys act for more than the copy; their ids stay `key.buildingplanner.lock_copy`, the
`nudge_*` ids and `key.buildingplanner.copy_mode`. Lining the copy up, turning and flipping the block in hand, and
marking in Delete by aim use Minecraft's sprint key (Left Ctrl), as the copy's clicks and wheel use Minecraft's own
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
sorts the keys of one heading by their names (`KeyMapping.compareTo`), so the heading lists "Block mix", "Change Zoop
or Delete mode", "Lock or unlock", the six "Nudge" keys (back, down, forward, left, right and up), "Open Modifier
Settings", "Open Radial Menu", "Redo", "Show or hide ghost blocks" and "Undo".

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
sits there (`CopyPlacement`). A turn or a mirror keeps the copy's middle block where it was, and the copy then rests
against the aimed face again, unless it's locked (below), when it spins in place.

**Snapping.** Every position is on Minecraft's block grid, so the copy always lines up with blocks, as Satisfactory's
buildings line up with the grid on a foundation. Beyond the grid, the copy snaps to the player's own pastes, as a
Satisfactory foundation snaps edge to edge to the foundation it's aimed at (806 Q3 C): while the aimed block lies in the box of
one of the player's 16 most recent pastes in this dimension (`PasteBoxes`, whatever has happened to their blocks since;
each copy of a Zoop or Vertical row counts as a paste of its own),
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
- R ("Change Zoop or Delete mode") switches the copy between Single, Zoop and Vertical, in that order, as R cycles
  Satisfactory's build modes (806 Q6 A; below).
- A right click (M2), or anything else that ends the mode, puts the copy away: Building Planner removes its placement
  from Litematica and keeps the schematic, with its turn and mirror, as the most recent copy. During a zoop, after the
  first click, a right click drops the row and leaves the copy at the aim, as a right click in Satisfactory cancels the
  step it's on.

**Build modes and zoop.** The copy's mode starts as Single each time the game starts and stays as the player last set
it while the game runs, as Satisfactory keeps a build mode until R changes it. Satisfactory gives foundations these
three modes; its wheel, shown while R is held, has no equivalent here, since tapping R reaches each of the three.

- Single: each left click pastes one copy, as above.
- Zoop: the first left click fixes the first copy where it is, and sends nothing. The row then follows the aim: copies
  end to end from the first, each the copy's own length (as it's turned) from the one before, so they touch without
  overlapping, along whichever of the four compass directions from the first copy the aim is farthest along, up to 10
  copies in all, as Satisfactory zoops up to 10. The aim here is where the copy would rest against the aimed face; the
  row ends with the copy that holds it along the row, and a tie between directions takes east or west. While the
  player aims at nothing, the row stays as it was. The second left click pastes the whole row as one shape edit, which
  one undo takes back, and the copy follows the aim again for a new row, unless it's locked, when it stays where the
  row started. Each copy's box joins the recent pastes the copy snaps to, as a paste of its own, in the order the
  copies lie in the row from the first (961 Q1 A): aimed beside the third copy of a row, the copy sits flush beside
  that copy, and a row of 10 counts as 10 of the 16 pastes. Left Ctrl's line stays with the most recent paste, the
  row's last copy.
- Vertical: as Zoop, with the row going up or down from the first copy, whichever way the view is: the row reaches the
  height where the view's line passes nearest the vertical line through the first copy's middle, within 64 blocks, so
  it follows the view up and down with no block to aim at (`CopyRow`).

The row's other copies are placements of the same schematic, which Litematica draws as it draws the copy and Building
Planner adds and removes as the row grows and shrinks (hooks L4, L5 and L6 treat them as the copy). While the row is
fixed, the wheel and the middle click turn and mirror every copy in it (the first in place, around its middle, and the
others laid end to end again at the turned length), H, R and the nudge keys act on nothing, and a right click drops
the row. A copy locked before the first click starts the row where it's locked. A row whose blocks together pass the
block cap draws red (below), and its click pastes nothing.

**Colours.** The copy's box is drawn by Building Planner through MaLiLib's renderer, as the box for Copy is
(`RenderUtils.renderAreaOutline` and `renderAreaSides`, an outline and see-through sides in the box's colour, as
transparent as that box's: 0.15 alpha, and 0.2 where it is red; `CopyOutline`), in Satisfactory's hologram colours (806
Q7 B): blue (#00D4FF) where a paste would go in as it shows; yellow (#FFE100) where some of its blocks would replace ghost
blocks already there, which the paste still does, as Satisfactory's yellow "soft clearance" still builds; and red
(#FF7070) where the server would refuse the paste: part of the copy outside the world's build height or its border, or
a zoop row over the block cap. While it's red, the controls line says why (a Zoop or Vertical copy's too, before its first click) and a left click pastes nothing and sends no
edit. Hook L6 leaves the copy and its row out of Litematica's own placement boxes, as it does tiles. The copy's blocks
draw as Litematica draws them.

The colour is a pure function (`CopyColour`), worked out each frame and at each click. With a zoop's row fixed, each
copy's box is drawn, and the whole row has one colour, since it pastes as one edit. Red follows the server's checks in
its order: the row's blocks over the cap, then the build height, then the border. The copy's box is the smallest box
around its blocks, so the row's two corners stand for its blocks in the height and border checks. Yellow reads the
player's copy of the layer whether ghost blocks show or not, since a paste shows them again and replaces them. A ghost
block the same as the copy's block there doesn't count, since the paste leaves it as it is. Whether the copy is over
ghost blocks is kept until the row's boxes, the copy, its turn or the layer change. A zoop's first click, which pastes
nothing, still fixes the row while the copy is red; the second click is the one that pastes nothing.

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
| Dismantle, mass dismantle, Dismantle Filter, Customizer | F, Ctrl, G, X | None for the copy: the break click erases with Effortless Building's shapes, and Delete by aim marks with Left Ctrl and erases with a held left button, as mass dismantle does (see "Delete by aim"). |
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

### One ghost block

In Effortless Building's Single mode, the copy's controls work for the one ghost block a click places, as if it were
a copy of one block (Geo's note on 972, and inbox request 980). Single's buttons stay (980 Q1 A): a right click places
and a left click erases. Two of the copy's controls are Single's own already for one block: the copy against the aimed
face is, for one block, the block beside that face (`CopyPlacement.againstFace` with a 1×1×1 box), which is where
Single places; and a one-block copy flush against a paste's box lands where aiming at that face puts it. So neither
adds anything here, and a ghost block's row doesn't join the recent pastes the copy snaps to. Left Ctrl doesn't line
the block up (980 Q6): it turns and flips the block instead (below), in every one of Effortless Building's modes
(1009 Q2 C), and in Disable the real block a right click places while Left Ctrl is held (1029 Q1 A, see "Turning real
blocks in Disable").

The block's controls are active while Building Planner has a session, Effortless Building's mode is Single, no mouse
mode is on, and the main hand holds an item that places a block (`ClickRule`, as for shapes). The turning controls
(Left Ctrl with the wheel and the middle click) are active under the same conditions in every mode, while no shape
drawn with the break click is under way (`getBuildState()` isn't `BREAKING`), since that shape erases
(`SingleRule.turning`); in Disable they act on the real block a right click places. H, the nudge keys, R's Zoop and Vertical, the coloured box and Single's controls line act in
Single mode only. A client class beside the copy's, `SingleBlock`, stores the Single controls' state: the block's own
mode, Single, Zoop or Vertical (`CopyMode`), apart from the copy's (980 Q3 A); the locked position, with the face and
hit point aimed at when it was locked; and a fixed row's start. A second client class, `HeldTurn`, stores the turn and
the item it was set for, since the turn reaches every mode. The mode starts as Single at each launch and stays as the
player last set it while the game runs, as the copy's does. Leaving the server, changing dimension, choosing a mode in
the radial menu (E21) and any mouse mode starting unlock the block and drop its row. The block at the aim has Single's reach, Effortless
Building's own (32 in survival, 200 in creative).

**Locking and nudging.** H ("Lock or unlock") locks the block where a right click would place it at that moment (980
Q4 A): beside the aimed face, at the position Effortless Building's preview draws its first block
(`BuildPipelineClient.resolveFirstClickPos`). With nothing aimed at, H does nothing. Locked, the block stays where it
is while the player walks or flies around it, and the arrow keys, Page Up and Page Down nudge it a block at a time, as
they nudge the copy: along the compass direction nearest the view (the Freecam camera's while Freecam flies it), with
the copy's repeat for a held key (`CopyNudge`, `KeyRepeat`) and no distance limit. A right click places one ghost
block at the locked position, with Effortless Building's mirror, radial mirror and array, and the block stays locked
for the next. A left click erases every ghost block a right click would place, whatever the player aims at (1009 Q1
A, 1074 Q1 A), and the block stays locked. Holding either
button acts once, since repeats would land on the same position until it's nudged. H again unlocks it, and it follows
the aim again. A locked block's state comes from its placement rules with the face and hit point aimed at when it was
locked and the facing of the player (or the camera) at each click, then the turn (below).

**Zoop and Vertical.** R ("Change Zoop or Delete mode") switches the block between Single, Zoop and Vertical, in
that order (980 Q2). In Zoop and Vertical the first right click fixes the row's start where the block is (where it's
locked, if it is) and sends nothing. The row then follows the aim as the copy's does (`CopyRow` with a 1×1×1 spot and
the row's longest length): along whichever of the four compass directions from the start the aim (where a right click
would place) is farthest along, a tie taking east or west, up to 32 blocks in all (Geo's answer to 980 Q2); in
Vertical, up or down from the start, to the height where the view's line passes nearest the vertical line through the
start, up to 32 blocks, so it rises into open air with no block to aim at. While the player aims at nothing, the row
stays as it was. The second right click places the whole row as one shape edit, which one undo takes back. A left
click with the row fixed drops it and erases nothing. While the row is fixed, H, R and the nudge keys act on nothing,
and turning and flipping (below) act on every block in the row. After the row is placed, the block follows the aim
again for a new row, or stays locked where the row started. Before the first click, the left click erases as in
Single: at the aim, or while the block is locked, every ghost block a right click in Single would place.

Each block in the row faces the same way, as in a Line, and the row goes through the same steps as Effortless
Building's shapes: Building Planner makes a `BlockSet` of the row's positions, with its first and last positions, and
runs it through Effortless Building's modifiers, the block mix and its constraints
(`BuildPipelineClient.CLIENT.processBlocks`), so Effortless Building's mirror, radial mirror and array apply to the
row as to a Line. Each entry then takes its state as E1 makes it (`EffortlessShapes.place`, which takes the hit as an
argument for this): the placement rules with the start's face and hit point, the facing at the second click, then the
turn. Hook E27 hands the same processed set to Effortless Building's preview, which draws it with the mix and the
modifiers. A locked block that isn't in a row goes through the same steps, as a row of one; the preview skips a set
of one block while no shape is under way, so hook E30 lets it draw the locked block and a row of one, and hook E28
gives it the state a right click places.

The block's clicks while it's locked or in Zoop or Vertical are Building Planner's: hook E14 takes them from Effortless
Building, M2 takes the right click, M1 the left and M3 holding the left (see "Hooks"). Hold to repeat therefore runs
only while the block follows the aim in Single.

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
the hit the locked block or its row was fixed with, the shape's first click, or the aim, as the preview shows it where
it draws the block (see "Colours" for Single's block at the aim). From
then on, the block keeps that orientation whichever way the player faces and whichever face they aim at, until they
switch to another block (980 revision 2, Q1 A): the main hand's item becoming another item, by the hotbar or by picking
a block, drops the turn, and so does switching to another block and back; the same block in another hotbar slot
keeps it. The turn is kept as the values of the block's orientation properties (facing, axis, rotation, half, slab
type, attached face, hanging), and `BlockTurn.apply` sets each on every block placed that has that property, after
the placement rules, which still set the rest (waterlogging, a fence's connections). With the block mix on, each of
the mix's blocks takes the properties it has.

Every mode's shapes and their preview take the turn (1009 Q2 C): Single's one block and its rows, and the shapes of
Line, Wall, Floor, Cube, Diagonal Line, Diagonal Wall, Slope Floor, Circle, Cylinder and Sphere, the modes Effortless
Building 4.3 offers besides Disable and Single (`buildmode/BuildModeEnum.java`, where Pyramid, Cone and Dome are
commented out). Each of these modes gives every block of its shape the held block's placement rules with the first
click's face (`BuildPipelineClient.getFirstClickHit`), then each entry's mirror and turn from the modifiers
(`BlockEntry.applyTransforms`), in the preview (`BlockPreviewRenderer.render`) as in E1's states
(`EffortlessShapes.place`). The turn goes between the two: E1 applies `BlockTurn.apply` to each state after its
placement rules and before the entry's transforms, and E28 to the state the preview starts from, so a mirrored copy
of a turned stair faces the mirrored way, as the preview shows it. The turn stays as the player changes mode, Disable
among them, until they switch to another block (980 revision 2, Q1 A). The player may turn the block before a shape's
first click or between its clicks; the preview shows each turn at once (before the first click it draws no set of one
block without the mirror, radial mirror or array, so a turn made then shows from the first click), and the shape places
with the turn it has at its last click. A turn changes a shape's states and never its positions: no mode reads the held block's state (the only
block state read under `buildmode/` is the world's, in `BuildSettings.java`).

In Disable the turning controls act on the real block a right click places, as "Turning real blocks in Disable"
(below) describes.

Left Ctrl takes the wheel only while the block in hand (or a block of the mix) is one a turn changes, and the middle
click only while it's one that flips. With a stone block in hand, the wheel changes the hotbar slot and the middle
click picks a block, Left Ctrl or not; with a chest, which turns but doesn't flip, the middle click with Left Ctrl is
Minecraft's, which in creative mode picks a block with its contents. Effortless Building 4.3 handles the wheel and
Ctrl only in its screens (its jar's classes searched for those calls), so neither clashes with its controls. Litematica's pick block stays out while the middle click flips (hook
L11). The sprint key still sprints, so a player who walks forward while turning the block also sprints.

**Colours.** While the block is locked or its Zoop or Vertical row is fixed, the copy's box shows
around the locked block or each block of the row (980 Q7 A), drawn as the copy's is (`CopyOutline`, with
`CopyColour` over 1×1×1 spots): blue where it would go in as it shows, yellow where it would replace a different ghost
block, and red where part of it is outside the world's build height or its border. A red block or row places nothing
and sends no edit. The colour reads the row's own blocks; copies Effortless Building's modifiers make outside the world
are left out by its constraints, as for its shapes. A row of 32 blocks can't pass the server's block cap, so no row is
red for its size. While the block follows the aim in Single, Effortless Building's preview draws it at the aim, turned
as it will be placed, even as a set of one block, which Effortless Building's own preview skips (E30; 1108 Q1 A); the
box still shows only while the block is locked or a row is fixed.

**Controls line.** While the block's controls are active, with no screen open and the HUD shown, a line of them
shows where the mouse modes' lines show, in the same look and with the same background (`ModeHud`,
`ControlsLine.single`), with the keys as the player has them bound (980 Q8 A). It names only the controls that work at
that moment. These texts are proposed, for Geo's review with the build's screenshots:

- following the aim: "Right-click to place, Left-click to erase, Hold Left Control to turn or flip, H to lock, R for
  Zoop";
- while Left Control is held, "Scroll to turn, Middle-click to flip" in place of "Hold Left Control to turn or flip";
  for a block that only turns, "Hold Left Control to turn" and "Scroll to turn"; for one that only flips, "Hold Left
  Control to flip" and "Middle-click to flip"; for one that does neither, no part;
- locked: "Locked: Right-click to place, Arrow keys + Page Up/Down to nudge, Left-click to erase, Hold Left Control to
  turn or flip, H to unlock, R for Zoop";
- Zoop before its first click: "Zoop: Right-click to set the start, Left-click to erase, Hold Left Control to turn or
  flip, H to lock, R for Vertical", and Vertical the same, starting "Vertical:" and with "R for Single"; locked, it
  starts "Zoop, locked:" and has the nudges and "H to unlock", as the locked line does;
- a row after its first click: "Zoop: Right-click to place 12 blocks, Hold Left Control to turn or flip, Left-click
  to cancel", with the row's count ("place 1 block" for one), locked or not;
- once the block is turned or flipped, ", turned" after the mode and the lock in the line's start, or "Turned:" alone
  in Single: "Turned: Right-click to place, ...", "Zoop, locked, turned: Right-click to set the start, ...";
- while the block or the row is red, the reason in red, "Can't place: outside the world", in place of "Right-click to
  place" or the row's place; in Zoop and Vertical before the first click, before "Right-click to set the start".

In Effortless Building's other modes but Disable (whose line is below), while the turning controls are active and
the block in hand turns or flips, a
line of the turning controls, the turning line, shows in the same place, look and background (`ControlsLine.turning`),
with the sprint key as the player has it bound, in Single's line's phrases for the same actions. It names only the
turning controls, since Effortless Building's clicks in those modes are its own. These texts are proposed, for Geo's
review with the build's screenshots:

- "Hold Left Control to turn or flip", or "Hold Left Control to turn" or "Hold Left Control to flip" as in Single, and
  no line for a block that does neither or while the sprint key has no key;
- while Left Control is held, "Scroll to turn, Middle-click to flip", or the one of them the block has;
- once the block is turned or flipped, "Turned:" before either: "Turned: Hold Left Control to turn or flip".

During a shape drawn with the place click, Effortless Building draws its own line, "Left-click to cancel, Right-click
to place", 58 above the screen's bottom (`RenderHandler.renderSubtitle`), and the shape's block count in the action
bar just above it (`RenderHandler.updateFeedback`, which calls `sendOverlayMessage`). The game draws the action bar's
text from 72 to 63 above the screen's bottom (`Hud.extractOverlayMessage`), so while Effortless Building's line shows,
`ModeHud` draws the turning line above the action bar, its last line ending 2 pixels above it, and both of Effortless
Building's texts stay readable. A shape drawn with the break click has no turning line, since its turning controls
aren't active.

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

Disable's turning controls are active while the turning controls would be in another mode (see the start of "One
ghost block"), with a block in hand that turns or flips:

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
  turn of the block in hand while Left Ctrl is held in Disable, and none otherwise (`DisableTurn`). Messages on one
  connection keep their order, so the server has the turn before it handles the click. It stores the last one for
  each player (`PlaceTurns`) until the player leaves, and accepts only the orientation properties `BlockTurn` sets.
- Hook S2, `@ModifyExpressionValue` on `BlockItem.place`'s call to `getPlacementState(BlockPlaceContext)`, which
  every block item's right click goes through, including the items that override `getPlacementState` (a torch's, a
  sign's, a bed's): when the context's player has a turn for the item in the main hand, S2 sets the turn's properties
  on the state (`BlockTurn.apply`), lets the neighbouring blocks set its shape again
  (`Block.updateFromNeighbourShapes`, so a turned stair joins the stairs beside it as Minecraft's would), and checks
  that the turned block can stay (below). On the server it reads `PlaceTurns`, and on the player's computer the turn
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
| S2 | `net/minecraft/world/item/BlockItem.java`, `place(BlockPlaceContext)` | `@ModifyExpressionValue` on the call to `getPlacementState`, on both sides | Turn the state the click places, set its shape from its neighbours, and return null when the turned block can't stay. |
| E29 | `buildpipeline/BuildPipelineClient.java`, `getPreviewBlocks` | `@ModifyReturnValue` | In Disable, while Disable's preview shows, a set of one block where a right click would place, or null when it wouldn't place. |
| E30 | `render/BlockPreviewRenderer.java`, `render` | `@ModifyExpressionValue` on the first `BlockSet.size()` | Draw a set of one block, which the method skips while no shape is under way: Disable's, the locked one ghost block, and a fixed row of one (see "One ghost block"). |

S2 is the server's second hook, after S1 (see "Ghost blocks the real world fills"), in the mixin config's common
section, since both sides place blocks through `BlockItem.place`. M6 is in the client section, beside M2.

**When the turned block can't stay.** Minecraft's `getPlacementState` checks the state it would place: that it can
stay where it goes and that no entity is in its way (`BlockItem.canPlace`). S2 checks the turned state again the same
way, so a wall torch turned away from its wall or a lantern turned to hang under nothing fails. A bed places its head
toward its facing (`AbstractBedBlock.setPlacedBy`, which checks nothing), and Minecraft checks that space only for the
player's facing (`getStateForPlacement`), so S2 also checks that the space toward the turned facing can take a block
and is inside the world border. A turned block that fails places nothing, and the player keeps the item (1033 Q1 A).
The preview draws it red, as Effortless Building draws a block
the player lacks: tinted red with a red outline (hook E8, which marks that one position missing).

Blocks of two parts turn whole. A door's upper half takes the placed half's state (`DoorBlock.setPlacedBy`), and so
does a small dripleaf's top; a bed's head takes the foot's facing. A tall plant neither turns nor flips, so it places
as in Minecraft.

**The preview.** Effortless Building's preview draws Disable's block, with its see-through block, checkerboard sides
and white outline, as in the other modes. While Left Ctrl is held in Disable with a block that turns or flips, hook
E29 gives the preview one block where a right click would place it, from Minecraft's own aim (`Minecraft.hitResult`,
at Minecraft's reach): the aimed block when it can be replaced, and the space beside the aimed face otherwise
(`BlockPlaceContext.getClickedPos`, after the item's `updatePlacementContext`). Hook E28 gives that block the state S2
would give it (`DisablePreview.state`), the turned state from Minecraft's own `BlockItem.getPlacementState`, so a
torch aimed at a wall shows as a wall torch. E30 lets the preview draw a set of one block; with no shape under way, it
shows no block count and plays no sound (`RenderHandler.updateFeedback`). Nothing shows while the aim isn't a block,
or where the block can't go at all (`BlockPlaceContext.canPlace`). Effortless Building's mirror and array draw no
copies while Disable's preview shows.

**Controls line.** In Disable, while the turning controls are active, a line of them shows in the place, look and
background of the turning line (`ControlsLine.disable`), with the sprint key as the player has it bound, in the
turning line's phrases and Single's line's "Right-click to place". These texts are proposed, for Geo's review with
the build's screenshots:

- without Left Control, the turning line: "Hold Left Control to turn or flip", or "Hold Left Control to turn" or
  "Hold Left Control to flip", with no "Turned:", since a right click then places as in Minecraft (1033 Q2 A); no
  line while the block a right click would place hangs on the block behind it, as Minecraft's rule for that block
  requires (a ladder; a torch, sign, banner, button or lever on a wall; a tripwire hook), though Left Control still
  turns it (1037 Q1 B);
- while Left Control is held, "Right-click to place, Scroll to turn, Middle-click to flip", or with the one of "Scroll
  to turn" and "Middle-click to flip" the block has, and "Turned:" before it once the block is turned or flipped;
- while the turned block can't stay, the reason in red, "Can't place here", in place of "Right-click to place".

**Freecam.** Minecraft aims from Freecam's camera while it's on (`Minecraft.pick` traces from `getCameraEntity`), so
the preview goes where the camera aims. With Freecam's "Allow interaction" off, the click doesn't reach the world, so
no preview shows (`Aim` reads the option by reflection, as it reads "Player control"). With it on, the right click
goes to the server as the player's, and the server places only within the player's reach
(`ServerPlayer.isWithinBlockInteractionRange`), so the preview shows only where the player, not the camera, is within
that reach. The turn sets property values, so the block faces the same way from the camera as from the player.

**Without the session.** On a server without Building Planner, or with another protocol version, Building Planner has
no session, so in Disable Left Ctrl, the wheel and the middle click work as in Minecraft, M6 sends nothing, and S2
changes nothing. The player's computer turns its guess only during a session, so its guess never differs from the
server's block. A player without the add-on places blocks as in Minecraft, and sees other players' turned blocks as
any blocks. `place_turn` is one more payload for the client's check at joining (see "When the server lacks the
mod"). It is part of protocol version 5, which no release has used.

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
Building Planner's boxes, the copy at the aim, the one ghost block's lock and rows, and Delete by aim's marking all aim
from the camera, and each block of a shape faces as it would for a player at the camera facing the same way (a stair
drawn while the camera faces north faces north). The nudge keys move the copy and the locked ghost block by the way the
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
`BlockPlaceContext` for the camera entity), the one ghost block's controls, and Delete by aim's sweep (`DeleteAim`)
read it. Reaches count from the camera: 256 for a box's corner and Delete by aim's marks, 64 for the copy, and Effortless
Building's own for shapes and the one ghost block. Freecam loads no
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
- The menu's corner text reads "Building Planner", and Controls shows Building Planner's keys, the copy's eight among
  them, and Effortless Building's under the "Building Planner" heading, sorted by name as Controls sorts them, with no
  raw key name;
  each is shot, Controls at 854 by 480 and 1920 by 1080, GUI scale 2, and at 1920 by 1080, GUI scale 4. The driver's
  own Page Up key, under a heading of its own, is unbound first, so Controls shows no clash.
- The Copy button starts a box: the controls line shows "Copy: Left-click to set the first corner, Right-click to
  cancel", a right click and a use of the place click places no block, and Effortless Building's preview and mode are
  as they were. Aiming at a block's top face, the box's first corner is the space above it; aiming at its side, the
  block itself. The box follows the aim between clicks and the list under the crosshair counts its blocks. The third
  click makes the copy, with its message, places no real block, doesn't reach the other player, and leaves
  Effortless Building's mode as it was. The copy rests on the aimed top face, the middle of its bottom on the block
  above, and follows the aim, and Litematica neither selects nor lists it. Against a real wall's side face, it stands
  beside the wall, the middle of the side that touches it on the block beside the face; under a floating block's bottom
  face, it hangs below it. On the floor and against the wall it's shot at 854 by 480 and 1920 by 1080, GUI scale 2.
- At 854 by 480, GUI scale 2, the list counts a small box's blocks, most numerous first, and the material list box's
  controls line wraps onto two lines with the box's size whole. A box of 144 by 3 by 144 (as wide as the chunks the dev
  server's view distance of 6 loads around the player allows) over a floor of 40 kinds of block shows the list in rows
  of 10 that end inside the screen and above the controls line, with a "+11" cell; it's shot. A break shape drawn in
  Line mode over real grass has an empty break list (E8). A box past the side limit, or over a chunk not loaded, draws
  red, says which limit, and its click sets no corner.
- With the copy at the aim, the controls line shows its controls. A notch up turns it a quarter turn clockwise in place,
  the middle of its bottom staying on the same block, a notch down back, and the hotbar slot stays; a middle click,
  facing south with the crosshair on the copy's blocks, flips it east to west within the same box, with its stairs
  turning with it, and picks no block, neither the game's nor Litematica's.
- Over other ghost blocks, the copy's blocks show where they overlap, and the ghost blocks show in its empty space.
- A left click pastes it: the other player sees its blocks at once, turned and mirrored as the copy showed them,
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
  again, a left click pastes it, for the other player too, and it stays locked where it was. With Freecam's camera on
  and facing east while Planner1 faces north, Up moves it east, away from the camera.
- The copy's mode starts as Single, and R switches it to Zoop, Vertical, Single and Zoop again. In Zoop the first left
  click fixes the copy as the row's start and sends no edit. Aimed 9 blocks north, the row has 4 copies end to end,
  each the copy's length north to south from the one before, each one of Litematica's placements, which Litematica
  draws; the first stays where the click fixed it. It's shot at 854 by 480 and 1920 by 1080. Aimed farther, the row
  stops at 10 copies; aimed back, it shrinks to 4. A notch of the wheel up turns every copy a quarter turn, the row
  laid again at the turned length (6 copies), and a notch down turns them back. H and the nudge keys act on nothing
  while the row is fixed. A right click drops the row, sending no edit, and leaves the copy following the aim, still in
  Zoop. A row of 4 pastes as one edit, which the other player sees, every copy's blocks; the copy is then at the aim
  with no row, and aimed at the east face of the row's third copy it sits flush beside that copy, not beside the row's
  end. One Ctrl+Z takes back the whole row. In Vertical, looking up past the copy, the row goes up 5
  copies, each the copy's height above the one before, and is shot at both sizes; looking down, it goes down 3, to the
  bottom of the world.
- With Zoop set, a new copy made with the Copy button keeps the mode, and its first click fixes a row. After Planner1's
  game restarts, the mode is Single again.
- The copy's colours (the driver's `copycolour`): on the grass it's blue, and Litematica draws no box for it or for a
  zoop row's copies (hook L6, the driver's `boxes`), which are blue too; over a ghost gold block its stone bricks would
  replace, it's yellow, and its click still pastes. Locked and nudged down to the world's bottom it's blue, and one
  block lower, partly below the build height, it's red; then, back on the grass, with a world border whose edge
  crosses it, it's red again. Each red click pastes nothing, sends no edit and adds no chat line, and the copy stays
  locked where it was; with the border back, it's blue. A copy of 102,400 ghost stone blocks is blue, and a Vertical
  row of two of them, 204,800 blocks, is red; its click sends no edit and leaves the row, and aimed back to one copy
  the row is blue. The copy is shot blue on the grass, yellow over the ghost blocks, red at the border, and the red row
  over the cap, each at 854 by 480 and 1920 by 1080, GUI scale 2.
- The controls lines, each checked word for word (the driver's `modehud`): Single before the first paste, and with
  "Hold Left Control to line up" after it; locked after a paste, with the nudge keys, "R for Zoop" and no "line up";
  locked and red below the build height and at the border, with the reason in place of the paste, and the paste back
  once it's blue; R on that locked red copy, which makes it a red Zoop before its first click, with the reason before
  "Left-click to set the start", and R twice more back to Single; Zoop and Vertical before the first click, and Zoop
  locked; a fixed row of 1, 4, 10 and 5 copies; the red row over the cap, with "Can't paste: too many blocks"; and each
  box's line, Delete's among them. With the lock key bound to K, the mode key to G, the nudge forward to I and the
  sprint key to Right Control (the driver's `bindkey`), the line names K, G and Right Control, K locks the copy, and
  the locked line names the four nudges one by one beside "Page Up/Down", K and G; with the keys back, it names H and R again. Each line is
  shot at 1920 by 1080, GUI scale 4, the longest (locked and red) at 854 by 480 and 1920 by 1080, GUI scale 2 too, and
  Single's over a floor of ghost white wool, birch planks and glowstone at GUI scales 2 and 4, with the lines as they
  wrap and their window box in the log (the driver's `modewrap` and `modebox`), the box being the text's, without the
  background's 2 pixels at each side.
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
  says "Erased 16,001 ghost blocks.", sends one edit, takes the door's upper half too and leaves the real stone, and the
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
- Delete by aim, marking (step 12): R in a Delete box after its first click switches to Aim with no corners, and R again
  to a box with no corners (the driver's `mousemode`, `deletemode`); after a Delete in Aim, the Delete button starts
  Aim, and after Planner1's game restarts, a box. The Delete button with ghost blocks hidden, in Aim, shows them. Facing
  a ghost wall of 10 by 5 stone bricks 8 blocks away, with a real stone block in front of the middle row's fourth,
  holding Left Ctrl while the driver turns the view across the middle row from end to end in 3 frames (`turn`) marks the
  row's other 9 blocks and no real block (`marks`), with no gap; the list under the crosshair shows stone bricks and 9.
  A second hold that starts on the third mark and sweeps west unmarks the first three. A ghost door marked by its lower
  half has one mark and two positions to erase. Planner2 erasing a marked block drops its mark. A right click clears the
  marks, a second ends Aim; the Copy button in Aim starts a Copy box. One hold over each row marks the wall's 49 blocks
  the aim meets, and the marked wall is shot at 854 by 480 and 1920 by 1080, GUI scale 2, and with GUI scale 4, beside
  the same view unmarked. What marks cost goes to the log: frames timed (`frametimes`) over 200,000 ghost bricks with
  none marked and with all marked (`markall`), building the shape of those marks and of 200,000 that touch no other,
  whole and after one mark changes (`timeoutline`), and a frame's sweep of 65 aims (`timeaims`).
- Delete by aim, erasing and its line (step 13): with 9 marks and the ghost door marked, a left click held 10 ticks
  erases nothing; held 20 ticks it sends one edit, which takes the door's upper half too and which Planner2 sees glow
  with Planner1's name, and says "Erased 11 ghost blocks."; holding on erases nothing more, though a sweep marks the
  top row meanwhile. Aim stays on with no marks, and one Ctrl+Z brings the blocks back unmarked. The lines, each checked
  word for word (the driver's `modehud`): a Delete box's three lines with "R for Aim", Aim with no marks and with marks,
  an unmarking sweep, "Erasing 50%" at the hold's 10th tick (`await erasing`), and the cap's "Too many blocks" with
  200,000 marks; with the mode key bound to G and the sprint key to Right Control, the lines name G and Right Control.
  Each line is shot at 1920 by 1080, GUI scale 4, the erase's message with it, and the marked wall with Aim's line at
  854 by 480 and 1920 by 1080, GUI scale 2.
- One ghost block, locking and nudging (step 14): in Single mode with stone bricks, H locks the block beside the aimed
  face, and with Planner1 flown 5 blocks away and aiming elsewhere, Effortless Building's preview draws it there. With
  Effortless Building's array of 2 copies one block apart upward, the preview draws all three. Facing north, Up moves it
  one block north and Page Up one up. A right click places one ghost block there, on both clients, and it stays locked;
  holding the right button for a second sends one edit; with Planner1 aiming at another ghost block, a left click erases
  the ghost block at the locked position, leaves the aimed one, and the block stays locked. With the array of 2 copies
  upward, a right click places three ghost blocks and a left click erases all three (1074 Q1 A). Holding the left
  button for a second sends one edit. Locked, an oak stair placed with Planner1 facing east faces east. H unlocks it.
  With a stick in hand, H and the nudge keys do nothing and Single's clicks act as without the add-on; choosing Line
  in the radial menu unlocks the block; with the copy at the aim, H locks the copy and not the block. With Freecam's
  camera on and facing east while Planner1 faces north, Up moves the locked block east. The locked block is shot in
  open air at 854 by 480 and 1920 by 1080, GUI scale 2, and Controls' "Building Planner" heading with the keys'
  proposed names at GUI scale 4.
- One ghost block, Zoop and Vertical (step 15): in Single mode with stone bricks, R switches the block to Zoop,
  Vertical and Single (the driver's `singlerow`), and the copy's mode stays Single. In Zoop, a right click fixes the
  row's start and sends no edit, and the preview draws the start alone; aimed 11 blocks east of the start, the row is
  12 blocks and the preview draws them (the driver's `preview`). While the row is fixed, H, R and the nudge keys act on
  nothing, and aimed at the sky, the row stays as it was. A second right click places the 12 ghost blocks as one edit,
  which Planner2 sees and one Ctrl+Z takes back, and the block follows the aim again, still in Zoop. Aimed 40 blocks
  east of the start, the row stops at 32. A left click aimed at a ghost block with the row fixed drops the row and
  erases nothing. The right button held a second for each of the two clicks places one row. With the block mix of
  stone and andesite on, each block of the row is the mix's for its position, as the preview's is, and both kinds
  show. With Effortless Building's mirror on (the driver's `ebmirror`), the preview draws the row and its mirror image,
  24 blocks, and the click places both. In Zoop, H locks the block and Up nudges it; a row started then starts where
  it's locked, and once placed, the block stays locked there. In Vertical, looking up into open air on the line above
  the start, the row rises 8 blocks and the preview draws them; the second click places them as one edit. A Zoop row
  and a Vertical row are shot before the second click at 854 by 480 and 1920 by 1080, GUI scale 2.
- One ghost block, turning and flipping (step 16): with oak stairs in Single mode, facing north, a notch up with Left
  Ctrl held turns the stair a quarter turn clockwise, to face east (the driver's `heldturn`), and the hotbar slot
  stays; locked, the preview draws the stair facing east (the driver's `previewstate`); the stair placed then faces
  east, and so does one placed after Planner1 turns to face south. The middle click with Left Ctrl held, aimed at a
  ghost block, makes the stair's top half, with no block picked, from the game or Litematica; locked, the preview
  draws it so, and placed, it is so on both clients; unlocked, no preview draws it. A Zoop row of 8 turned stairs
  faces one way, as its preview drew it. With the mix of oak and spruce stairs, a row's stairs of both kinds take the
  turn. The same stairs in a second slot keep the turn; with stone in hand, Left Ctrl and the wheel change the hotbar
  slot, and back on the stairs, the turn is gone. An oak log aimed at a side flips to standing and back. Without Left
  Ctrl, the wheel changes the hotbar slot and the middle click picks the aimed block. The checks log what an unlocked
  left click erases with Effortless Building's array and mirror on. Turned and flipped stairs are shot before placing,
  locked, and after, at 854 by 480 and 1920 by 1080, GUI scale 2.
- Turning in every mode (step 17): with oak stairs turned a quarter turn in Single, to face east, the turn stays after
  choosing Line in the radial menu, and a Line of 5 places 5 stairs facing east, on both clients, as its preview drew
  each of them (the driver's `previewstate`). In Line, a notch up with Left Ctrl held before the first click turns the
  stairs to face south, which the preview draws from the first click, since before it the preview draws no set of one
  block; another notch between the two clicks turns the preview at once, to face west, and the Line places facing
  west. One shape of the turned stairs in each of Wall, Floor, Cube, Diagonal Line, Diagonal Wall, Slope Floor,
  Circle, Cylinder and Sphere has every ghost block in its box facing west (the driver's `boxstates`, with each
  shape's count in the log); a Floor of oak slabs flipped to the top half has every slab's top half. With Effortless
  Building's mirror across x −52, a Line of 4 turned stairs has its mirror image's stairs facing east, in the preview
  and once placed; with the mix of oak and spruce stairs, a Wall's stairs of both kinds all face west. In Disable, Left
  Ctrl with the wheel changes the hotbar slot, to the same stairs in the next slot, and back in Line the stairs keep
  the turn; switching to stone and back to the stairs drops it; and at the end, the middle click with Left Ctrl held
  picks the aimed block. During a Line drawn with the break click, no turning line shows and Left Ctrl with the wheel
  changes the hotbar slot; a right click cancels it, erasing nothing. The turning line, checked word for word (the
  driver's `modehud`): in Line with stairs, with Left Ctrl held, turned, with and without Left Ctrl, and during a
  Line's shape, for a chest (turn only) and a slab (flip only), each with Left Ctrl held too; none for stone, in
  Disable, during a break shape, or with a stick in hand; with the copy at the aim, the copy's line shows; with the
  sprint key on Right Control, it names Right Control; F1 hides it. During a Line after its first click, the turning
  line ends 2 GUI pixels above the action bar's text, which shows the shape's block count (the driver's `modebox` and
  `actionbar`), at 854 by 480, GUI scale 2, and at 1920 by 1080, GUI scale 2 and 4, each shot. The turning line is
  shot in Line before a shape at 1920 by 1080, GUI scale 4, and 854 by 480, GUI scale 2, and a placed Line of turned
  stairs at 854 by 480 and 1920 by 1080, GUI scale 2. Steps 18 and 19 replace this check's Disable parts with their
  own.
- Turning real blocks in Disable (step 18), in creative and in survival, with Planner1 facing north: oak stairs
  turned a quarter turn with Left Ctrl and a notch up, and a right click with Left Ctrl held, place a real stair facing
  east, which the server's world (read with `/execute if block` from the server's console) and Planner2 show facing
  east. The driver reads Planner1's own block at that position every tick from the click on, and it faces east at
  every tick, with no flicker. An oak log flipped to lie east to west and placed on top of a block lies east to west,
  where Minecraft would stand it up. Without Left Ctrl, the stair faces north, as Minecraft places it. The right
  button held with Left Ctrl for a second places a turned stair every 4 ticks. A turned stair placed beside a stair
  joins it as Minecraft's would (the same shape as a stair Minecraft places facing east there). An oak door turned to
  face east has both halves facing east; a red bed turned toward a stone wall places nothing and leaves the wall; a
  torch aimed at a wall and turned away from it places nothing, and the player keeps the torch. A ghost stair facing
  east is filled by the turned real stair. With a turn made in Line, Disable without Left Ctrl places as Minecraft
  does, and with Left Ctrl places turned. Switching to stone and back drops the turn. With the server handing the
  client a `hello` of another protocol version (as `step8.sh` does), Left Ctrl with the wheel changes the hotbar slot,
  and the stair places as Minecraft places it. With Freecam's camera on, "Allow interaction" on and the camera 3
  blocks from Planner1, the turned stair goes where the camera aims and faces east. A turned stair and log are shot
  placed, at 854 by 480 and 1920 by 1080, GUI scale 2.
- Disable's preview and its line (step 19): with Left Ctrl held and turned oak stairs, the preview draws one stair
  where a right click would place it, facing east (the driver's `preview`, with each block's state), and the stair
  placed matches it; aimed at grass, which a block replaces, the preview takes the grass's place. Without Left Ctrl,
  no preview shows. A torch aimed at a wall and turned away from it shows red. Beyond Minecraft's reach, and aimed at
  the sky, no preview shows. With Freecam's camera on and "Allow interaction" off, no preview shows; with it on, it
  shows only while Planner1 is within reach of the aimed block. With Effortless Building's mirror on, the preview
  shows one block. The lines, each checked word for word (the driver's `modehud`): without Left Control, with it held,
  turned, for a chest (turn only) and a slab (flip only), red, and none for stone or with a stick in hand; with the
  sprint key on Right Control, they name Right Control; F1 hides them. The preview of a turned stair and the red
  torch are shot at 854 by 480 and 1920 by 1080, GUI scale 2, and each line at 1920 by 1080, GUI scale 4, with the
  changes outlined in red.
- One ghost block, colours and its line (step 20): locked over grass the block is blue (the driver's `copycolour`);
  nudged into a ghost gold block it's yellow and its click still places; nudged below the world's bottom it's red, and
  its click sends no edit; a Zoop row crossing ghost blocks is yellow; with a world border crossing a row, it's red and
  places nothing. With Planner1 following the aim, no box shows. The lines, each checked word for word: following the
  aim, with Left Ctrl held, for a chest (turn only), a slab (flip only) and stone (neither), turned, locked, Zoop and
  Vertical before the first click, Zoop locked, a row of 12 and of 1, and red; with the lock key on K, the mode key on
  G, the nudge forward on I and the sprint key on Right Control, the lines name them. No line shows with a stick in
  hand or with the copy at the aim (the copy's line shows), and in Line mode only the turning line shows; F1 hides it. Each line is shot at 1920 by
  1080, GUI scale 4, the locked line at 854 by 480 and 1920 by 1080, GUI scale 2, and a yellow row crossing ghost
  blocks at 1920 by 1080, GUI scale 2.

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
- A paste onto built blocks (the driver's `pastehere`, which copies a box and pastes it where it was copied from)
  changes nothing, with no glow.
- Ghost blocks saved over matching real blocks while the server ran without the add-on are gone once their chunk
  loads, and a ghost stair under a real stair facing the other way stays.
- Neither client's log has a self-check warning, checked before the restarts and at the end.
- Planner2 is shot at 854 by 480 and 1920 by 1080, GUI scale 2, looking at a ghost wall before and after real blocks
  fill half of it.

The copy's placement has unit tests as pure functions. `CopyPlacementTest` covers the copy against each of the six
faces, with sides of odd and even lengths, the origin for each turn and mirror filling the turned box, and a turn or
mirror keeping the middle block. `PasteBoxesTest` covers snapping: flush against each face of a paste's box at the
corner nearest the aim, the most recent paste winning, only 16 kept, each copy of a row a paste of its own, and the guide line along a row and a column on the
latest paste's level.

The copy's keys have unit tests too. `CopyNudgeTest` covers each nudge key's direction from the view's yaw: forward the
nearest compass direction, back its opposite, left and right its sides, up and down whatever the facing. `KeyRepeatTest`
covers a key's repeat: a press acts at once, a held key again after 4 ticks and then every 4 (5 a second), the
computer's own key repeat not counting while the key stays down, and each new press acting at once.

The zoop's row has unit tests as a pure function too. `CopyRowTest` covers R's order of the modes; Single as the first
copy alone; Zoop in each compass direction, the copies touching without overlapping, the direction the aim is
farthest along with a tie going east or west, the row ending with the copy that holds the aim, at most 10 copies, and a
turned copy its turned length apart; Vertical up and down its height apart, whatever the aim's distance to the side;
the view's height for Vertical, looking up, down, away and straight up; and the box around the row.

The copy's colour has unit tests as a pure function too. `CopyColourTest` covers a copy on clear ground and one beside
ghost blocks, blue; a copy whose block would replace a ghost block, yellow, but not over a ghost block the same as its
own; a row yellow when any of its copies is; a copy partly above or below the build height and one partly outside the
border, red, and one just inside each, blue; red over yellow; a row over the block cap, red, and one at the cap, blue;
a row reaching past the build height while its first copy doesn't; the cap checked first, as on the server; and ghost
blocks read only while no red check fails. `LayerTest` covers the layer's version, which changes with each change and
only then.

The controls lines have unit tests as a pure function too. `ControlsLineTest` reads each line in English from
`en_us.json`: Single before and after the first paste, locked with and without a paste to line up with (no "line up",
and "R for"), Zoop, Vertical, Zoop locked and Vertical locked before the first click, a fixed row of 4 and of 1 (no "R
for"), each red reason in place of the paste (and a red Zoop or Vertical, locked or not, giving its reason before "set
the start"), the reason as a warning and the actions as actions, keys a player rebound, the nudge keys off the arrow
keys named one by one, the up and down keys named with their shared first word dropped ("Page Up/Down", "Keypad
8/2") or in full ("U/J"), the nudge phrase with only one of its two parts, unbound keys left out and the phrase gone
with all six unbound, and each box's lines, Delete's among them. `PayloadsTest` covers the
Delete box's message writing 16,001 with digit grouping. For the new features (steps 13, 17, 19 and 20),
`ControlsLineTest` also covers Aim's lines (no marks, marks, unmarking, the hold's progress in steps of 5%, the cap's
warning) and the Delete box's "R for Aim", the turning line (each turn phrase, with Left Control held, "Turned:" with
and without it, the sprint key as a player rebound it, the actions as actions, and no line for a block that neither
turns nor flips or with the sprint key unbound), Disable's lines (without Left Control and with no "Turned:", with
it held, turned, each turn phrase, the red "Can't place here", and no line without Left Control for a ladder or a
torch aimed at a wall), and the one ghost block's lines for each state
above, with each turn phrase and title.

Delete by aim's marks have unit tests as pure functions (step 12). `DeleteMarksTest` covers a sweep between two aims
10 blocks apart marking all 10 ghost blocks between them, and none behind a real block; at most 65 aims for a frame's
half turn; an aim at nothing or at a real block marking nothing; a sweep that starts on a mark unmarking until it
ends; a door marked by its lower half as one mark and two positions to erase; the cap, with the other halves counted;
marks dropping when their ghost blocks leave the layer, and staying as they were when none did; and the changes the
outline rebuilds from, each mark, unmark and dropped mark, and all of them after a clear. `MouseModeTest` covers which
actions end Delete by aim. `EraseHoldTest` (step 13) covers the hold: 19 ticks erase nothing, the 20th erases once, holding on erases
nothing more, a release starts the count again, a hold cancelled when its marks go erases nothing, and the progress
shown in steps of 5%.

The one ghost block's rules have unit tests as pure functions (steps 14 to 17). `SingleRuleTest` covers when its
controls are active (session, Single mode, no mouse mode, an item that places a block), which clicks are Building
Planner's in each state, and when the turning controls are active (`SingleRule.turning`: every mode but Disable,
where step 18 adds `DisableTurn`'s rule; none without a session, with a mouse mode on, with an item that places no
block, or during a shape drawn with the break click, but during one drawn with the place click). `CopyRowTest` covers a 1×1×1 row (step 15):
every block to the aim east, north and up, the block alone when aimed at itself, and up to 32 blocks, where the copy's
row stops at 10. `BlockTurnTest` (step 16) covers a
quarter turn each way for stairs, logs, chests and signs, and two turns adding up; the flips' order for a log, a
stair, a slab (a double slab flipping to bottom), a piston (one placed facing up flipping to the way it would face on
its side, and a turn while flipped up turning the way it goes back to), a button and a lantern; the turn kept as
property values and set on another block of the mix that has them, leaving a door's upper or lower half and
waterlogging; which blocks turn and which flip; a block that neither turns nor flips; and the turn dropping when the
item in hand changes. `CopyColourTest` covers a 1×1×1
spot blue, yellow and red.

Disable's turn has unit tests as pure functions (steps 18 and 19). `DisableTurnTest` covers the turn M6 sends: the
turn while Left Ctrl is held in Disable, none without Left Ctrl, in another mode, with no session, with a mouse mode
on, or with a block that neither turns nor flips, and a payload only when the turn differs from the last one sent.
`PlaceTurnTest` covers the turn S2 applies: only for the turn's item, in the main hand; property names outside
`BlockTurn`'s and values the block doesn't have refused; a bed's head space toward the turned facing, which must take
a block; and no turn for a player without one. `DisablePreviewTest` covers where the preview goes: the aimed block
when it can be replaced, the space beside the aimed face otherwise, and none beyond the player's reach while Freecam's
camera is on.

The fill rule's unit tests (`FillRuleTest`) cover each property it compares or ignores: a stair facing another way or
upside down, a log on another axis, a door with the other hinge or its other half, a bed's other part, a button on
another face, a sign turned another way and a hopper, piston, dripstone tip and crafter facing another way stay; a
stair of another shape or waterlogged, a fence or wall with other connections, a waterlogged slab, a crop of another
age, an open or powered door, an occupied bed, a lit furnace and an extended piston fill it; a top or double slab
doesn't fill a bottom one; air fills nothing; and for each of candles, sea pickles, snow layers, turtle eggs, pink
petals and leaf litter, a block of another count stays and one of the same count fills. `LayerFillTest` covers the end-of-tick edit (by the nil author with no
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
  column of the layer, it reads the blocks of every copy in the row until one would replace a ghost block, up to the
  block cap of 200,000; expected a few milliseconds, not measured.
- That Ctrl with the middle click in creative mode picks a block with its contents in Minecraft 26.3 (expected, from
  earlier versions; the client's `pickBlockOrEntity` not read for it), which hook M4 stops while it flips the one
  ghost block.
- That Effortless Building's preview draws a block set Building Planner makes (hook E27) with the mix's block at each
  position. Step 14's checks show it drawing the locked block and an array's copies of it (with hook E30), and step
  15's a row and its mirror image, which the click places too, and a row with the mix on, whose placed blocks are the
  mix's; no check reads which block the preview draws at a position.
- That no Litematica or MaLiLib hotkey acts on Left Ctrl with the wheel or the middle button while Litematica's tool
  item is off, as Building Planner sets it.
- Satisfactory's mass dismantle as Delete by aim follows it: marking by sweeping the aim with Ctrl held rests on one
  player's post from 2020, not the official wiki; whether holding the left button for a second also confirms a mass
  dismantle isn't stated; and the colour of Satisfactory's marks isn't given.
- Turning real blocks in Disable (see "Turning real blocks in Disable"), read in the 26.3 jars' bytecode and Effortless
  Building's source, not run: that the client's prediction and the server's block match with no flicker; that
  `place_turn` reaches the server before the `ServerboundUseItemOnPacket` sent right after it (both go out on the one
  connection, in order, as the layer's snapshots and edits rely on); that `Block.updateFromNeighbourShapes` gives a
  turned stair the shape Minecraft gives one placed facing that way; and that nothing on the server places blocks
  through `BlockItem.place` with a player who has a turn other than that player's own right click (a dispenser's
  context has no player).
- How Freecam 1.5.1 stops a right click while "Allow interaction" is off, and that `Aim` can read the option by
  reflection, as it reads "Player control"; its source wasn't read for either.
- That every orientation property `BlockTurn` stores is a separate property from the one a two-part block uses for its
  part: a door's and a tall plant's `half` (`DoubleBlockHalf`) shares its name with a stair's `half` (`Half`), so
  `BlockTurn` and `PlaceTurn` go by the property, not its name.
- What Delete by aim's marks cost on a player's computer, with a graphics card. In the in-game checks' container (6
  CPUs, Mesa's software OpenGL; `step9.sh`'s log), a frame's sweep of 65 aims took 0.65 ms on average, and 1.2 ms when
  every aim reached the horizon. Building the whole shape of 200,000 marks, as when R brings Aim back with its marks,
  took 59 ms for a solid block of them and 546 ms for 200,000 that touch no other; building it again after one mark
  changed, as in a frame of a sweep, took 0.6 ms on average, and at most 14 ms for marks that touch no other. Frames
  over 200,000 ghost blocks took 86 ms on average with none marked and 90 ms with all of them marked, times that
  software drawing sets. With a graphics card, expected well under a frame; not measured.
