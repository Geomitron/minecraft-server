# Plan

## Current setup

**Server.** Minecraft 26.3 on Fabric (loader 0.20.1, server launcher 1.1.2) with Fabric API 0.166.2+26.3,
Syncmatica 0.3.20, and Building Planner 0.1.0 from the release `building-planner-0.1.0` (`/home/minecraft/mods/`), on
Geo's VPS. The systemd unit `minecraft.service` runs it as root from `/home/minecraft`:
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java -Xmx6G -Xms1G -jar fabric-server-mc.26.3-loader.0.20.1-launcher.1.1.2.jar nogui`,
port 25565. Survival, hard, whitelist enforced (11 players), online mode. None of the mods adds blocks or items, so players without
mods can join: an unmodded 26.3 client joined a dev server with the same mods (`def7603`). The world (`world/`) is 1.9 GB.
Archives sit beside it: `minecraft-backup-2026-11-15-pre-building-planner.tar.gz` (the world just before Building Planner
was installed, 1.5 GB), `minecraft-backup-2026-11-14-pre-fabric.tar.gz` (the world just before the switch to Fabric,
1.5 GB), `minecraft-archive-2026-10-29-1.21.11.tar.gz` and `minecraft-backup-2025-12-12.tar.gz`.
To take Building Planner off the server, remove `mods/building-planner-0.1.0.jar` and run
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
pack "Server Pack": Minecraft 26.3 and Fabric loader 0.20.1 with Litematica, MaLiLib, Syncmatica 0.3.20, Freecam,
Fabric API, Building Planner 0.1.0 (downloaded from its release), and Effortless Building 4.3, served from `main` at
https://raw.githubusercontent.com/Geomitron/minecraft-server/main/pack/pack.toml, so
a push to `main` changes what players get at their next launch. `instance/` is the Prism Launcher instance "Server
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

Building Planner as built, from inbox requests 469, 473, 477, 482, 483, 485, 570, 571, 578 and 584:

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
  no Effortless Building mode chosen (Q4 A).
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
  texts as written. An agent looks for a way to show a plan's blocks under the copy's empty space, and reports back
  (Q3 A). Copy and paste goes to players as Building Planner 0.2.0, released the same way as 0.1.0 (Q4 A).
- Before the player guide's section on planning builds is published, Geo walks through every planning feature in the
  game from a runbook with room for feedback (Geo, inbox request 578: "Build a runbook to teach me how it works, walk
  me through using all the features we have available, with space for me to provide feedback and suggestions on how to
  adjust the experience of using the mod. Once I'm happy with it, we can publish a guide on how to use it.").

## Agent-ready work

- **Building Planner** (design approved, 411), built in this order, each step verified as
  [building-planner.md](building-planner.md) describes before the next: (1) the `mod/` scaffold with run setups for a
  server and two clients; (2) the plan model and `.litematic` storage; (3) the live-sync core on the server; (4) server
  undo; (5) Syncmatica on the server (plans from shared files, the delete check, archiving deleted plans); (6) the
  Litematica bridge on the client; (7) Effortless Building capture; (8) joining a server without the add-on; then
  (9) copy and paste of part of a plan (420, Q8 A). All nine steps are built.

## Design work: Building Planner

[building-planner.md](building-planner.md) is the spec: what players get, how each goal above is met, the technical
design with every hook checked in the source, and what's unverified. Geo's decisions from inbox requests 411 and 420
are above. The messages players see go to Geo for review once something is built.

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
