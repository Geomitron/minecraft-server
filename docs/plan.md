# Plan

## Current setup

**Server.** Minecraft 26.3 on Fabric (loader 0.20.1, server launcher 1.1.2) with Fabric API 0.166.2+26.3 and
Building Planner 0.10.0 from the release `building-planner-0.10.0` (`/home/minecraft/mods/`), on Geo's VPS, with
`max-edit-blocks=200000` in `config/buildingplanner.properties`. The systemd unit `minecraft.service` runs it under its
own account, `minecraft`, from `/home/minecraft`:
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java -Xmx6G -Xms1G -jar fabric-server-mc.26.3-loader.0.20.1-launcher.1.1.2.jar nogui`,
port 25565. systemd locks it down: the server can write only in `/home/minecraft` and a private `/tmp`, and it cannot
gain privileges or use capabilities, devices, or kernel settings (`NoNewPrivileges`, `CapabilityBoundingSet=`, `ProtectSystem=strict`,
`ProtectHome=read-only`, `ReadWritePaths=/home/minecraft`, `PrivateTmp`, `PrivateDevices`, `ProtectKernelTunables`,
`ProtectKernelModules`, `ProtectKernelLogs`, `ProtectControlGroups`, `ProtectClock`, `ProtectHostname`,
`RestrictSUIDSGID`, `RestrictRealtime`, `RestrictNamespaces`, `LockPersonality`, `SystemCallArchitectures=native`;
not `MemoryDenyWriteExecute`, which breaks Java's JIT). `minecraft` owns `/home/minecraft` and everything in it except
the archives. Run `chown minecraft:minecraft` on each file copied there as root, such as a release's jar or an edited
config, so the server can write it like the rest.
Survival, hard, whitelist enforced (11 players), online mode. None of the mods adds blocks or items, so players without
mods can join: an unmodded 26.3 client joined a dev server with the same mods (`b739caf`). The world (`world/`) is 1.9 GB.
Building Planner keeps the shared ghost blocks in `world/building-planner/layer/`, and each player's material lists
in `world/building-planner/material-lists/`, a folder it creates once a player counts a box. Its one-time import of Syncmatica's shared
plans found none (Syncmatica's list in `world/syncmatica/placements.json` was empty), so the ghost blocks started
empty; Syncmatica's files, `world/syncmatica/` and `syncmatics/`, stay as they were.
Archives sit beside it, readable only by root (mode 600):
`minecraft-backup-2026-11-28-pre-building-planner-0.10.0.tar.gz` (the world and `syncmatics/` just before
Building Planner 0.10.0 was installed, 1.5 GB), `minecraft-backup-2026-11-28-pre-building-planner-0.9.0.tar.gz` (the world and `syncmatics/` just before
Building Planner 0.9.0 was installed, 1.5 GB), `minecraft-backup-2026-11-27-pre-own-account.tar.gz` (the world and `syncmatics/` just before the server first ran
under its own account, 1.5 GB), `minecraft-backup-2026-11-27-pre-building-planner-0.8.0.tar.gz` (the world and `syncmatics/`
just before Building Planner 0.8.0 was installed, 1.5 GB), `minecraft-backup-2026-11-26-pre-building-planner-0.7.0.tar.gz` (the world and `syncmatics/`
just before Building Planner 0.7.0 was installed, 1.5 GB), `minecraft-backup-2026-11-23-pre-building-planner-0.6.0.tar.gz` (the world and `syncmatics/`
just before Building Planner 0.6.0 was installed, 1.5 GB), `minecraft-backup-2026-11-20-pre-building-planner-0.5.0.tar.gz` (the world and `syncmatics/`
just before Building Planner 0.5.0 was installed, 1.5 GB), `minecraft-backup-2026-11-18-pre-building-planner-0.4.0.tar.gz` (the world and `syncmatics/`
just before Building Planner 0.4.0 was installed, 1.5 GB), `minecraft-backup-2026-11-17-pre-building-planner-0.3.0.tar.gz`
(the world and `syncmatics/` just before Building Planner 0.3.0 was installed, 1.5 GB), `minecraft-backup-2026-11-16-pre-building-planner-0.2.0.tar.gz`
(the world just before Building Planner 0.2.0 was installed, 1.5 GB), `minecraft-backup-2026-11-15-pre-building-planner.tar.gz` (the world just before
Building Planner 0.1.0 was installed, 1.5 GB), `minecraft-backup-2026-11-14-pre-fabric.tar.gz` (the world just before
the switch to Fabric, 1.5 GB), `minecraft-archive-2026-10-29-1.21.11.tar.gz` and `minecraft-backup-2025-12-12.tar.gz`.
To take Building Planner off the server, remove
`mods/building-planner-0.10.0.jar` and run
`systemctl restart minecraft.service`. To go back to 0.9.0 instead, put `/root/bp-0.10.0-stage/building-planner-0.9.0.jar`
in its place, `chown minecraft:minecraft` it, and restart.
The vanilla `server.jar` stays in place: to switch back, change the jar in `ExecStart` to `server.jar`, then run
`systemctl daemon-reload` and `systemctl restart minecraft.service`. The VPS has 30 GB of RAM (about 16 GB available) and
160 GB of free disk; it also hosts other services.

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
Planner 0.10.0 (downloaded from its release `building-planner-0.10.0`), and Effortless Building 4.3, served from `main`
at https://raw.githubusercontent.com/Geomitron/minecraft-server/main/pack/pack.toml, so a push to `main` changes what
players get at their next launch. The pack and the server both run Building Planner 0.10.0, protocol version 5, which
0.6.0, 0.7.0, 0.8.0 and 0.9.0 use too, so a game with any of them works with the server; a game with a pack older than 0.6.0 still joins, and
Building Planner does nothing in it. `instance/` is the Prism Launcher instance "Server
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
  (402 r2, comment c1), except that with the setting "Place real blocks" on, on servers that run Building Planner, the
  server places the whole shape, or the paste, from the player's inventory (1368 Q2 A, 1372 Q6 B; "Building Planner
  0.9.0" below).
- Randomized block mixes, such as cobblestone with some mossy cobblestone (402 r2, c1).
- Layout guides are shared with everyone on the server; client-only guides like MiniHUD's are rejected (402 r2, c2).
- No automation (block placers, breakers, quarries) for now (402 r2).
- The server runs under its own account, `minecraft`, locked down by systemd, rather than as root. Asked in 1420
  whether to, Geo answered: "this really isn't a concern; I trust all my players. But if there are no noticeable
  downsides to adding this security change, than sure, you can do it".
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
  adjust the experience of using the mod. Once I'm happy with it, we can publish a guide on how to use it."). Geo
  approved the section "Build Plans" for Building Planner 0.7.0 as drafted (578 revision 2, no edits). It goes to
  players with 0.7.0's release, so the guide never describes a version players don't have, with 0.7.0's texts
  in Geo's wording (1313).

From Geo's walkthrough (inbox request 608) and requests 669 to 709 after it:

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
  "Use mix" leaves the chat message "Block mix on. Press B to edit or clear it."
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
- Geo's walkthrough of 0.3.0 on the server (752) settles these:
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
  - **The block mix:** a click on a block in the search results adds or removes it; the panel shows "B: Edit mix"
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

  Geo saw the mouse modes, the box and the list under the crosshair in the game (774) and kept the controls line
  where Effortless Building draws its own, though a chat message covers its left end for about 10 seconds (Q1 A).
- Geo's walkthrough of 0.4.0 on the server (784) settles these for 0.5.0. Geo left every feedback box empty but F11,
  and gave this feedback in comments and a note:
  - **Litematica's area selection:** it still showed ("I'm still seeing the old selection box", c1, image 1), with a
    one-block box with orange edges that Geo couldn't remove (c2, image 2). Both are the selection, and its origin,
    that Litematica saved for the server on Geo's computer. 0.4.0 turns the drawing off only where the record 0.3.0
    writes is missing, so on Geo's computer it never did. Each setting Building Planner changes once gets its own record, so 766 Q7 A
    reaches players who ran 0.3.0 or 0.4.0.
  - **The material list:** "In the material list UI, I want to customize it to just have a specific set of controls. I
    want Info HUD to show a similar pinned visual box like the block mix box, but with the list of blocks, along with
    total, available, and missing, sorted just like the full view is, with the multiplier the full view has. We can
    remove the "Litematica menu", export, and "write to file" buttons, and instead replace them with some simple custom
    controls to save and load material lists, which should be global per player and persistent across game closes.
    There's also an autosave material list for the last one collected (not necessarily the last one loaded)" (c3).
  - **M:** "M should be default bound to freecam's menu, not litematica. We shouldn't need to be able to access
    Litematica's menu" (c3). Litematica's "Tool Mode" button is at the bottom of its menu (F11).

  Geo's answers on the material lists (786) settle what c3 left:
  - **Where lists are kept:** on the server, for each player (Q1 B), as Geo read the option: "the lists are a file on
    the server, which the server sends only to the player who saved them, and whoever runs the server can read. They
    follow the player to another computer, on this server only. That needs new messages between the game and the
    server, a server update with the release, and a player whose game doesn't match the server's version of Building
    Planner sees no lists."
  - **Saved lists:** as many as the player saves (Q2 A), each under a name the player types, with the date and time
    filled in to start (Q3 A).
  - **Reopening a list:** a sixth radial menu button, "Material lists" (Q4 A), showing Minecraft's own Book and Quill
    (Q5 A).
  - **The panel:** in the top-left corner (Q6 A), each row the block's icon and its three counts (Q7 A), counting the
    box again every 2 seconds while it shows its list (Q8 A), and staying on with the same list after a restart, until
    the player turns it off (Q9 A).
  - **A game on another version:** Geo left it to the agent ("Doesn't matter; no users will have that old version
    installed, as it has not been distributed yet", 788), so the simplest stands: protocol version 4, and a game on
    another version than the server's has Building Planner off, with no message.
  - **The M key:** M opens Freecam's menu, as its screenshot showed (790 Q1 A).
  - **The save and load screens:** as their screenshots showed, with their texts as written (804 Q1 A).
  - **The panel and its button:** as their screenshots showed (807 Q3 A), with the panel's column headings "All",
    "Hold" and "Need" (807 PR1, Geo's wording). The panel shows only the rows that fit above the hotbar and counts the
    rest in "+N more" (Q1 B). Only drawing a box or pressing Refresh replaces "Last counted"; the panel's recount
    updates only the panel and the screen (Q2 B).
- Building Planner 0.6.0 takes the six changes Geo asked for on November 20th: delete limits the same as the copy
  box's, a ghost block removed once the real block fills it (those already filled too), editing ghost blocks from
  Freecam as from the player, every feature working in Freecam, an older shape's ghost blocks not disappearing under a
  new shape, and the copy placed with Satisfactory's controls. Settled:
  - **The copy's placement (806):** against the face aimed at, with its middle under the crosshair (Q1 B); a notch up
    turns it clockwise (Q2 B); it snaps flush to the player's recent pastes, and holding Left Ctrl keeps it in line
    with the last paste (Q3 C); the middle click still mirrors it (Q4 A); H locks it, the arrow keys move it a block
    forward, back, left or right, and Page Up and Page Down move it up or down (Q5 A); R switches between Single, Zoop
    and Vertical (Q6 A); its box is blue when it can be pasted, yellow when it would replace ghost blocks, and red when
    the server would refuse it (Q7 B). While Left Ctrl is held, a copy narrower or wider than the paste sits flush
    with the paste's nearer edge (890 Q1 A), and it follows the aim along the line, onto the paste too (890 Q2 A).
    Each copy in a Zoop or Vertical row counts as a paste of its own, so a later copy snaps to the copy aimed at
    (961 Q1 A). The box is drawn as an outline with see-through sides in its colour (962 Q1 B). Locking and nudging
    (954 A), Zoop and Vertical (961 Q2 A) and the copy's colours (962 Q2 A) go in 0.6.0 as their screenshots showed.
  - **Deleting (808):** a Delete button in the radial menu draws a box the way Copy does, then deletes every ghost
    block in it (Q1 A). Its icon is a Minecraft item, as "Material lists" uses the Book and Quill (Geo); the agent
    picked the Barrier, which Geo sees in the build's screenshots.
  - **Filled ghost blocks (809):** a real block fills its ghost block when it's the same block placed the same way
    (facing, axis, top or bottom half, slab type and door hinge), whatever it connects to (Q1 C), and any change to
    the real block counts: placing by hand, commands such as /fill, pistons, flowing water and growing plants (Q2 A).
    A removed ghost block doesn't come back if the real block is broken later. A block placed several to a spot
    (candles, sea pickles, snow layers, turtle eggs, pink petals) fills its ghost block only at the same count (823
    Q1 B); wildflowers and leaf litter, which also go several to a spot, follow the same rule. Drawing a ghost block where the real block already fills it removes any older ghost block there, and undo
    brings the older one back (823 Q2 B).
  - **Overlapping shapes (811):** where a new shape covers older ghost blocks, the new shape's blocks stay (Q4 A).
    Geo saw most or all of the older shape disappear (Q1 C), which that rule doesn't explain: a bug in 0.5.0 that the
    test game hasn't reproduced, so 0.6.0 logs a warning in the game when it happens again.
    Both shapes were drawn in the same session, often floors about 20 blocks long, and Geo thinks the older shape came
    back once the new one was removed (811 revision 2).
  - **Settled by the agent:** editing in Freecam aims from the camera whenever Freecam's camera is on, since Geo asked
    to edit ghost blocks there "like I would as the player"; ghost blocks filled before 0.6.0 are found as each chunk
    loads; a filled ghost block goes without a glow, since Litematica already draws it as nothing.
  - **Texts (972):** the controls lines, the key names and the Delete texts as Geo read them, with Geo's wording of the
    locked line, whose phrase "Arrow keys + Page Up/Down to nudge" every locked line uses (PR1 c3, and revision 2); a
    dark background behind the line always, as chat has (Q1 B); the controls lines go in 0.6.0 (Q2 A). Every text the
    game shows uses the fewest words that stay clear, and one name for each action, as the spec states with the controls
    lines (985 Q1 A).
  - **Two more changes (972):** Zoop and the copy's other controls for Effortless Building's Single mode, as if one
    ghost block were a copy of one block, and a second Delete mode like Satisfactory's dismantle, where the ghost
    blocks the player looks at are marked and a confirm erases them (Geo's note on 972). Both go in 0.6.0, and its
    release waits for them (972 revision 2, Q1 B).
  - **One ghost block (980):** Single mode keeps its buttons: a right click places, a left click erases, and holding
    either repeats (Q1 A). R gives it Zoop and Vertical rows of up to 32 blocks (Q2, Geo's answer), in a mode of its
    own, apart from the copy's (Q3 A). H locks it, and the arrow keys and Page Up and Page Down nudge it (Q4 A). The
    wheel and the middle click work as in Minecraft, and Left Ctrl keeps nothing in line; while Left Ctrl is held, the
    wheel turns the block and the middle click steps through the other ways that block can sit, so the player can place
    it facing any way wherever they stand (Q5 and Q6, Geo's answers). Blocks keep the turn until the player switches to
    another block (revision 2, Q1 A). The copy's blue, yellow or red box shows only while the block is locked or a row
    is fixed (Q7 A), and Single mode's controls line shows whenever a block is in hand (Q8 A). Building Planner always
    draws the unlocked block at the aim in Single, turned as it will be placed (1108 Q1 A), and in Zoop and Vertical
    before their first click (1128 Q1 A). A locked block's left
    click erases the ghost block where it's locked (1009 Q1 A), and with Effortless Building's mirror, radial mirror
    or array on, every block a right click would place (1074 Q1 A). Left Ctrl turns and flips the block in every one of
    Effortless Building's modes, not only Single, and every mode's shapes take the turn (1009 Q2 C). In Disable, where
    the player places real blocks, Left Ctrl turns and flips the real blocks they place only while they hold Left
    Ctrl, with a preview of the turned block (1029 Q1 A), and 0.6.0 waits for it (1029 Q2 B). A turned block that
    can't stay places nothing, and the preview shows red with "Can't place here" (1033 Q1 A). Before Left Ctrl is
    held, Disable's line says "Hold Left Control to turn or flip", as in the other modes (1033 Q2 A), "but only for
    block types where this is useful" (Geo's comment): not for blocks that hang on the block behind them, such as
    ladders and torches on a wall (1037 Q1 B).
  - **Delete by aim (981):** R switches Delete between "Box" and "Aim", as it switches the copy's modes (Q1 A). Holding
    Left Ctrl and sweeping the aim marks each ghost block the crosshair passes over, and holding the left button for a
    second erases the marked ones (Q2 A, Q3 A). A sweep that starts on a marked block unmarks what it passes over,
    until the player lets go (Q4 A). Up to 200,000 can be marked, the server's limit for one edit, as for the Delete
    box (Q5 A). Each marked ghost block has a red outline and see-through red sides, with their count and kinds under
    the crosshair (Q6 A); together they show as one red shape, outlined at its outer edges (1060 Q1 A). The mode
    stays on after an erase (Q7 A).
  - **Controls lines (1155):** every controls line shows each control as "key: action", as Geo wrote the Delete
    box's line: "Delete: [Left-click: set the first corner][R: Delete mode][Right-click: cancel]". Geo's comment: "I
    want all control hints to be formatted like this. If possible, instead of brackets separating them, they should be
    different adjacent text rectangles across the bottom above the hotbar, with a gap between each one." Geo approved
    the eight keys' new names and "Too many blocks" as proposed, and then every controls line in that form as
    revision 2 showed it: each control its own rectangle, the label its own first rectangle, a reason its own red
    rectangle, a box's size its own last rectangle, and "R: Delete mode" in Delete and "R: Zoop mode" in the copy's and the one
    ghost block's lines (1155 revision 2). Effortless Building's own line during a shape is drawn in the same
    rectangles: "[Left-click: cancel][Right-click: place]" (1155 revision 2, Q1 A). Geo approved its two other forms
    and releasing 0.6.0 with them: "[Left-click: cancel][Right-click: interact]" with a tool in hand, and "[Left-click:
    break][Right-click: cancel]" with the actions in red for a shape that breaks blocks (1185 A).
- **Building Planner 0.7.0** takes the changes Geo asked for in the walkthrough of 0.6.0 (815 revision 2, comments
  c2 to c19). Their design settles how each works and asks Geo what's still open:
  - the radial menu: show/hide below the wrench, Copy, Paste and Delete in the row above the wrench, and "Block mix",
    "Material list" and "Material lists" in the row above that (c2);
  - a ghost block shown red because a different block fills it is seen faintly through walls from nearby (c3);
  - every build mode shows the block a right click would place before the first click, as Single does (c4), and
    H locks and nudges that first position, or during a shape every position set so far, together; everything else
    works as usual, and the lock ends once the blocks are placed (c5);
  - each key that switches a state names the current state, as "R: Zoop mode (Off)" (c6);
  - Geo proposes removing Zoop from the one ghost block, since Line, Wall, Floor and Cube overlap it, and asks which
    part of Zoop those modes can't do easily (c6); the copy keeps it, as "Mode" (Off, Line, Wall, Floor), placing
    copies as those modes do (c12, c15);
  - the key that switches the one ghost block's Zoop stays available while a row's length is adjusted, switching
    between a Vertical row and none (c7); the design applies this to the copy's mode, which keeps a row;
  - the first corner of a box (Copy, "Material list", Delete), placed looking down on a block, darkens that block's top
    face, and the box is red for
    Delete, white for Copy and blue for "Material list" (c8);
  - a setting that, when on, places a block into a ghost block of the same kind turned as the ghost block is (c9);
  - Esc steps out of one mode a press at a time (a copy's row, then the copy, then a build mode back to "Disable")
    before it pauses, and the lines say "Esc/Right-click: close" (c10, c13, c17);
  - the copy pastes with a right click, as blocks are placed (c14);
  - Delete becomes one mode: Left Control with a held left click draws a box that marks what's in it, or unmarks it
    when it starts on a marked block, and holding the left click alone erases the marks, with a progress bar in the
    middle of the screen above the controls line (c16);
  - the held item's name shows where the build mode's name is, the build mode's name stays beside the hotbar while a
    build mode is on, and every build mode shows its left and right click controls before the first click (c17);
  - the blocks listed under the crosshair while a box is drawn sit on the controls line's dark background (c11,
    c18);
  - the material list screen's (i) text covers only Building Planner, and the list screens get a "Close" button in
    their bottom right corner (c8, c19).

  Geo's answers to the questions the design left open:
  - Single loses Zoop and Vertical, so rows of single blocks are drawn with Line, and R does nothing in Single (1238
    Q1 A);
  - while the copy is at the aim, a left click puts it away, or drops a row of copies, as Esc does, so its line reads
    "[Esc/Left-click: close]" in place of c13's "Esc/Right-click: close" (1238 Q2 A);
  - after Esc drops a row, the copy stays in Line, Wall or Floor, ready for a new row (1238 Q3 A);
  - the copy's Line, Wall and Floor lay up to 10 copies along each direction, so a Wall or a Floor has up to 100
    (1238 Q4 A);
  - any click that finishes a shape ends the lock, placing or erasing (1239 Q1 A);
  - a locked copy unlocks once it's pasted and follows the aim again (1239 Q2 A);
  - in Delete, holding Left Control still marks each ghost block the crosshair passes over, alongside the box, but
    "if the user is actively drawing the delete box, it does *not* mark ghost blocks that the crosshair passes over"
    (1240 Q1 A, with Geo's comment);
  - red ghost blocks, and orange ones (the right block in another state), show faintly through walls within 32 blocks
    (1241 Q2 C, Q3 B);
  - "Match ghost blocks" is a row in Effortless Building's "Visual Settings" screen, which its radial menu opens (1244
    Q1 B), so the radial menu has no settings button of Building Planner's. Geo had answered "I meant the game options
    for effortless building, which you have to get to through the pause or main menu's settings button" (1241 Q1,
    Other); "Visual Settings" is Effortless Building's only settings screen, and only its radial menu and Mod Menu,
    which the pack doesn't have, open it.

  Geo's answers on 0.7.0's texts and screenshots (1313): every text as proposed, except as below.
  - "Match ghost blocks" turns a placed block only while ghost blocks show (1313 Q1 A), and its tooltip reads
    "Automatically flips and turns a block you place to match a ghost block you're filling in" (1313, Geo's wording);
  - the wrong blocks' outline through a wall is drawn at 80% opacity (1313 Q2 B);
  - the controls lines show no status as a label: "we don't need these statuses, like "Turned:" and "Locked:", those
    should either be evident from how the preview looks or is indicated on the control hint itself, like "Lock (On)""
    (1313 [c1]). The copy's mode as a label ("Line:", "Wall, locked:") goes too, since "[R: Mode (Line)]" shows it
    (1313 revision 2, approved as shown);
  - the tool's name leaves the lines for Copy's box, the material list's box and Delete, and shows left of the
    hotbar instead: "I thought these should be on the left side of the hotbar, like the names for the placement
    modes?" (1313 revision 2, Q3). While the box is drawn or Delete is on, "Copy", "Material list" or "Delete" shows
    where the build mode's name shows, in its place, and in Disable too; the build mode's name comes back after.
    While the copy is at the aim, "Paste" shows there the same way: "why does this say "Single"? If this text is
    supposed to be the currently selected action, shouldn't it be "Paste"?" (1335 [c1]).
  Geo approved the release of 0.7.0 with the guide's two changes, "faintly" out of the line on red and orange blocks
  through walls and "while ghost blocks show" in the "Match ghost blocks" sentence (1335 Q1 A), and then with "Paste"
  beside the hotbar, from its screenshots (1335 revision 2, Q1 A: "Release it.").
- **Building Planner 0.8.0** takes the changes Geo asked for in the trial of 0.7.0 (1359, comments c1 to c7; c5 asks
  for nothing: "ignore that, I forgot to delete the comment", 1363). Their design settles how each works and asks Geo
  what's still open:
  - the two lines Geo saw in a corner of the screen, "Layer Mode: All" and "Renderers: A: ON S: ON B: ON O: ON SEL:
    OFF", are Litematica's status panel: Litematica shows it for 10 seconds each time it adds a placement while one of
    its drawing switches is off, and Building Planner turned off "SEL" (area selections) and adds a placement for each
    column of ghost blocks, so it shows often. Geo likely wants it gone (c1); Building Planner turns off
    Litematica's option that shows it, once on each computer;
  - every ghost block within 32 blocks shows through walls, as red and orange ones do, in light blue where its block
    is missing (c2);
  - H locks the step under way: before the first click the first position, and after each click the position the aim
    sets for the next, such as a wall's length and height or a cube's height. The nudge keys then move that step's
    position, or move the whole shape along a direction the step doesn't set, such as across a wall (c3), since
    Effortless Building reads a wall's far corner moved across it as a turn. This replaces 0.7.0's lock during a
    shape, which held every position set so far and nudged them together (c5 on 815 revision 2);
  - "Visual Settings" is renamed "Settings", the screen and the radial menu's button (c4);
  - Geo wants a setting "Place real blocks", weighed against Building Planner working from a player's computer alone
    on other servers, where other players don't see that player's ghost blocks (c4). From the computer alone, a mod
    can place a real block only as a player does, one right click a block within about 5 blocks, as Litematica's Easy
    Place does; a server part can place a whole shape at once, as Effortless Building's own server does;
  - Delete's controls line says "toggle mark" where it says "mark" (c6);
  - before the first click in the build modes, the block a left click would start from shows framed in red, as a shape
    drawn with the left click shows it, beside the block a right click would place (c7);
  - "Build speed" and "Break speed" move to the radial menu's right side as headed sections, where Cube's "Filling"
    shows, and show only while Single is the mode (c7).

  Geo's answer: Litematica's status panel is hidden: "Building Planner turns off Litematica's option that shows the
  panel, once on each player's computer" (1367 Q1 A).

  0.8.0 builds neither of Geo's answers on real blocks and other servers (1368), which "Building Planner 0.9.0"
  below records.

  In Single, the red frame shows on ghost blocks only, and in the other modes on any aimed block: "Yes, but on ghost
  blocks only, since single does erase ghost blocks" (1369 Q1).

  Geo approved 0.8.0's texts and the player guide's sentences as proposed, with no edits (1391), and for the red frame
  on a ghost block picked "Draw it more strongly on ghost blocks, so it reads as red." (1391 Q1 B).
- **Building Planner 0.9.0** adds the setting "Place real blocks", which Geo asked for in the trial of 0.7.0 (1359,
  c4): "I also want a setting for "place real blocks". However, I have ideals for this being a client-side only mod
  that could be used on other servers that don't need other people to be able to see your own ghost blocks. And in
  cases like that, I'm not sure if it's still possible to implement a "place real blocks" feature, so that would have
  to be weighed against that ideal, if it's possible or not." Asked in 1368, Geo picked:
  - for "Should Building Planner also work on servers that don't run it?", "Yes, in a later version: there, your ghost
    blocks are saved on your computer and only you see them, and this server still shares them." (Q1 A). That version
    comes after 0.9.0, which designs nothing that blocks it;
  - for "Should shapes be able to place real blocks, with a setting "Place real blocks" in "Settings"?", "Yes, on
    servers that run Building Planner: the server places the whole shape." (Q2 A). This changes Geo's rule that
    Effortless Building's shapes, mirror, repeat and Randomizer edit only Litematica ghost plans, never real blocks
    (402 r2, c1): while the setting is on, the place click's shape places real blocks.

  Geo's answers to the request "Place real blocks: open choices" (1372):
  - in survival, a shape's real blocks come from the inventory, all or nothing: "Your inventory, and nothing goes in
    unless it has every block the shape needs." (1372 Q1 C);
  - a shape places real blocks within 64 blocks of the player: "Up to 64 blocks." (1372 Q2 A);
  - a real block in a shape's space stays: "It stays; the shape fills only spaces a right click could place into (air,
    water, lava, grass, or a snow layer)." (1372 Q3 A);
  - with the setting on, the break (left) click's shape erases only ghost blocks: "Erase ghost blocks only, as with
    the setting off." (1372 Q4 A);
  - Ctrl+Z doesn't undo real blocks: "No: Ctrl+Z undoes only ghost blocks, and real blocks come out by hand." (1372
    Q5 B);
  - with the setting on, a paste places real blocks: "Place real blocks from your inventory, by the same rules as a
    shape." (1372 Q6 B).

  The design ("Place real blocks" in [building-planner.md](building-planner.md)) settles how it works within Geo's
  answers:
  - the setting is a row of "Settings" after "Match ghost blocks", off at first and saved with the screen's "Save"; it
    acts on a server whose Building Planner takes the new payload `place`, and 0.9.0 keeps protocol version 5, so a
    0.8.0 game and a 0.9.0 server work together;
  - while it acts, the place click's shape in every build mode but Disable places real blocks, with the mirror, the
    radial mirror, the array and the block mix, each block turned as the preview shows it, and with "Match ghost
    blocks" on, each block that fills a ghost block of its own block turned as that ghost block is; Single places one
    real block a click, repeated at the build speed while the button is held;
  - a paste places the copy's blocks as real blocks, and a Line, Wall or Floor of copies places the whole set at its
    second click, by the same rules as a shape;
  - the server places each block as the player's own right click would place it there, through Minecraft's
    `BlockItem.place`, with a right click's checks (a game mode that builds, a loaded chunk, the build height, the
    world border, spawn protection, a block that can stay, no entity in its way, operators' blocks only for
    operators), from the lowest level up, giving a block whose support the same shape or paste places one more try
    after the rest;
  - in survival, nothing goes in unless the inventory contains an item for every block that passes those checks: every
    block of the mirror's and the array's copies counts, each block of the mix counts as its own item, and a block
    made of several items, such as a double slab, counts as that many; blocks left out for their space or the
    reach need none, and when the inventory falls short, a notice names the items short;
  - at most 2,000 real blocks a shape or a paste, and a second, for each player (Effortless Building's own cap on a
    survival player's shape), fixed rather than a server setting so that the game can draw a copy past it red; and
    one place sound a shape or a paste;
  - the preview draws red each block the player's game can tell won't go in, and the whole shape when the inventory
    doesn't cover it; Effortless Building's own icons beside the crosshair count the blocks the click uses; and the
    controls lines read the same with the setting on as with it off (1411 [c2], [c3], below);
  - the copy's box is blue where every block would go in, yellow where some wouldn't (a real block in the space, past
    the reach, outside the world, or a block no item places, such as water), and red where nothing would (over 2,000
    blocks, or an inventory that doesn't cover it), with the reason on the controls line and a click that pastes
    nothing;
  - a shape or a paste of real blocks makes no undo step, so Ctrl+Z right after one undoes the player's newest change
    to ghost blocks, made before it;
  - a real block that fills a ghost block takes it out of the layer, as any real block does.

  Asked in 1408, Geo picked, for a shape drawn with torches (or signs, banners, heads or coral fans) aimed at the side
  of a wall, "Nothing, as now." (Q1 C): each block keeps Effortless Building's standing form, which can't stay beside
  a wall, so nothing goes in; and for a block another player puts in a spot between the preview and the click, "No:
  it's skipped silently, like the spots the preview already showed red." (Q2 A).

  Geo's answers on 0.9.0's texts and screenshots (1411):
  - in a shape aimed at the side of a wall, heads go in standing upright, "Yes, as the preview shows them." (Q1 A);
  - a block that another block of the same shape holds up, such as scaffolding in a Line up from the ground, "Show it
    as going in." (Q2 B), with Geo's comment: "Can't you do this, but only narrow that check if the placement involves
    scaffolding and any of the other rare blocks that this might apply to?";
  - the control hints stay as they are with the setting on: "Again I don't want the control hints to change based on
    the place real blocks setting" (1411 [c2], on "[Right-click: paste real blocks]") and "This is not necessary;
    don't change the control hints for this" (1411 [c3], on the Line's controls line before its first click);
  - Geo asked for a box listing the items the action needs (1411 [c1]): "I think if we're about to place something, we
    can show a box like that to indicate how many blocks the action will require, and compare that to the number of
    each that you currently have. This is basically like a materials list, but just for this particular action."
    Asked in 1411 revision 2, Geo picked, for when it shows, "In survival, from a shape's first click until it's
    placed, and while a copy is at your aim." (Q1 A); for what it's like, "The material list panel: each item's icon,
    how many the click uses, how many you have, and how many are missing, in red." (Q2 A); for chat after a click that
    places nothing because the inventory is short, "Only that something is missing, for one item or several:
    "Nothing was placed (not enough blocks)"" (Q3 B, which replaces Geo's earlier wording "Nothing was placed
    (missing 1 oak planks)"); and for "[Can't paste: not enough blocks]" on a copy that's red for being short, "Yes,
    as "[Can't paste: too many blocks]" shows for a copy over the limit." (Q4 A). Asked in 1411 revision 3, Geo
    picked, for where the panel goes, "The top-right corner, below the effect icons and Litematica's lines about the
    ghost block at your crosshair while they show." (Q1 A); for its rows, "At most 10, as on the material list
    panel." (Q2 A); for which rows show, "The items you're short of first, then the rest, each most used first."
    (Q3 A); and for Effortless Building's icons beside the crosshair while the panel shows, "No, since the panel
    shows the same counts." (Q4 A). The panel's title is "Required blocks" (Geo's wording) and its headings "Uses",
    "Hold" and "Need" (1411 revision 3, PR11);
  - Geo approved, as proposed in 1411 revision 2, the other notices "Nothing was placed (2,001 blocks, over the limit
    of 2,000 at once)", "Nothing was placed (over the limit of 2,000 blocks a second)", "1 block wasn't placed (not
    allowed there)" and "7 blocks weren't placed (not allowed there)", the guide's paragraph on items without the
    screen's details, and its paragraph on pastes without "with the reason above the hotbar";
  - Geo read the setting's label and tooltip ("Place real blocks"; "Shapes and pastes place real blocks from your
    inventory, not ghost blocks"), the player guide's sentences on turning it on, on where blocks go, on red in the
    preview, on torches and signs, on undo, and the first sentence on pastes, with no edit or comment (1411 PR1, PR2,
    PR5 c2, PR6, PR8, PR10 c1).
- **Building Planner 0.10.0** works on servers that don't run Building Planner, as Geo asked in the trial of 0.7.0
  (1359, c4): "I have ideals for this being a client-side only mod that could be used on other servers that don't need
  other people to be able to see your own ghost blocks." Asked in 1368, Geo picked:
  - for "Should Building Planner also work on servers that don't run it?", "Yes, in a later version: there, your ghost
    blocks are saved on your computer and only you see them, and this server still shares them." (Q1 A). This changes
    the rule that Building Planner turns off silently on a server without it (483), which still holds for a server
    with another version of Building Planner;
  - for "Place real blocks", "Yes, on servers that run Building Planner: the server places the whole shape." (Q2 A), so
    on other servers shapes draw ghost blocks only.

  Asked in 1374 ("Building Planner on other servers"), Geo picked:
  - for what tells a player that only they see their ghost blocks, "A line above "Building Planner" in the radial
    menu's corner" (Q1 C);
  - for saved material lists, "Yes, saved on the player's computer for that server" (Q2 A);
  - for Disable's turn with Left Ctrl and "Match ghost blocks", "I'd prefer it, but not if you need some weird hack to
    make it work that might get detected as a problem by servers" (Q3). The only way is for the game to send the
    server a turned view just before each such click, as "printer" mods do, and anti-cheat plugins flag those turns,
    so real blocks place there as in Minecraft;
  - for singleplayer worlds and worlds opened to LAN, "Yes, with ghost blocks shared with the world's LAN guests"
    (Q4 C), with the comment "Really depends, if C is a ton of work, then B would be fine." C takes one build step, as
    B would: `BuildingPlanner.onInitialize` registers the server part in the game too, and the game drops the
    exceptions it makes for its own world, so C is the one built.

  Asked in 1375 ("Ghost blocks between servers"), Geo picked "Yes, the copy stays while the game runs, across servers
  and dimensions" (Q1 A) and, for a server that starts running Building Planner, "They stay on each computer, unused,
  and show again if the server stops running Building Planner" (Q2 A).

  The design ("Servers without Building Planner" in [building-planner.md](building-planner.md)) settles how it works
  within Geo's answers:
  - on a server that takes none of Building Planner's payloads, a server running Effortless Building's own server
    part among them, the game starts a local session: it runs the server's own classes for the layer, undo and the lists
    (`LayerSync`, `LayerStore`, `MaterialLists`) for that player alone; a server that takes some of them waits for
    `hello`, and a `hello` during a local session replaces it; a LAN guest's game starts one in a world whose host's
    game doesn't run Building Planner;
  - the player's ghost blocks for each server are files in the game's folder, under the name Litematica gives that
    server's folders, read at joining and written at most every 30 seconds, at leaving and as the game closes;
  - every feature works there as on this server, except that no one else sees the ghost blocks; undo lasts until the
    game closes; a ghost block a real block fills goes only where the game sees the change or loads the chunk, and
    a placement the server refuses leaves it; "Place real blocks", Disable's turn and "Match ghost blocks" don't act;
    and the removal of the old plans' placements doesn't run;
  - Litematica's colour for extra blocks stays hidden in the player's own placements, as in 0.9.0 (1453 Q2 B);
  - during a local session, the radial menu shows a line above "Building Planner" in its corner, right-aligned as the
    name is and in the grey of the menu's mode descriptions, and a row above a hovered mode's description on a screen
    up to 256 high in the GUI's units, where the description would cross it;
  - in the player's own world, singleplayer or opened to LAN, the game runs Building Planner's server part as this
    server does: the ghost blocks and lists are saved in the world's folder, the cap is the default 200,000 blocks
    an edit without a config file, LAN guests with Building Planner share the ghost blocks, Effortless Building's
    shapes draw ghost blocks and "Place real blocks" places real ones by this server's rules, the import and the
    removal of the old plans' placements don't run, and a world whose layer files can't be listed opens with
    Building Planner off;
  - 0.10.0 changes nothing this server does and keeps protocol version 5.

  Asked in 1381 ("Building Planner 0.10.0: the radial line and your own worlds"), Geo approved the designs for
  1374 Q1 C and Q4 C: the radial menu's line in the grey of the mode descriptions (Q1 A), with the comment that it
  "should say "(client only)" instead" of the proposed "Only you see your ghost blocks here" (c1); the player's own
  worlds as designed (Q2 A); and there, "Up to 2,000, within 64 blocks of you, as on this server" (Q3 A).

  Asked in 1453 ("Building Planner 0.10.0: the texts for other servers"), Geo approved the radial menu's line
  "(client only)" and the guide's sections "Other Servers" and "Your Own Worlds" and its paragraph on the copy as
  proposed; leaving a server or changing dimension puts the copy away, and "Paste" in the radial menu brings it back
  (Q1 A); and for Litematica's magenta marks of extra blocks in the player's own placements, "No, keep them hidden, as
  in 0.9.0" (Q2 B).

  Asked in 1461 ("Release Building Planner 0.10.0"), Geo approved the release (Q1 A) and the server taking 0.10.0
  too (Q2 A: "Yes, update the server too").

- **Saved copies** is a feature Geo asked for in a comment on 1375 Q1: "I'd like to add a feature to save previously
  copied regions, like saved materials lists. Instead of a button next to the radial menu, you would access it as a
  new keyboard press that's displayed while you're pasting. It would have a menu like the saved materials lists.
  Copies would save automatically, with the most recent 10, but you can view any of those copies in that menu and
  rename it, which saves it until you delete it."

  The design ("Saved copies" in [building-planner.md](building-planner.md)), proposed for 0.11.0, settles how it works
  within Geo's words and answers:
  - each copy Copy makes is saved at once, named by its date and time; the 10 most recent copies not renamed stay, and
    a renamed copy stays until deleted, without counting among the 10 (Q3);
  - the copies are kept on the player's computer, a set for each account, the same on every server, so 0.11.0 changes
    nothing the server does and keeps protocol version 5 (Q1);
  - the key "Saved copies" (V) opens the screen whenever no screen is open, and the copy's controls line shows it as
    "[V: saved copies]" (Q2);
  - the screen "Saved copies", laid out as "Saved lists", lists them newest first, each with its name, under it its
    size and block count, and "Paste", "Rename" and "Delete" (Q4); "Rename" opens a screen laid out as "Save list", and
    saving it makes the copy stay until it's deleted;
  - with no copy since the game started, the radial menu's Paste button brings back the newest saved copy (Q5).

  Asked in 1384 ("Saved copies"), Geo picked:
  - for where the copies are kept, "On your computer, the same on every server." (Q1 A);
  - for the key, "V." (Q2 A);
  - for how many stay, "No: every renamed copy stays, however many and however large, beside the 10 most recent
    others." (Q3 A);
  - for what a row shows, "No picture: the name, size, and number of blocks only." (Q4 A), so the design takes no
    picture of the player's view;
  - for a restart, "Bring back the newest saved copy." (Q5 A).

  Asked in 1464 ("Building Planner 0.11.0: the texts for saved copies"), Geo approved every text and the guide's
  sentences as proposed (commas in block counts, "That copy couldn't be read, so it was removed.", a "Keys" row for V,
  the section "Saved Copies" and "Copy and Paste"'s paragraph), and, for a picked copy while you aim at no block, "On
  your level, starting two blocks in front of you." (Q1 A).

  Asked in 1473 ("Release Building Planner 0.11.0"), Geo approved the release (Q1 A) and the server taking 0.11.0
  too (Q2 A: "Yes, update the server too").
- The shared layer replaces the decisions above that concern separate plans: named plans that move and turn as a whole
  (411 Q1), plan growth (411 Q2), who deletes a plan (411 Q3, 420 Q9), starting a plan with N (420 Q1, 469 Q1), hiding
  a plan with Litematica or Syncmatica (420 Q2, 469 Q2), the paste's target plan (485 Q3), Litematica's Edit Schematic
  actions syncing live (411 Q5), and Syncmatica on the server and in the pack (408 Q2).

## Agent-ready work

**Building Planner 0.8.0** (the bullet "Building Planner 0.8.0" in "Decisions" above: Geo's comments c1 to c7 on
1359). [building-planner.md](building-planner.md) is the design; each choice still Geo's carries an `OPEN:` mark
naming the request that asks it. 0.8.0 keeps protocol version 5 and changes nothing on the server.

The steps come in this order, each small enough for one child. Step 2 may run beside any step but 6; the others change
`step9.sh`, `step10.sh`, `filled.sh` or the language file, so they run one at a time. A step that changes what players
see is committed without pushing, and goes to `main` once Geo has seen its in-game screenshots, with the changes
outlined in red, in step 7. Each step rewrites the spec's "In-game checks" and its unit tests' paragraphs for what it
changes, removing the checks of behavior it removes, and runs the unit tests and the in-game scripts it names; the full
set of in-game scripts runs once, on step 7's last build ("Thread Procedures" in the project's index). Screenshots are
at 854 by 480 and 1920 by 1080, GUI scale 2, plus GUI scale 4 where a step says so.

1. **Litematica's status panel off** ("Litematica's status panel" and "Settings Building Planner changes once"; c1).
   Can start now (1367 Q1 A). The record
   `status-panel-off-applied` turning off Litematica's `statusInfoHudAuto` once. Done when `LookDefaultsTest` covers
   the option turned off from its default, left as the player set it, the record written once, and no record when
   saving fails; and `step9.sh`'s once-settings checks pass: after a start, Litematica's config holds
   `statusInfoHudAuto` false and Building Planner's file the record; with the record there and the option turned back
   on, a restart leaves it on; and for 15 seconds after Planner1 joins beside ghost blocks, Litematica's status panel
   never draws (`StatusInfoRenderer.shouldRenderStatusInfoHud()` false each tick). Shot of the screen 5 seconds after
   joining, with the corner where Geo saw the panel.
2. **Ghost blocks through walls** ("Ghost blocks through walls"; c2). Can start now. `WrongBlocks` and
   `WrongBlocksOutline` renamed `ThroughWalls` and `ThroughWallsOutline`, light blue for a ghost block over air or a
   fluid, and only the changed sections rebuilt. Done when `ThroughWallsTest` (from `WrongBlocksTest`) covers light
   blue over air, water and lava, red, orange, nothing over the same block, the range, and the sections that changed
   between two scans; and `filled.sh`'s checks pass: a ghost stone brick over air 4 blocks behind the wall draws light
   blue from 10 blocks and nothing from 33, beside the red and orange ones, and erasing it takes it out within half a
   second. The driver's `wrongscan` also times building the shapes whole and again after a 1-block edit, with a ghost
   block at every position within 32 blocks, to the checks' log and the spec's "Unverified". Shots from both sides of
   the wall, with light blue, red and orange.
3. **A lock for each step** ("Locking and the first click": "Locking and nudging", "Colours" and "Controls line"; c3).
   Can start now. `ShapeLock` holding the step under way, hook E37, the directions each step sets, a nudge along
   another direction moving the whole shape through E33, the copy's box around the locked step's position, and the
   next step unlocked after each click. Done when `ShapeLockTest` covers H for each step, the directions each mode's
   steps set (Line one block long and longer, Wall one block wide and wider, `findFloor`'s and `findHeight`'s steps),
   a nudge in each, the click taking the locked position, the next step unlocked, and a cancel keeping a first
   position locked before the first click; `ControlsLineTest` covers "[H: Lock (Off)]" after each click; and
   `step9.sh`'s lock checks pass: in Wall, after the first click, H locks the far corner, which stays as Planner1 turns,
   Page Up makes the wall a block taller, a nudge across the wall moves the whole wall a block, and the placed wall is
   the one shown; in Cube, after the second click, H locks the height, Page Up raises it and a level nudge moves the
   whole cube; in Line, a nudge across a line of 5 moves the whole line; in Floor, Page Up moves the whole floor up.
   Shots of a locked Wall step before and after a nudge across it, and a Cube's locked height.
4. **The red frame for the left click** ("Locking and the first click": "The first click's blocks"; c7). Needs
   step 3 (Geo answered 1369). Hook E38 and its rule for where the frame shows (a pure
   function beside `ShapeLock`), with the mirror and array copies. Done when a unit test covers the rule for Geo's
   answer (Single on a ghost block and on a real block, the other modes on each, none while locked, in Disable, during
   a shape, with a mouse mode on or aiming at nothing), and `step9.sh` checks pass: what the last frame drew in red
   (recorded by E38 for the driver) is the aimed ghost block in Single, the aimed real block in Wall, and both mirror
   copies with the mirror on, and nothing once the first position is locked. Shots in Single on a ghost block and in
   Wall on a real block, with the stair a right click would place beside it.
5. **The speed sliders in Single** ("The radial menu's buttons and sliders": "The sliders"; c7). Can start now. The
   headings "Build speed" and "Break speed" and the two sliders on the menu's right side, the sliders showing only
   their values, shown only while Single is the mode. Done when `RepeatSpeedTest` covers the values' text, and the
   radial menu's checks in `step10.sh` and `step9.sh` pass: in Single, the headings and sliders are where Effortless
   Building puts a mode's first and second sections, a click and a drag set the speeds as before, and nothing
   overlaps at 854 by 480 and 1920 by 1080, GUI scales 2 and 4; in Cube and Disable no slider shows, and choosing Line
   in the open menu hides them. Shots of the menu in Single and in Cube (with GUI scale 4).
6. **"Settings" and "toggle mark"** ("Settings" and "Delete": "Controls line"; c4, c6). Can start now. Hook E39 with
   `buildingplanner.ui.settings`, and "toggle mark" in Delete's lines. Done when `ControlsLineTest` covers Delete's
   lines with "toggle mark", `step9.sh`'s Delete checks read each line exactly, and `filled.sh`'s settings checks pass
   with the screen's title "Settings" and the radial menu's button named "Settings" on hover. Shots of the screen, the
   button's hover text, and Delete's line with and without marks.
7. **Texts and screenshots for Geo**: every text steps 1 to 6 add or change (the headings and the sliders' values,
   "Settings", "toggle mark"), and the player guide's sentences on ghost blocks through walls, the radial menu's
   "Visual Settings" and sliders, locking and nudging, and the red frame, go to Geo as editable text, with every step's
   screenshots, the changes outlined in red, each text following the rule Geo approved for the game's texts (985 Q1 A;
   1313). Geo's version is built in, `ControlsLineTest`, `step9.sh` and `filled.sh` check each text exactly, and the
   full set of in-game scripts runs on the last build. The same step sets the version 0.8.0, and the release request
   shows the changed parts in the game, outlined in red.
8. **The release** (done, Geo's approval in 1403): the GitHub release `building-planner-0.8.0`, not latest, from the commit on
   which `overlap.sh`, `filled.sh` and `import.sh` passed, run by the server and the pack (see "Current setup"), with
   the guide's changed sentences in Geo's wording.

**Building Planner 0.9.0** (the bullet "Building Planner 0.9.0" in "Decisions" above: Geo's comment c4 on 1359,
1368 Q2 A and Geo's answers in 1372). "Place real blocks" in [building-planner.md](building-planner.md) is the design.
0.9.0 starts once 0.8.0 is released, and keeps protocol version 5.

The steps come in this order, each needing the one before it, and each small enough for one child. 0.8.0's rules for
commits, screenshots, the spec's "In-game checks" and the full set of in-game scripts apply; the full set runs once, on
step 5's last build. The in-game checks of real blocks are in `real.sh`, which step 2 adds.

1. **The rule for each block** ("Place real blocks": "How the server places each block", the checks before it, "The
   inventory" and "The cap"). `RealPlace` (common), the pure rule the game's preview, the copy's colour and the
   server's placing share: the order, the inventory and its all-or-nothing count, the reach, a space that takes a
   block, a block an item places, and the cap. Done when `RealPlaceTest` covers what the spec's unit tests' paragraph
   lists for it.
2. **The server places a shape** ("How the server places each block", "The cap", "One sound", "The notice",
   "Compatibility"). The payload `place`, `RealPlacer`, hook S2 giving each block the shape's orientation, form and
   count, hook S3, the cap on a shape and on a second, and the notices. Done when `PayloadsTest` covers `place` and its
   limits, `filled.sh` and `overlap.sh` pass (hook S2 still turns Disable's blocks and matches ghost blocks), and
   `real.sh`'s checks of what lands on the server pass, with the driver sending each shape's `place` itself (a driver
   command that steps 3 and 4 keep for the cap's check): the whole Line and the Line 1 oak plank short, the stairs,
   door, bed and torches along a wall, the mix and the mirror, real blocks and the cow in the way, the reach, ghost
   blocks filled, spawn protection and adventure mode, the cap and its time, and one sound.
3. **The setting in the game** ("The setting", "What a shape does", "The preview", "Undo"). The row in "Settings"
   (hook E36's label keys, `VisualSettingsRow`), hook E1 sending the shape as `place`, hooks E8, E40 and E41, and the
   controls lines. Done when `ControlsLineTest` covers the lines with the setting on and off, `filled.sh`'s settings
   checks pass with the new row, `step9.sh`'s controls line checks pass unchanged with the setting off, and every
   `real.sh` check of a shape passes through Planner1's own clicks, with the preview's red blocks, the whole Line red
   and no sound when it's 1 oak plank short, the crosshair's icons, and Ctrl+Z after a Line undoing the ghost shape
   before it.
   Shots as `real.sh` lists them for shapes.
4. **A paste of real blocks** ("A paste"). Hook M2 sending a paste, or a Line, Wall or Floor of copies, as `place`
   with a paste's face and hit; `CopyColour`'s colours for real blocks; and the copy's controls lines with the setting
   on. Done when `CopyColourTest` and `ControlsLineTest` cover what the spec's unit tests' paragraph lists for them,
   `step9.sh`'s copy and paste checks pass unchanged with the setting off, and `real.sh`'s paste checks pass through
   Planner1's own clicks: the hut with its door, wall torch, hanging lantern, double slab and candles, yellow for its
   water, red 1 cobblestone short, a real block in the way, a Line of 3 copies, a Floor over 2,000 blocks, and one
   sound for each player. Shots as `real.sh` lists them for the copy.
5. **Texts and screenshots for Geo**: every text steps 2 to 4 add (the row's label and tooltip, the controls lines'
   words, the copy's lines and its reasons, and the notices for blocks left out, an inventory that falls short and the
   cap), and the player guide's sentences on "Place real blocks", go to Geo as editable text, with every step's
   screenshots, the changes outlined in red, each text following the rule Geo approved for the game's texts (985 Q1 A;
   1313). Geo's version is built in, `ControlsLineTest` and `real.sh` check each text exactly, and the full set of
   in-game scripts runs on the last build. The same step sets the version 0.9.0, and the release request shows the
   setting, a placed shape and a paste in the game, outlined in red.
6. **The release** (done, Geo's approval in 1441): the GitHub release `building-planner-0.9.0`, not latest, from the
   commit on which the full set passed and `overlap.sh`, `filled.sh` and `import.sh` passed again, run by the server
   and the pack (see "Current setup"), with the guide's new sentences in Geo's wording.

**Building Planner 0.10.0** (the bullet "Building Planner 0.10.0" in "Decisions" above: Geo's comment c4 on 1359,
1368 Q1 A and Geo's answers in 1374, 1375, 1381 and 1453). "Servers without Building Planner" in
[building-planner.md](building-planner.md) is the design, with every choice answered. The steps start once 0.9.0 is
released. 0.10.0 keeps protocol version 5 and changes nothing this server does.

The steps come in this order, each needing the one before it, and each small enough for one child. 0.8.0's rules for
commits, screenshots, the spec's "In-game checks" and the full set of in-game scripts apply; the full set runs once,
on step 8's last build, before the release. The in-game checks of servers without Building Planner are in `local.sh`,
which step 1 adds: it starts the dev server with the add-on left off its classpath (`start_server "$ADDON"
server-without-addon`, as `step8.sh` does), both clients having the add-on, and restarts it with the add-on for the
checks that need both kinds of server. Those of the player's own worlds are in `own.sh`, which step 6 adds.

1. **The local session** ("Which servers", "The local server", "The files", and undo in "What works there"). The rule
   at joining, `LocalServer`, `SessionLink` for `ClientSession` and `ListSync`, the files and their folders, a `hello`
   replacing a local session, `OldPlans` only during a session with a server that runs Building Planner, and
   `step8.sh`'s checks of the server without the add-on rewritten for a local session. Done when `JoinRuleTest`,
   `LocalFoldersTest` and `LocalServerTest`'s edits, undo, redo and files pass, `step8.sh` passes, and `local.sh`'s
   checks of the session, a shape, undo and redo, and the files pass.
2. **Ghost blocks a real block fills** (in "What works there"). Hook C1, the wait for the server's answer, and the
   check at chunk load. Done when `LocalServerTest` covers them, `local.sh`'s checks of filled ghost blocks pass, the
   refused placement's included, and `filled.sh` passes unchanged.
3. **What needs a server that runs Building Planner** (in "What works there"). "Place real blocks", Disable's turn and
   "Match ghost blocks" acting only with such a server. Done when `DisableTurnTest` covers no turn during a local
   session, `local.sh`'s checks of Disable and "Place real blocks" pass, and `step10.sh`'s Disable checks, `filled.sh`
   and `real.sh` pass unchanged.
4. **Material lists on the computer** (in "What works there"). Done when `LocalServerTest` covers the lists kept and
   read back, `local.sh`'s list checks pass, and `step9.sh`'s list checks pass unchanged.
5. **The copy between servers** ("Between servers"). The copy
   kept outside the session, through leaving, joining and changing dimension. Done when `local.sh`'s checks of the copy
   and of the server restarted with and without Building Planner pass, and `step9.sh`'s copy checks pass.
6. **The player's own worlds** ("The player's own worlds", and the rule at joining in "Which servers").
   `BuildingPlanner.onInitialize` registering
   `ServerSync` in the game too, with the default cap and without a config file; hook S1 in the mixin config's common
   section; the import only on a dedicated server; a world whose layer files can't be listed leaving the server part
   off, without `hello`; `OldPlans` skipped in the player's own world; `off`, `ownsShapes` and `undo` asking only
   whether a session runs; the spec's "Unverified" bullet on Effortless Building placing real blocks in a singleplayer
   world removed; and the harness opening a copy of the dev server's world in Planner1's game
   (`--quickPlaySingleplayer`) and to LAN (the driver's `lan`), with Planner2's game joining it at that port. Done when
   `JoinRuleTest` covers the game's own world waiting for `hello`, `own.sh`'s checks pass, and `step6.sh`, `filled.sh`
   and `real.sh` pass unchanged.
7. **The line in the radial menu** (in "What works there"). Hook E42, `RadialLine`, and the line's key in the language
   file. Done when `RadialLineTest` covers the row on screens 240, 256, 257 and 270 high, `local.sh`'s checks of the
   line and `own.sh`'s checks that it doesn't show pass, and `step10.sh`'s radial menu checks pass unchanged, with the
   line reading "(client only)" (1381 c1; 1453).
8. **Texts and screenshots for Geo**: the radial menu's line and the player guide's sentences on other servers and on
   the player's own worlds go to Geo as editable text, with every step's screenshots, the changes outlined in red, each
   text following the rule Geo approved for the game's texts (985 Q1 A; 1313). Geo's version is built in, `local.sh`
   checks it exactly, and the full set of in-game scripts runs on the last build. The same step sets the version
   0.10.0, and the release request shows ghost blocks on a server without Building Planner with the radial menu's line,
   and ghost blocks a LAN guest shares in the player's own world, outlined in red.
9. **The release** (done, Geo's approval in 1461): the GitHub release `building-planner-0.10.0`, not latest, from the
   commit on which the full set passed and `overlap.sh`, `filled.sh` and `import.sh` passed again, run by the server
   (1461 Q2 A) and the pack (see "Current setup"), with the guide's new sentences in Geo's wording.

**Building Planner 0.11.0** (the bullet "Saved copies" in "Decisions" above: Geo's comment on 1375 Q1 and the answers
in 1384 and 1464). "Saved copies" in [building-planner.md](building-planner.md) is the design. 0.11.0 starts once
0.10.0 is released, keeps protocol version 5, and changes nothing the server does.

The steps come in this order, each needing the one before it, and each small enough for one child. 0.8.0's rules for
commits, screenshots, the spec's "In-game checks" and the full set of in-game scripts apply; the full set runs once,
on step 3's last build, before the release. The in-game checks of saved copies are in `saved.sh`, which step 1 adds.

1. **The saved copies and their files** ("Saved copies": "Which copies stay", "Where they're kept", "The files" and
   "After a restart"). `CopyHistory` and the index's format (common), `SavedCopies` with its background thread, each
   copy saved as Copy makes it, the 11th dropping the oldest not renamed, and the radial menu's Paste button bringing
   back the newest saved copy after a restart; the driver renames, picks and deletes copies through commands of its own
   until step 2 adds the screen. Done when `CopyHistoryTest` and `CopyIndexTest` pass, `saved.sh`'s checks of the
   files, the 11th copy, a rename kept, unreadable files, the restart and the server without Building Planner pass, and
   `step9.sh`'s copy checks pass unchanged.
2. **The screen and its key** ("The key" and "The screen"). `SavedCopiesScreen`, `RenameCopyScreen`, the key
   `key.buildingplanner.saved_copies` (V), and its control in `ControlsLine.copy`. Done when `ControlsLineTest` covers
   the key's control, `saved.sh`'s checks of the screen, the key and the controls line pass through Planner1's own
   presses and clicks, and `step9.sh`'s controls line checks pass with the new control. Shots as `saved.sh` lists them.
3. **Texts and screenshots for Geo**: every text steps 1 and 2 add (the key's name, the control, the screens' titles,
   line, buttons and field, the rows' size and block count, the delete question, the empty screen's line and the
   message for a copy that can't be read), and the player guide's sentences on saved copies, go to Geo as editable
   text, with every step's screenshots, the changes outlined in red, each text following the rule Geo approved for the
   game's texts (985 Q1 A; 1313). Geo's version is built in, `ControlsLineTest` and `saved.sh` check each text
   exactly, and the full set of in-game scripts runs on the last build. The same step sets the version 0.11.0, and the
   release request shows "Saved copies" and the controls line with the key in the game, outlined in red.
4. **The release**, once Geo approves it: the GitHub release `building-planner-0.11.0`, not latest, from the commit on
   which the full set passed, run by the pack, with the guide's new sentences in Geo's wording. The release request
   asks whether the server takes it too, since it changes nothing the server does.

## Design work: Building Planner

[building-planner.md](building-planner.md) is the spec: what players get, how each goal above is met, the technical
design with every hook checked in the source, and what's unverified. Geo's decisions are above. The texts players see
are in the wording Geo approved, 0.7.0's in 1313.

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
