# Plan

## Current setup

**Server.** Vanilla Minecraft 26.3 on Geo's VPS, run by the systemd unit
`minecraft.service` as root from `/home/minecraft`:
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java -Xmx6G -Xms1G -jar server.jar nogui`, port 25565. Survival, hard,
whitelist enforced (11 players), online mode. The world (`world/`) is 1.9 GB. Earlier archives sit beside it:
`minecraft-archive-2026-10-29-1.21.11.tar.gz` and `minecraft-backup-2025-12-12.tar.gz`. No mods or plugins. The VPS has
30 GB of RAM (about 16 GB available) and 163 GB of free disk; it also hosts other services.

**Players.** Unmodded today. Geo prefers Prism Launcher (Geo, inbox request 402).

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

Proposed, not yet approved: players run Litematica (with MaLiLib), Syncmatica and Freecam; the server runs Fabric,
Fabric API and Syncmatica (402 r2 to r4, 408).

## Agent-ready work

Ready to run once Geo approves the action (inbox request 408):

- **Server switch:** back up `world/`, replace `server.jar` in `minecraft.service` with Fabric's server launcher for
  26.3, add Fabric API and Syncmatica (26.3 builds), restart when no one is online, and check the world loads.
- **Player pack:** the proposed player mods for 26.3, delivered as request 408's answer chooses (a self-updating Prism
  instance through packwiz, or a modpack file).

## Design work: the planning mod

Geo decides these; the project thread turns them into a decision brief for review.

1. Plan model: named plans (each a box that can be shown, hidden, moved and rotated as a whole, with its own material
   list) or one shared ghost layer over the whole world.
2. Growth: when a shape crosses a plan's edge, grow the plan or drop the blocks outside it.
3. Permissions: who can change or delete a plan (anyone, or its creator and people they add).
4. Undo: each player's own changes only, or any change in a plan.
5. Whether Litematica's own Edit Schematic changes sync live too, not only Effortless Building's.
6. Structure: one new add-on mod that hooks into unmodified Effortless Building, Litematica and Syncmatica, or changed
   copies (forks) of Effortless Building and Syncmatica.
7. Players without the pack: whether they must still be able to join (Effortless Building on the server adds an item,
   which may stop unmodded players from joining; unverified).
8. The mod's name and every message players see (Geo reviews all player-facing text).

### Source evidence (read 2026-11-13)

- **Effortless Building**: `Requios/effortless-building-multi`, branch `26.3` (`f31a5b5`, 2026-10-10), LGPL-3.0.
  `common/.../buildpipeline/BuildPipelineClient.java` computes the full block set on the client (build mode, then
  modifiers, then constraints) and on the last click sends `PlaceBuildModePacket` with only the click positions and
  options; the server rebuilds the set itself (`BuildPipeline.SERVER.runServerPipeline`). The preview runs the same
  pipeline each frame. A plan mode can take the client's block set at that send and write it into a plan instead.
- **Litematica**: `sakura-ryoko/litematica`, branch `26.3` (`eb15b39`), LGPL-3.0. `util/SchematicUtils.java`
  (private `setTargetedSchematicBlockState`) writes a block into a placement's sub-region container only inside the
  sub-region's box, then calls `markChunkForRebuild`; the pieces it uses (`getAllPlacementsTouchingChunk`,
  `getSchematicContainerPositionFromWorldPosition`, `getUntransformedBlockState`) are public.
  `LitematicaSchematic.createEmptySchematic(area, author)` makes an empty plan from an area selection.
- **Syncmatica**: `sakura-ryoko/syncmatica`, branch `26.3` (`37a969e`), CC0. `data/ServerPlacement` identifies a
  plan's contents by a fixed hash of its file (`hashValue`); `MODIFY` packets carry only placement values (origin,
  rotation, mirror, sub-regions). Block changes don't sync today; a changed plan must be shared again as a new file.

Unverified: whether Effortless Building's client works when the server lacks it, and how a placement's sub-region can
grow in place.
