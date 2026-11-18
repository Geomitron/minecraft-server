# Plan

## Current setup

**Server.** Minecraft 26.3 on Fabric (loader 0.20.1, server launcher 1.1.2) with Fabric API 0.166.2+26.3 and
Building Planner 0.3.0 from the release `building-planner-0.3.0` (`/home/minecraft/mods/`), on Geo's VPS. The systemd unit `minecraft.service` runs it as root from `/home/minecraft`:
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java -Xmx6G -Xms1G -jar fabric-server-mc.26.3-loader.0.20.1-launcher.1.1.2.jar nogui`,
port 25565. Survival, hard, whitelist enforced (11 players), online mode. None of the mods adds blocks or items, so players without
mods can join: an unmodded 26.3 client joined a dev server with the same mods (`def7603`). The world (`world/`) is 1.9 GB.
Building Planner keeps the shared ghost blocks in `world/building-planner/layer/`. Its one-time import of Syncmatica's shared
plans found none (Syncmatica's list in `world/syncmatica/placements.json` was empty), so the ghost blocks started
empty; Syncmatica's files, `world/syncmatica/` and `syncmatics/`, stay as they were.
Archives sit beside it: `minecraft-backup-2026-11-17-pre-building-planner-0.3.0.tar.gz` (the world and `syncmatics/`
just before Building Planner 0.3.0 was installed, 1.5 GB), `minecraft-backup-2026-11-16-pre-building-planner-0.2.0.tar.gz`
(the world just before Building Planner 0.2.0 was installed, 1.5 GB), `minecraft-backup-2026-11-15-pre-building-planner.tar.gz` (the world just before
Building Planner 0.1.0 was installed, 1.5 GB), `minecraft-backup-2026-11-14-pre-fabric.tar.gz` (the world just before
the switch to Fabric, 1.5 GB), `minecraft-archive-2026-10-29-1.21.11.tar.gz` and `minecraft-backup-2025-12-12.tar.gz`.
To take Building Planner off the server, remove `mods/building-planner-0.3.0.jar` and run
`systemctl restart minecraft.service`.
The vanilla `server.jar` stays in place: to switch back, change the jar in `ExecStart` to `server.jar`, then run
`systemctl daemon-reload` and `systemctl restart minecraft.service`. The VPS has 30 GB of RAM (about 16 GB available) and
163 GB of free disk; it also hosts other services.

**Error watcher.** `ops/error-events/` runs beside the server from `/home/minecraft-error-events` (Docker Compose,
container `minecraft-error-events` on the network of that name, no published port). It gives each error a short id and
exports counts for Prometheus. The drop-in `/etc/systemd/system/minecraft.service.d/error-events.conf` records how the
process ended at each stop. Its README holds the metrics, the alert expressions and the command that marks an error
handled. The VPS's Grafana alerts on it (deployed 10:53 PM ET, Nov 13): a new error, a known error's rate jump (able
to fire once the watcher has 25 hours of history, from about 11:54 PM ET, Nov 14) and the watcher going quiet for 10
minutes. Its contact point **Minecraft Server error events** emails Geo and sends each alert to the project thread as
an inbox notice, carrying only the error's id, kind and source.

**Player pack.** This repository is public at https://github.com/Geomitron/minecraft-server. `pack/` is the packwiz
pack "Server Pack": Minecraft 26.3 and Fabric loader 0.20.1 with Litematica, MaLiLib, Freecam, Fabric API, Building
Planner 0.3.0 (downloaded from its release `building-planner-0.3.0`), and Effortless Building 4.3, served from `main`
at https://raw.githubusercontent.com/Geomitron/minecraft-server/main/pack/pack.toml, so a push to `main` changes what
players get at their next launch. The server and the pack run the same Building Planner, protocol 3; a game with an
older pack still joins, and Building Planner does nothing in it. `instance/` is the Prism Launcher instance "Server
Pack", which runs `packwiz-installer-bootstrap.jar` against that URL before each launch. Players import it from
https://github.com/Geomitron/minecraft-server/releases/latest/download/prism-instance.zip, a zip of `instance/` in the
release `v1.0.0`; the instance needs a new release only when `instance/` changes. The guide sent to players is
[players.md](players.md).

**Players.** Unmodded until they install the pack. Geo prefers Prism Launcher (Geo, inbox request 402). Geo is new to how Minecraft mods
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
  repository, a public GitHub repository under Geo's account (409, and 402 r4 Q1).
- Players run Litematica (with MaLiLib), Syncmatica and Freecam, with Fabric API; the server runs Fabric, Fabric API
  and Syncmatica. These are the mods 402 r2 to r4 proposed and 408 described when Geo approved the pack and the switch.
- The player guide covers Prism Launcher only, with no steps for the official launcher (410, comment c1).
- Publish the repository with the player guide (`docs/players.md`), the README and the pack and instance name "Server
  Pack" as Geo wrote them, and the instance at the release link the guide gives (410 r2, Q1 A).
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

How players use Building Planner, from inbox request 420:

- A player starts a plan by marking the area, pressing a key and typing a name (Q1 A).
- A shared plan appears for everyone straight away, and each player can hide it (Q2 A).
- An Effortless Building shape goes into the plan it starts in, or else the selected plan (Q3 A).
- A shape can start on a plan's ghost blocks as well as on real blocks (Q4 A).
- The block-mix picker offers every block in the game (Q5 A). Geo would like a toggle that filters it to the blocks in
  the player's inventory (Q5 comment).
- Ctrl+Z undoes and Ctrl+Y redoes (Q6 A).
- When someone else edits a plan, the changed ghost blocks glow briefly, with the editor's name (Q7 B).
- Copy and paste of part of a plan comes after the first version works (Q8 A).
- Server operators can delete any plan, as well as its creator (Q9 A).
- Building Planner's code uses the MIT licence (Q10 A); the repository has no other licence.
- Agents push finished, tested Building Planner work and doc changes to this repository without asking each time;
  changes to the pack or the instance, and releases, go to Geo first (inbox request 429, Q1 A).

Building Planner as built, from inbox requests 469, 473, 477, 482, 483, 485, 570, 571, 578, 584 and 613:

- N is the "Start a plan" key until a player changes it in Controls (Q1 A).
- Players hide a plan with Litematica's rendering switch or Syncmatica's Unload; Building Planner adds no switch of its
  own (Q2 A).
- Another player's edit glows light cyan, fading over 3 seconds, with the editor's name (Q3 A).
- Building Planner's messages to a player show in chat (Q4 A).
- Geo kept the "Start a plan" screen's title, name hint and Create button, the message when no area is marked, and
  the Controls heading and key name as written.
- Geo kept Building Planner's other messages to players and its description as written, with a message of its own
  for one block left as it was after an undo or redo: "1 block someone else has changed since was left as it is."
  (inbox request 473).
- B opens the block-mix picker (477 Q1 A). The picker keeps its layout: search and the inventory switch at the top,
  the blocks, then the mix with a share box under each block, and the buttons (Q2 A).
- A player's mix lasts until they close the game (Q3 A). While it's on, every right-click draws into a plan, even with
  no Effortless Building mode chosen (Q4 A); Geo reversed this after the walkthrough (below).
- Geo kept step 7's texts as written, except two: the empty mix row reads "Click blocks above to add them. (up to
  nine)", and each share box's tooltip reads "Percentage of the mix" (477).
- Building Planner works around Syncmatica's crash when a plan another player has loaded is removed (482 Q1 A). The
  crash isn't reported to Syncmatica (Geo, inbox request 571: "nevermind, don't post this").
- When Building Planner is released to players, the player guide's list of mods gains Building Planner ("lets
  everyone edit the same plan at once, and shows each change to everyone as it's made") and Effortless Building
  ("draws lines, walls, floors, and other shapes into a plan"), as Geo approved them (inbox request 570).
- Building Planner is released to players: on the server, and with Effortless Building in the player pack (570 Q1 A).
  The player guide gains a section on planning builds with these mods, drafted for Geo's review (Q2 A).
- On a server without Building Planner, or with another version of it, Building Planner turns off silently, with no
  message (Geo, inbox request 483: "none of these messages are necessary; just silently disable the mod in these
  cases").
- Copy and paste (485): a Copy key makes a movable copy of the plans' blocks in the marked area, and a Paste key
  writes it into a plan as one edit (Q1 A). A paste writes only the copy's blocks, so the empty space in it leaves
  the plan's blocks as they are (Q2 A). A paste goes into the plan where the copy's corner sits, or else the plan it
  was copied from (Q3 A). The spec's section on copy and paste has the design.
- Copy and paste's keys are J (copy), K (paste), U (turn), H (mirror), and Delete (clear), until a player changes them
  in Controls (584 Q1 A). Copy switches Litematica's tool to "Schematic Placement" (Q2 A). Geo kept copy and paste's
  texts as written. Copy and paste goes to players as Building Planner 0.2.0, released the same way as 0.1.0 (Q4 A).
- A plan's blocks show under the copy's empty space: Building Planner fills that space with structure void, which
  Litematica doesn't draw and a paste skips (613 Q1 A). The change goes to players as Building Planner 0.2.1 in the
  player pack, while the server keeps 0.2.0, since the change is only in the players' game (Q2 A).
- Before the player guide's section on planning builds is published, Geo walks through every planning feature in the
  game from a runbook with room for feedback (Geo, inbox request 578: "Build a runbook to teach me how it works, walk
  me through using all the features we have available, with space for me to provide feedback and suggestions on how to
  adjust the experience of using the mod. Once I'm happy with it, we can publish a guide on how to use it.").

From Geo's walkthrough (inbox request 608) and requests 669 to 709 after it, not built yet:

- Ghost blocks show see-through at 0.8 opacity by default: "render blocks as translucent is true", "ghostBlockAlpha
  default to 0.8" (608 note). A player's own change to either stays.
- In Effortless Building's "Single" mode, holding the button keeps placing or erasing ghost blocks: "I want to be able
  to place or destroy quickly" (608 note). Two sliders in Effortless Building's menu set the placing speed and the
  erasing speed (Geo, comment c2 on inbox request 670: "I'd like this to be two different configurable sliders on the
  effortless building UI, easy to change as you work"). They read "Build speed: 5 blocks/s" and "Break speed: 3.33
  blocks/s" at their first settings (Geo's wording, inbox request 703). They sit aligned just below the menu's
  left-hand buttons, narrower and a bit smaller than first built, clear of a mode's description: "C and D, and make
  them a bit smaller if possible. Also move them to be aligned just below the other config buttons." (703 Q1, where C
  moved the sliders away from the description and D made them narrower with shorter labels). They stay in the menu
  rather than in the wrench button's Modifiers screen (703 revision 2 Q4: "A. In the menu, under the buttons, as
  shown."). The first repeat waits at
  least 0.2 seconds, the game's own wait before a held click repeats, then repeats at the slider's speed (703 Q2 A).
- In every Effortless Building mode, with anything but a placeable item in hand (a tool, food, an empty hand), every
  click acts as without mods, breaking and using real blocks; ghost blocks are drawn and erased only with a placeable
  item in hand (704 Q1 A). Placeable items include more than blocks: "only with a block in hand" should be expanded
  to placeable things, such as redstone, fences, etc..." (Geo's comment on 704). The agent reads that as every item
  that places a block (blocks, redstone dust, fences, doors, signs, seeds, string and the like); items that place an
  entity or a fluid (item frames, armor stands, boats, buckets) act as without mods, since ghost blocks show only
  blocks.
- With "Disable" chosen in Effortless Building, clicks act as they do without mods, even with the block mix on. The
  mix must never be on without the player knowing: "I don't want that feature of block mix; I'd rather deliberately
  select which blocks to use with the block mix UI open... either that or some much clearer "block mix selection mode"
  that makes it much clearer that I'm in a special mode for that and it's clear what's happening (such as it
  displaying the current block mix and the keys or controls to save, cancel)" (Geo, replying to inbox request 669).
  While the mix is on, a panel at the edge of the screen shows its blocks and shares, and the keys to change it or
  turn it off (670 Q2 A). Choosing the mix is a mode of its own: "choosing the mix is a distinct mode, where after
  chosen, the UI for it goes away, but it now applies to the effortless building's block placing controls until
  disabled. ... it should still be clear whether the currently selected block is just the one you're placing, or if
  you're placing a mix. But the point of the mix mode is to allow the player to look at specific blocks and pick them
  to be added to the current mix, I think." (Geo's comment on 670 Q2). Players choose the mix's blocks only in the
  block mix screen, with its search and "Only my inventory" switch (671 Q1 A), not by looking at blocks: "I don't want
  to be able to select blocks by looking at them: Just make it so that you have to select blocks using the block search
  (with the only my inventory filter)" (671, note c3). The mix applies only while a placeable item is selected: "this
  should only apply when placeable items are selected on the hotbar; switching to tools or nonplaceable items will go
  back to normal behavior and will hide the mix panel until placeable items are selected again" (671, comment c2).
- Ghost blocks stay shared live with everyone on the server, as one layer rather than separate plans, and each player
  shows or hides all of them with one switch (Geo, comment c1 on inbox request 670: "it's still very important for
  ghost blocks to be synced and shared for users on the same server, where each player can choose whether to see
  active ghost blocks or not. But we don't need to group them into different schematics either; it can be a simple
  toggle control for the user to see all ghost blocks or hide all ghost blocks").
- Litematica draws the shared layer out of sight, and Syncmatica leaves the server and the player pack (672 Q1 B).
  Effortless Building is the only set of controls, and its menu gains copy and paste buttons; the "Cube" shape marks
  the area to copy, and the stick and Litematica's tool modes play no part in planning (608 note).
- Anyone may erase ghost blocks someone else placed; each player's undo takes back only their own changes (672 Q2 A).
- The material list counts the ghost blocks in an area the player marks with the "Cube" shape, as copy does (672 Q3 A).
- A key and a button in Effortless Building's menu both show or hide the ghost blocks (672 Q4 C).
- Drawing a shape while the ghost blocks are hidden shows them again, then draws (672 Q5 C).
- Copy takes the ghost blocks and the real blocks in its "Cube" box together, as one copy to paste as ghost blocks,
  with no separate "Copy real blocks" (742 note: "I actually just want one copy operation that copies both at the same
  time, regardless of what was in the copy box that you drew").
- The first release of the shared layer moves every current plan into it, including plans players have hidden; where
  plans overlap, the later plan's blocks win (672 Q7 A).
- While the mix is on, the block mix key (B) opens the mix screen to change the mix; the screen gains a "Turn off"
  button (677 Q1 A).
- The mix panel, as Geo approved it with changes (inbox request 709): the heading "Block mix", then each block's icon
  with its share only ("80%"), and no line about B: "remove B: close; the chat message is enough" (709 revision 2
  Q2). It is narrow: "note that my changes allow it to be significantly vertically narrower, which I want" (709 Q1).
  "Use mix" leaves the chat message "Block mix on. Press B to change it or turn it off."
- The panel's text and icons, and the speed sliders' labels, are drawn small but evenly: at 70% the font's pixels
  came out uneven and hard to read ("just restore the panel to a slightly larger size where the font doesn't have
  the weird scaling issues that makes it hard to read", 709 revision 2 Q3; "same text issue happens with the scaled
  down sliders", Geo in the project's thread). Each is drawn at the smallest size at or above 70% at which every font
  pixel covers a whole number of screen pixels: full size at GUI scales 1 to 3, 75% at 4, 80% at 5.
- At that size the sliders' one-line labels don't fit between the menu's buttons and its ring, so both sliders are as
  wide as the longer label needs, ending where they did, outside the ring, and reaching further left than the buttons
  (709 revision 3 Q1 A, over two-line labels). In a GUI narrower than about 420 pixels, such as a 1024×768 window at
  GUI scale 3, they run off the screen's left edge and cut off "Build" and "Break"; Geo saw that before choosing.
- The copy follows where the player aims: its corner sits on the block they look at, until they paste or clear it
  (677 Q2 A).
- N shows or hides the ghost blocks, since "Start a plan" no longer needs it (677 Q3 A).
- Ghost blocks only add blocks to a build; planning which real blocks to remove waits for a later design (677 Q4 A).
- Building Planner turns Litematica's tool item off once, since the stick plays no part in planning ("no need for
  Litematica's controls or the stick", 608 note). Geo saw this in 677 without a comment, with the agent's other
  choices: old plans leave Litematica's list while their files stay; "Material list" is a button without a key; the speed sliders run from 1 to 20 blocks a second; the mix panel sits at the right edge;
  Copy switches Effortless Building to "Cube" to mark the area, then back.
- Building Planner's buttons in the radial menu sit above Effortless Building's wrench, undo and redo, in the same
  columns, with the Turn, Mirror and Clear row above them while a copy exists (729 Q1 B). They act when the player
  lets go of the menu key over them, as Effortless Building's own buttons do (729 Q3 A).
- Showing or hiding the ghost blocks, by N or the button, says so in chat each time, such as "Ghost blocks hidden."
  (729 Q4 B).
- The buttons' icons are generated to match Effortless Building's own, and Geo picks from several options: "I don't
  like either; would it be possible to image generate icons that fit the style of the other image icons? That means
  pixelated at about the same resolution with black borders. Maybe there's something in my opencode provider that
  can do a good job at this? I'd want to pick from multiple generated options." (729 Q2, over Lucide's line icons
  and Minecraft items). Geo picked each icon from every generated variant and from variations on their first picks
  (738): show and hide from Set 14, Copy from Set 16, Paste from Set 56a, "Material list" from Set 3, Turn from Set
  31, Mirror from Set 66a (GPT-5.4 Image 2) and Clear from Set 6. Geo fine-tunes each icon by hand: "These still
  have issues; put them all in a folder so that I can fine tune edit each one myself in Paint.NET" (738 revision 2,
  c1).
- Geo approved the texts players see in 0.3.0 as 742 showed them (the chat messages, the button and key names, and
  the material list's title), with Copy's texts as revision 2 showed them for the one Copy above, and the mod's
  description in Geo's wording: "Plan builds together using ghost blocks that everyone on the server can see." Geo saw
  the radial menu without "Copy real blocks", with "Material list" in its slot, and the rule that where a ghost block
  stands over a different real block, Copy takes the ghost block (742 revision 2).
- Geo's walkthrough of 0.3.0 on the server (752) settles these for the next version:
  - **Copy and paste without keys:** "We don't need keys." While the copy shows at the aim, its controls show on
    screen: a left click pastes, a right click leaves the copy, the scroll wheel turns the copy and a middle click
    mirrors it, ahead of what those do otherwise ("you're not able to scroll to different tools on the hotbar with
    scroll wheel; that rotates the copied blocks instead"). Doing anything else in the mod, such as choosing "Wall"
    or pressing "Material list", leaves the copy. Copy's box is a mode of its own, not "Cube": the wheel doesn't
    highlight "Cube", and the text on screen is the box's own.
  - **The radial menu's buttons:** show/hide, Copy, "Material list", Paste (back to the most recent copy, even after a
    right click left it) and a new Block mix button; Turn, Mirror and Clear go. The buttons join the menu's opening
    animation and are rearranged cleanly. "Effortless Building" where it shows, such as the corner text while the
    wheel shows, reads "Building Planner", and Effortless Building's key category heading reads as words (c1).
  - **Boxes for Copy and the material list:** only a left click draws one, with a prompt on screen (Q2). Its first
    corner is the space in front of the aimed block when aiming at a block's top, and the aimed block otherwise (Q1).
    Boxes may be much larger ("Let's dramatically increase the amount we can select"), and a box never shrinks to fit
    a limit. The list of items shown while drawing wraps onto more lines instead of running off the screen. Pressing
    "Material list" while Copy waits for a box switches to the material list's box, and the other way round (Q4 B).
  - **Controls on screen:** "if there is an active "mode" (such as copying, specifying a materials list, or
    building/breaking ghost blocks), then the usable controls related to it should be shown on screen" (Q2).
  - **Hidden ghost blocks:** the copy follows the aim through them (Q5 A). While they're hidden, Copy and the
    material list leave out the ghost blocks in the box (Q5 comment), and the material list leaves them hidden (Q6 B).
  - **The material list:** a box without ghost blocks opens an empty list (Q7 B), and the list counts the real blocks
    in the box as well as the ghost blocks (c2).
  - **The block mix:** a click on a block in the search results adds or removes it; the panel shows "B: Edit Mix"
    under it; one "Clear Mix" button empties the mix and turns it off, in place of "Clear" and "Turn off"; "Only my
    inventory" is on by default.

  Geo's answers on the design (766) settle the rest:
  - **Box limits:** at most 256 blocks a side, and copies and pastes of up to 200,000 blocks, with the server's undo
    budget of 2,000,000 blocks a player (Q1 A). A corner may be aimed at up to 256 blocks away (Q2 C).
  - **The copy at the aim:** showing or hiding ghost blocks puts it away; undo, redo and the speed sliders keep it
    (Q3, Geo's words: "showing/hiding ghost blocks will put it away, but undo, redo, and the speed sliders do not").
  - **Material list:** one list; a real block with no ghost block over it counts in "Total" and never in "Missing"
    (Q4 A).
  - **Radial menu:** two rows above the wrench, undo and redo, in their columns (Q5 A). The show and hide icons get
    a larger eye, and slash over it, with the bottom-right corner fixed and the block behind unchanged (Q5 comment):
    Geo found the doubled eye too big and picked 12 by 10 pixels from the sizes between (768). The "Block mix"
    button shows Effortless Building's Randomizer Bag (Q8 A).
  - **Keys:** Effortless Building's four keys go under the "Building Planner" heading (Q6 A).
  - **Litematica's area selection:** its drawing is turned off once on each player's computer (Q7 A).
  - **Messages:** none when a box ends early (Q9 A, the walkthrough's Q3).
  - **"Clear Mix":** keeps the block mix screen open to pick a new mix (Q10 A).

  Geo saw steps 1 to 3 in the game (774) and kept the controls line where Effortless Building draws its own, though
  a chat message covers its left end for about 10 seconds (Q1 A).
- The shared layer replaces the decisions above that concern separate plans: named plans that move and turn as a whole
  (411 Q1), plan growth (411 Q2), who deletes a plan (411 Q3, 420 Q9), starting a plan with N (420 Q1, 469 Q1), hiding
  a plan with Litematica or Syncmatica (420 Q2, 469 Q2), the paste's target plan (485 Q3), Litematica's Edit Schematic
  actions syncing live (411 Q5), and Syncmatica on the server and in the pack (408 Q2).

## Agent-ready work

**Building Planner 0.3.0, the shared layer** (the decisions above from inbox request 608 on;
[building-planner.md](building-planner.md) is the design). Hold to repeat and the look of ghost blocks are built
(`step10.sh`). The steps below come in this order, each small enough for one child, which pushes it to
`main` once its check passes (inbox request 429).

1. **The block mix's rules.** Built (`step7.sh`): the mix applies only while the main hand holds a block, and in
   Disable every click is vanilla's (hooks E5 and E6). With anything but an item that places a block in hand, every
   click acts as without mods, in every mode (hooks E13 to E15, 704).
2. **The mix screen's "Turn off" and the mix panel.** Built (`MixPanel`, `SmallText` and `BlockMixScreen` in the
   client; `step7.sh` passes), as Geo approved them in 709.
3. **The radial menu's widgets and the two speed sliders** (hooks E11 and E12; the sliders set E10's gaps and are kept
   in the client config). Built (`RadialMenuWidgets` and `RepeatSlider` in the client, `RepeatSpeed`, with its
   unit tests, and `ClientConfig`; `step10.sh` passes), in the layout Geo chose in 709. The radial menu's
   buttons come with steps 9 and 10.
4. **The layer's storage and files** (common): columns of 16-block sections with palettes, and the area files. Built
   (`dev.buildingplanner.layer`, with its unit tests).
5. **The server's layer**: the store, column watching, edits in world positions with their checks, undo and redo,
   messages and protocol version 3. Done when unit tests over the simulated network cover a snapshot then later edits
   in order, every rejection, undo and redo across dimensions, and a save and reload. Built (`LayerSync` and
   `LayerStore` in `dev.buildingplanner.server`, the payloads in `dev.buildingplanner.sync.Payloads`, with unit
   tests).
6. **The import, and Syncmatica off the server**: the one-time import, and removing hook S1, `SyncmaticaPlans`, the
   delete check, the plan files' archive, `max-plan-side` and Syncmatica from `fabric.mod.json`, the build and the dev
   server. Done when the import's unit tests pass and the dev server, started on a dev world with shared plans and
   without Syncmatica, logs the import and serves the same blocks. Built (`PlanImport` in
   `dev.buildingplanner.server`, with `PlanImportTest` and the in-game check `import.sh`).
7. **The player's layer and its tiles**: the copy of the layer, column watching, tiles, shapes and erasing in world
   positions, and the edit glow on the layer. Built (`LayerReplica` in `dev.buildingplanner.sync`, with its unit
   tests, and `Tiles` and `ClientSession` in the client; `step6.sh`, `step7.sh` and `import.sh` pass).
8. **Tiles out of sight, and Syncmatica off the client**: hooks L4 to L7, L1 as a guard, removing S2, S3, L2, L3,
   "Start a plan" and `remove-loaded.sh`, the removal of old plan placements at a session's start, and the mixin
   plugin's Litematica rule. Built (the hooks in `dev.buildingplanner.mixin.litematica`, `OldPlans` in the client;
   `step6.sh`, with its checks on hidden tiles, and `step8.sh` pass).
9. **Show or hide**: the key, the radial menu button, showing again when drawing, and the tool item off by default.
   Built (`GhostsShown`, with its unit tests, the switch and its chat lines in `Tiles` and `ClientSession`, and
   `RadialButton` and `RadialMenuWidgets` in the client, with the buttons' place, triggers and icons as Geo chose them
   in 729 and 732; `step9.sh`'s checks other than copy, paste and the material list, and `step10.sh`, pass).
10. **Copy and paste on the layer**: the "Cube" box for Copy, which takes the ghost blocks and the real blocks in it
    (E1, E6), the copy following the aim, and the radial menu's buttons. Built (`BoxWait`, `GhostAim`, hook E17,
    `CopyRules` with its unit tests, and the copy in `Clipboard` and `ClientSession`, with the radial menu's Copy and
    Paste buttons in layout B's slots and Material list in the row above them, and Turn, Mirror and Clear in the row
    above that while a copy exists, with step 9's triggers and icons; `step9.sh`'s copy and paste checks pass).
11. **The material list for a marked area.** Built (`MaterialCount`, with its unit tests, and `GhostMaterialList`,
    with the radial menu's Material list button waiting for a "Cube" box as Copy does; `step9.sh`'s material list
    checks pass).
12. **Texts for Geo**: every text players see that the steps added or changed (messages, key names, button and slider
    labels, the panel) goes to Geo as editable text, and Geo's version is built in. Built: the game shows the wording
    Geo approved in 742 (`en_us.json`, and the description in `fabric.mod.json`), with `step9.sh` and `step10.sh`
    checking the texts exactly.
13. **The release**: version 0.3.0, as Geo approved it in 746. Released: the GitHub release `building-planner-0.3.0`
    (`c2d951a`), on the server after a backup, with Syncmatica removed (the import found no shared plans), and in the
    pack without Syncmatica, with the guide's list of mods and the README in the wording Geo approved (`59ed2a9`).
    "Current setup" above describes it. Geo's walkthrough (752) drew ghost blocks on the server from the updated pack.

**The walkthrough's changes** (752, in "Decisions" above), as one update with Geo's hand-edited icons.
[building-planner.md](building-planner.md) is the design, with Geo's answers (766). The steps come in this order,
each small enough for one child. A step that changes nothing players see goes to `main` once its checks pass (inbox
request 429). A step that changes what players see is committed without pushing, and goes to `main` once Geo has seen
its in-game screenshots, with the changes outlined in red; steps 1 to 3 go to Geo together, as do step 5 and steps 7
to 10.
Nothing goes to the server or the pack before the last step.

1. **The mouse modes and the copy at the aim** ("Building Planner's mouse modes" and "Copy and paste"): `MouseMode`,
   hooks M1 to M5, E14's and E15's new condition, E21 and E22, `ModeHud` with the controls line, the copy put away
   and brought back by Paste (`Clipboard` keeps the schematic), the counterclockwise turn, and the keys J, K, U, H
   and Delete removed with their texts and icons. Done when unit tests cover `Turn`'s counterclockwise quarter and
   which actions end a mode, and `step9.sh`'s copy, paste and put-away checks pass, with the driver's `scroll` and
   middle click. Built (`MouseMode` and `Turn.turnedCounterclockwise`, with their unit tests, hooks M1 to M5 in
   `dev.buildingplanner.mixin.minecraft`, E14's and E15's new condition, E21 and E22, `ModeHud`, and `Clipboard`
   keeping the copy put away; `step9.sh`'s copy, paste and put-away checks pass, sending the wheel and the middle
   button with xdotool).
2. **Building Planner's box** ("Boxes for Copy and the material list"): `BoxDraw` with its three left clicks, the first
   corner rule, the outline through MaLiLib, switching between Copy and the material list, the limits with the red box
   and its line, and `BoxWait`, E1's and E2's box calls, E13's box rule and E17 removed. Done when unit tests cover the
   corner rule, the second corner's plane and the height (pure functions over the eye, the look and the first
   corner), and each limit, and `step9.sh`'s box checks pass. Built (`BoxCorners` and `MouseMode.BOX`, with their unit
   tests, `BoxDraw`, `BoxOutline`, the box's lines in `ModeHud`, and `GhostAim.aim`; `step9.sh`'s box checks pass,
   with shots of a box for Copy and for the material list after its first and second clicks, and of each limit).
3. **The count and the list under the crosshair**: `BoxCount` reading whole sections, the wrapped list with its "+N"
   cell, and E8 clearing Effortless Building's break list during a session. Done when unit tests cover the count by
   sections against a count position by position, the wrap and the "+N" cell at 854 by 480, GUI scale 2, and
   `step9.sh`'s list check passes with its shot. Built (`BoxCount`, with Copy counting a box by it before taking any
   block, and `BoxListLayout`, with their unit tests, `BoxList`, the list and the size kept whole in `ModeHud`'s
   wrapped line, and E8's wrap of the break list; `step9.sh`'s list checks pass, with a box of 144 by 3 by 144, the
   widest the dev server's view distance loads, and the E8 check).
4. **Hidden ghost blocks left out** (`CopyRules` and `MaterialCount` taking whether ghost blocks show, and the material
   list no longer showing them). Done when the unit tests cover a box with both kinds of block, shown and hidden, and
   `step9.sh`'s hidden copy check passes. Built (`CopyRules.blocks` and `MaterialCount.of` take whether ghost blocks
   show, with their unit tests, and the material list leaves hidden ghost blocks hidden; `step9.sh`'s hidden copy and
   hidden material list checks pass).
5. **Real blocks in the material list** ("The material list"): `MaterialCount` counting the real blocks in "Total" (766
   Q4 A), and an empty list for an empty box. Done when a unit test gives the worked example's counts and
   `step9.sh`'s material list checks pass. Built (`MaterialCount.of` takes the real blocks as `BoxCount` reads them,
   with its unit tests including the worked example, `GhostMaterialList` opens for any box, the message for a box
   without ghost blocks is gone, and the title reads "Material list for the blocks in the box"; `step9.sh`'s material
   list checks pass, with shots of the list for a box with ghost and real blocks shown and hidden, and of an empty
   list).
6. **The server's cap and undo budget**: `max-edit-blocks` at 200,000, and `UndoHistory` dropping old steps past
   the budget of 2,000,000 blocks a player. Done when unit tests cover an edit at the cap, one over it, and the budget
   dropping the oldest steps while keeping the newest. Built (`SyncLimits.DEFAULT` at 200,000, the wire's own most, and
   `UndoHistory`'s block budget, with `UndoHistoryTest`, `LayerSyncTest`'s edit at the cap and `PayloadsTest`'s edit of
   the most blocks at their largest in the room `registerLarge` is given; `step9.sh`'s cap checks pass, with an
   edit of exactly 200,000 blocks sent through the server to both games, and a box and an edit over it refused).
7. **The block mix's changes** ("The block mix and its panel"): the click that toggles, "Clear Mix", "Only my
   inventory" on at launch, and "B: Edit Mix" in the panel. Done when `step7.sh`'s mix checks pass with their shots.
   Built (`BlockMixScreen` toggling a block with a click, keeping "Only my inventory" while the game runs, and its
   "Clear Mix" emptying and turning off the mix with the screen left open, and `MixPanel`'s "B: Edit Mix" line;
   `step7.sh`'s mix checks pass, with shots of the screen at 854 by 480 and 1920 by 1080).
8. **The radial menu's five buttons**: the two rows above the wrench, undo and redo, the Block mix button with the
   Randomizer Bag, the selected look for the current mode's button, E19, and E20 with the widgets moving with the
   menu's animation. Done when `step9.sh`'s radial menu checks pass at all three sizes, with the shots 1 and 2 frames
   after opening, and `step10.sh`'s slider checks still pass.
9. **Building Planner's name and the key heading**: E18, and E23 moving Effortless Building's keys under "Building
   Planner". Done when `step9.sh`'s corner text and Controls checks pass with their shots.
10. **Litematica's area selection**: its drawing turned off once, as the look of ghost blocks is set. Done when
    `step9.sh`'s selection check passes and `step10.sh`'s look checks cover the option.
11. **Texts for Geo**: every text the steps added or changed (the controls lines, the messages, the button names, "Clear
    Mix", "B: Edit Mix", the key heading and the material list's title) goes to Geo as editable text, and Geo's version
    is built in, with `step7.sh` and `step9.sh` checking the texts exactly.
12. **The release**: version 0.4.0 with Geo's hand-edited icons, after Geo's approval: the GitHub release, on the
    server after a backup (with `max-edit-blocks=200000` in its `config/buildingplanner.properties`, which has
    50000 written), and in the pack, with the README's description of the changes in Geo's wording.

## Design work: Building Planner

[building-planner.md](building-planner.md) is the spec: what players get, how each goal above is met, the technical
design with every hook checked in the source, and what's unverified. Geo's decisions are above. The texts players see
go to Geo for review once something is built.

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
  - All ten Edit Schematic actions run from `event/InputHandler.java` into `util/SchematicUtils.java`, which writes
    blocks only through four `container.set` calls, each also marking the plan modified: a hook point for live sync.
- **Syncmatica**: `sakura-ryoko/syncmatica`, branch `26.3` (`37a969e`), CC0. `data/ServerPlacement` identifies a
  plan's contents by a fixed hash of its file (`hashValue`); `MODIFY` packets carry only placement values (origin,
  rotation, mirror, sub-regions). Block changes don't sync today; a changed plan must be shared again as a new file.
  - Ealicence and publishes its source; an add-on's licence is free; CC0 asks nothing.

Unverified: whether a Fabric player who lacks Effortless Building is turned away like an unmodded one (likely, from
the registry code).
